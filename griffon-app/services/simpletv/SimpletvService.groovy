package simpletv

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.conn.EofSensorInputStream
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class SimpletvService {

    private static Set<String> cookies = []
    private static String accountId

    public String login(String username, String password) {

        Properties prop = new Properties()
        File propFile = new File("stv.properties")
        if (!propFile.exists()) {
            propFile.createNewFile()
        }
        prop.load(propFile.newDataInputStream())
        prop.setProperty("username", username)
        prop.setProperty("password", password)
        prop.store(propFile.newWriter(), null)

        new HTTPBuilder("https://us.simple.tv/Auth/SignIn").request(Method.POST, ContentType.JSON) {
            body = [UserName: username, Password: password, RememberMe: "true"]
            response.success = { HttpResponseDecorator resp, reader ->
                resp.getHeaders("Set-Cookie").each {
                    String cookie = it.value.split(";")[0]
                    if (cookie.split("=").length > 1) {
                        cookies.add(cookie)
                    }
                }
            }
        }
    }

    public List<Dvr> getDvrs() {
        List<Dvr> dvrs = []
        new HTTPBuilder("https://us-my.simple.tv").request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { HttpResponseDecorator resp, reader ->
                String restext = reader.text
                Document doc = Jsoup.parse(restext)
                Element section = doc.getElementById("watchShow")
                accountId = section.attr("data-accountid")

                Elements uls = doc.select("ul.switch-dvr-list")
                if (uls.size() == 1) {
                    uls.select("li").each { Element li ->
                        Element a = li.select("a").first()
                        Dvr dvr = new Dvr()
                        dvr.mediaServerId = a.attr("data-value")
                        dvr.name = a.text()
                        dvrs.add(dvr)
                    }
                } else {
                    log.error("Error finding one set of DVRs.  I found ${dvrs?.size()}.  Might be a bug in the code.")
                }
            }
        }
        dvrs
    }

    public List<Show> getShows(String mediaServerId) {

        List<Show> shows = []
        String jsonUrl = "https://stv-p-api1-prod.rsslabs.net/content/ond/contentmap/${mediaServerId}/groups?page=1-60&composition=mediaserver&state=Library"

        new HTTPBuilder(jsonUrl).request(Method.GET, ContentType.JSON) {
            headers["Host"] = "stv-p-api1-prod.rsslabs.net"
            headers["X-RSSINC-CLIENTTYPE"] = "ipadplayer"
            headers["User-Agent"] = "Simple.TV/772 CFNetwork/672.1.14 Darwin/14.0.0"
            headers["Authorization"] = "Basic ${getEncodedUserPass()}"
            response.success = { resp, reader ->
                reader.Groups.each { s ->
                    Show show = new Show()
                    show.groupId = s.ID
                    show.imgUrl = s.Image.ImageUrl
                    show.name = s.Title
                    shows.add(show)
                }

            }
        }
        return shows
    }
    public List<Episode> getEpisodes(Show show, String mediaServerId) {

        String jsonUrl = "http://stv-p-api1-prod.rsslabs.net/content/ond/contentmap/${mediaServerId}/group/${show.groupId}/iteminstances?page=1-201&composition=mediaserver"
        List<Episode> episodes = []
        new HTTPBuilder(jsonUrl).request(Method.GET, ContentType.JSON) {
            headers["Host"] = "stv-p-api1-prod.rsslabs.net"
            headers["X-RSSINC-CLIENTTYPE"] = "ipadplayer"
            headers["User-Agent"] = "Simple.TV/772 CFNetwork/672.1.14 Darwin/14.0.0"
            headers["Authorization"] = "Basic ${getEncodedUserPass()}"
            response.success = {resp, reader ->
                def episodeList = reader
                episodeList.each { ep ->
//                    println ep
                    ep.Instances.each { inst ->
                        Episode episode = new Episode()
                        episode.instanceId = inst.InstanceState.InstanceId
                        episode.itemId = ep.ID
                        episode.groupId = show.groupId
                        episode.title = ep.Title
                        episode.date = inst.DateTime
                        episode.season = (ep.EpisodeSeasonNo ? (ep.EpisodeSeasonNo as Integer) : null)
                        episode.episode = (ep.EpisodeSeasonSequence ? (ep.EpisodeSeasonSequence as Integer) : null)
                        inst.InstanceState.Streams.each {
                            episode.baseUrl = it.Location
                        }
                        if (episode.baseUrl != null) {
                            episodes.add(episode)
                        }
                    }
                }
            }

        }
        return episodes
    }
    public List<EpisodeUrl> getEpisodeUrls(Episode episode, String mediaServerId, Boolean useLocalUrls) {
        Map urlMap = getUrls(mediaServerId)
        String urlToUse = (useLocalUrls?urlMap.localUrl:urlMap.remoteUrl)

        List<EpisodeUrl> episodeUrls = []

        new HTTPBuilder(urlToUse + episode.baseUrl).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { res, read ->
                List<String> qualities = read.text.split("\n")
                qualities.each {
                    if (!it.startsWith("#")) {
                        Integer inc = it.substring(it.indexOf("hls-") + 4, it.indexOf(".m3u8")) as Integer
                        String q = it.replaceAll(/hls-[0-9]\.m3u8/, (100 + inc) as String)
                        String[] pathparts = episode.baseUrl.split("/")
                        String newpath = pathparts[0..(pathparts.length - 2)].join("/")
                        EpisodeUrl episodeUrl = new EpisodeUrl()
                        episodeUrl.url = urlToUse + newpath + "/" + q
                        episodeUrls.add(episodeUrl)
                    }
                }
            }
        }
        episodeUrls
    }
    public String downloadEpisode(String url, Show show, Episode episode, String saveLocation, ProgressBarPct downloadPct, Integer namingMode) {
		// TODO: the whole "download" process "fails" silently if no path is
		// defined, need to get better messaging in here and re-write this code
		// in a more robust way
        Properties prop = new Properties()
        File propFile = new File("stv.properties")
        if (!propFile.exists()) {
            propFile.createNewFile()
        }

		if (saveLocation) {
			// TODO: consider moving this to a central, reactive location that
			// persists even if you don't press the download button
			// do this first because in "plex compatible mode" the path will be
			// augmented with additional segments
			prop.setProperty("saveLocation", saveLocation)
		}
        prop.setProperty("namingMode", "${namingMode}")

        downloadPct.value = 0
		String filename;
		if (namingMode == 0) {
			filename = "${show.name} - s${episode.season?:"XX"}e${episode.episode?:"YY"} - ${episode.title}.mp4"
			filename = filename.replaceAll(/[^a-zA-Z0-9-.&_() ]/, "")
		} else if (namingMode == 2) {
			// Plex compatible - https://support.plex.tv/hc/en-us/articles/200220687-Naming-Series-Based-TV-Shows
		    filename = "${show.name} - ${episode.toString()}.mp4"
			filename = filename.replaceAll(/[^a-zA-Z0-9-.&_() ]/, "")
			saveLocation = "${saveLocation}/TV Shows/${show.name}/Season ${episode.getPaddedSeason()}"
		} else {
            filename = "${show.name} - ${episode.toString()}.mp4"
            filename = filename.replaceAll(/[^a-zA-Z0-9-.&_() ]/, "")
        }
        if (saveLocation) {
            if (!new File(saveLocation).exists()) {
                new File(saveLocation).mkdirs()
            }
        }
        if (saveLocation) {
            filename = saveLocation + "/" + filename
            prop.load(propFile.newDataInputStream())
            prop.store(propFile.newWriter(), null)
        }
        new HTTPBuilder(url).request(Method.GET, ContentType.BINARY) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, EofSensorInputStream reader ->
                log.info("total bytes: ${resp.getLastHeader("Content-Length").getValue()}")
                Long totalBytes = resp.getLastHeader("Content-Length").getValue() as Long
                FileOutputStream fos = new FileOutputStream(filename)
                byte[] bytes = new byte[4096]
                Long bytesDownloaded = 0
                while (true) {
                    Integer bytecount = reader.read(bytes)
                    bytesDownloaded += bytecount
                    downloadPct.value = (bytesDownloaded / totalBytes * 100)
                    if (bytes == null || bytecount == -1) {
                        break;
                    }
                    fos.write(bytes, 0, bytecount)
                }
                fos.flush()
                fos.close()
                log.info("Done!")
                downloadPct.value = 100
            }
        }
    }

    private Map<String, String> getUrls(String mediaServerId) {
        Map<String, String> urlMap = [:]
        String dataurl = "https://us-my.simple.tv/Data/RealTimeData?accountId=${accountId}&mediaServerId=${mediaServerId}&playerAlternativeAvailable=false"
        new HTTPBuilder(dataurl).request(Method.GET, ContentType.JSON) {
            headers["Cookie"] = cookies.join(";")
            response.success = { HttpResponseDecorator resp, json ->
//                println json
                urlMap.localUrl = json.LocalStreamBaseURL
                urlMap.remoteUrl = json.RemoteStreamBaseURL + "/"
            }
        }
        urlMap
    }
    private Set<String> fixCookies(Set<String> cookies, String mediaServerId) {
        Set<String> newCookies = [] as Set<String>
//        println cookies
        cookies.each { it ->
            if (it.contains("browserDefaultMediaServer")) {
                Integer idx1 = it.lastIndexOf("=") + 1
                String oldId = it.substring(idx1)
//                println "old id = ${oldId}"
                it = it.replace(oldId, mediaServerId)
            }
            newCookies.add(it)
        }
//        println newCookies
        newCookies
    }
    private String getEncodedUserPass() {
        Properties prop = new Properties()
        File file = new File("stv.properties")
        prop.load(file.newDataInputStream())
        String username = prop.getProperty("username")
        String password = prop.getProperty("password")
        "$username:$password".bytes.encodeBase64().toString()
    }
}
