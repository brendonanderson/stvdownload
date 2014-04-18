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
//                println restext
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
        String url = "https://us-my.simple.tv/Library/MyShows" +
                "?browserDateTimeUTC=2014%2F2%2F4+16%3A16%3A18" +
                "&browserUTCOffsetMinutes=-360" +
                "&mediaServerID=" + mediaServerId
        List<Show> shows = []
        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
//                println h
                Document doc = Jsoup.parse(h)
                doc.select("figure").each { Element e ->
                    Show show = new Show()
                    show.groupId = e.attr("data-groupid")
                    show.imgUrl = e.select("img").first().attr("src")
                    show.name = e.select("b").first().text()
                    show.episodes = e.select("span.no").first().text() as Integer
                    shows.add(show)
                }
            }
        }
        return shows
    }
    public List<Episode> getEpisodes(Show show) {
        String url = "https://us-my.simple.tv/Library/ShowDetail" +
                "?browserDateTimeUTC=2014%2F3%2F13+15%3A45%3A21" +
                "&browserUTCOffsetMinutes=-300" +
                "&groupID=${show.groupId}"
        List<Episode> episodes = []
        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
//                println h
                Document doc = Jsoup.parse(h)
                doc.select("#recorded").select("article").each {Element e ->
                    Episode episode = new Episode()
                    episode.instanceId = e.select("a.button-standard-watch").attr("data-instanceid")
                    episode.itemId = e.select("a.button-standard-watch").attr("data-itemid")
                    episode.groupId = e.select("a.button-standard-watch").attr("data-groupid")
                    episode.title = e.select("h3").first().text()
//                    log.info("Episode title: ${episode.title}")
                    String info = e.select(".show-details-info").html()
                    episode.date = info.substring(0, info.indexOf("&nbsp;")).trim()

                    if (info.contains("Season: ")) {
                        String season = info.substring(info.indexOf("Season: "))
                        season = season.substring(0, season.indexOf("</b>"))
                        season = season.substring(season.indexOf(">") + 1)
                        episode.season = season as Integer
                    }

                    if (info.contains("Episode: ")) {
                        String ep = info.substring(info.indexOf("Episode: "))
                        ep = ep.substring(0, ep.indexOf("</b>"))
                        ep = ep.substring(ep.indexOf(">") + 1)
                        episode.episode = ep as Integer
                    }
                    episodes.add(episode)
                }
            }
        }
        return episodes
    }
    public List<EpisodeUrl> getEpisodeUrls(Episode episode, String mediaServerId, Boolean useLocalUrls) {
//        log.info("service episode: ${episode.title}: ${episode.instanceId}:${episode.groupId}:${episode.itemId}")

        Map urlMap = getUrls(mediaServerId)


        String urlToUse = (useLocalUrls?urlMap.localUrl:urlMap.remoteUrl)
//        println urlToUse
        List<EpisodeUrl> episodeUrls = []
        String url = "https://us-my.simple.tv/Library/Player" +
                "?browserUTCOffsetMinutes=-300" +
                "&groupID=${episode.groupId}" +
                "&itemID=${episode.itemId}" +
                "&instanceID=${episode.instanceId}" +
                "&isReachedLocally=${useLocalUrls}"

//        println "Url to get episode urls: ${url}"
        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
//                println h
                Document doc = Jsoup.parse(h)
                String path = doc.getElementById("video-player-large").attr("data-streamlocation")
//                log.info("Path: ${path}")
//                log.info("URL: ${urlToUse + path}")
                new HTTPBuilder(urlToUse + path).request(Method.GET, ContentType.TEXT) {
                    headers["Cookie"] = cookies.join(";")
                    response.success = { res, read ->
                        List<String> qualities = read.text.split("\n")
                        qualities.each {
                            if (!it.startsWith("#")) {
                                Integer inc = it.substring(it.indexOf("hls-") + 4, it.indexOf(".m3u8")) as Integer
                                String q = it.replaceAll(/hls-[0-9]\.m3u8/, (100 + inc) as String)
                                String[] pathparts = path.split("/")
                                String newpath = pathparts[0..(pathparts.length - 2)].join("/")
                                EpisodeUrl episodeUrl = new EpisodeUrl()
                                episodeUrl.url = urlToUse + newpath.substring(1) + "/" + q
                                log.info(episodeUrl.url)
                                episodeUrls.add(episodeUrl)
                            }
                        }
                    }
                }
            }
        }
        episodeUrls
    }
    public String downloadEpisode(String url, SimpletvModel model) {
//        println url
        Episode episode = model.episodes[model.selectedEpisodeIndex]
        Show show = model.shows.find { it.groupId == episode.groupId }
        String filename = "${show.name} - s${episode.season?:"XX"}e${episode.episode?:"YY"} - ${episode.title}.mp4"
        filename = filename.replaceAll(/[^a-zA-Z0-9-.&_ ]/, "")
        if (model.saveLocation) {
            if (!new File(model.saveLocation).exists()) {
                new File(model.saveLocation).mkdirs()
            }
        }
        if (model.saveLocation) {
            filename = model.saveLocation + "/" + filename
            Properties prop = new Properties()
            File propFile = new File("stv.properties")
            if (!propFile.exists()) {
                propFile.createNewFile()
            }
            prop.load(propFile.newDataInputStream())
            prop.setProperty("saveLocation", model.saveLocation)
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
                    model.downloadPct = (bytesDownloaded / totalBytes * 100)
                    if (bytes == null || bytecount == -1) {
                        break;
                    }
                    fos.write(bytes, 0, bytecount)
                }
                fos.flush()
                fos.close()
                log.info("Done!")
                model.downloadPct = 100
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
}