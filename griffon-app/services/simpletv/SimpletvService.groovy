package simpletv

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseDecorator
import groovyx.net.http.Method
import org.apache.http.conn.EofSensorInputStream
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class SimpletvService {

    private static Set<String> cookies = []
    private static String localUrl
    private static String remoteUrl
    private static String sid

    public String login(String username, String password) {

        new HTTPBuilder("https://www.simple.tv/Auth/SignIn").request(Method.POST, ContentType.JSON) {
            body = [UserName: username, Password: password, RememberMe: "true"]
            response.success = { HttpResponseDecorator resp, reader ->
                sid = reader["MediaServerID"]
                resp.getHeaders("Set-Cookie").each {
                    String cookie = it.value.split(";")[0]
                    if (cookie.split("=").length > 1) {
                        cookies.add(cookie)
                    }
                }
            }
        }
        new HTTPBuilder("https://my.simple.tv").request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { HttpResponseDecorator resp, reader ->
                String restext = reader.text
                Document doc = Jsoup.parse(restext)
                Element section = doc.getElementById("watchShow")
                localUrl = section.attr("data-localstreambaseurl")
                remoteUrl = section.attr("data-remotestreambaseurl")
            }
        }
    }
    public List<Show> getShows() {
        String url = "https://my.simple.tv/Library/MyShows" +
                "?browserDateTimeUTC=2014%2F2%2F4+16%3A16%3A18" +
                "&browserUTCOffsetMinutes=-360" +
                "&mediaServerID=" + sid
        List<Show> shows = []
        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
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
        String url = "https://my.simple.tv/Library/ShowDetail" +
                "?browserDateTimeUTC=2014%2F2%2F4+16%3A16%3A18" +
                "&browserUTCOffsetMinutes=-360" +
                "&groupID=" + show.groupId
        List<Episode> episodes = []
        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
                Document doc = Jsoup.parse(h)
//                println h
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
    public List<EpisodeUrl> getEpisodeUrls(Episode episode) {
//        log.info("service episode: ${episode.title}: ${episode.instanceId}:${episode.groupId}:${episode.itemId}")
        List<EpisodeUrl> episodeUrls = []
        String url = "https://my.simple.tv/Library/Player" +
                "?browserUTCOffsetMinutes=-300" +
                "&groupID=${episode.groupId}" +
                "&instanceID=${episode.instanceId}" +
                "&itemID=${episode.itemId}" +
                "&isReachedRemotely=true"

        new HTTPBuilder(url).request(Method.GET, ContentType.TEXT) {
            headers["Cookie"] = cookies.join(";")
            response.success = { resp, reader ->
                String h = reader.text
//                println h
                Document doc = Jsoup.parse(h)
                String path = doc.getElementById("video-player-large").attr("data-streamlocation")
                log.info("Path: ${path}")
                new HTTPBuilder(localUrl + path).request(Method.GET, ContentType.TEXT) {
                    headers["Cookie"] = cookies.join(";")
                    response.success = { res, read ->
                        List<String> qualities = read.text.split("\n")
                        qualities.each {
                            if (!it.startsWith("#")) {
                                log.info(it)
                                String q = it.replace("hls-0.m3u8", "100")
                                q = q.replace("hls-1.m3u8", "100")
                                q = q.replace("hls-2.m3u8", "100")
                                String[] pathparts = path.split("/")
                                String newpath = pathparts[0..(pathparts.length - 2)].join("/")
                                EpisodeUrl episodeUrl = new EpisodeUrl()
                                episodeUrl.url = localUrl + newpath.substring(1) + "/" + q
                                episodeUrls.add(episodeUrl)
                            }
                        }
                    }
                }
//                println "path: ${path}"

//                println episodeUrl
//                episodeUrl = episodeUrl.replace("hls-1.m3u8", "100")
//                new HTTPBuilder(episodeUrl).request(Method.GET, ContentType.TEXT) {
//                    headers["Cookie"] = cookies.join(";")
//                    response.success = { res, read ->
//                        println "url ${read.text}"
//                    }
//                }
            }
        }
        episodeUrls
    }
    public String downloadEpisode(String url, SimpletvModel model) {
        println url
        Episode episode = model.episodes[model.selectedEpisodeIndex]
        Show show = model.shows.find { it.groupId == episode.groupId }
        String filename = "${show.name} - s${episode.season}e${episode.episode} - ${episode.title}.mp4"
        if (model.saveLocation) {
            filename = model.saveLocation + "/" + filename
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
}
