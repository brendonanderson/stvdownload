package simpletv

class SimpletvController {
    SimpletvModel model
    def view
    SimpletvService simpletvService

    def login = {

        model.status = "Logging in \"${model.username}\"..."
        model.selectedEpisodeIndex = null
        simpletvService.login(model.username, model.password)
        model.connected = true
        disableLogin(model)
        model.status = "Getting DVRs available..."
        List<Dvr> dvrs = simpletvService.getDvrs()
        model.dvrs.clear()
        model.dvrs.addAll(dvrs)
        if (dvrs?.size() >= 1) {
            model.selectedDvrIndex = 0
        }
        model.status = ""
    }

    def getShows = { Integer index ->
        model.status = "Getting all shows on DVR \"${model.dvrs[model.selectedDvrIndex].name}\""
        model.downloadBtnEnabled = false
        simpletvService.getUrls(model.dvrs[model.selectedDvrIndex].mediaServerId)
        edt {
            List<Show> showList = simpletvService.getShows(model.dvrs[model.selectedDvrIndex].mediaServerId)
            model.episodes.clear()
            model.episodeUrls.clear()
            model.selectedEpisodeIndex = null
            model.shows.clear()
            model.shows.addAll(showList)
        }
        model.status = ""
    }
    def getEpisodes = { Integer index ->
        println model.selectedNamingMode
        model.status = "Getting all episodes for show \"${model.shows[index].name}\"..."
        model.downloadBtnEnabled = false
        model.episodes.clear()
        model.episodeUrls.clear()
        model.selectedEpisodeIndex = null
        model.episodes.addAll(simpletvService.getEpisodes(model.shows[index], model.dvrs[model.selectedDvrIndex].mediaServerId))
        model.status = ""
    }

    def getEpisodeUrls = { Integer index ->
        if (model.selectedEpisodeIndex != null) {
            model.status = "Getting episode qualities..."
            model.downloadBtnEnabled = false
            model.episodeUrls.clear()
            Episode episode = model.episodes[model.selectedEpisodeIndex]
            List<EpisodeUrl> urls = simpletvService.getEpisodeUrls(episode, model.dvrs[model.selectedDvrIndex].mediaServerId, model.useLocalUrl)
            model.episodeUrls.addAll(urls)
            model.status = ""
        }
    }

    def download = { e ->
        if (model.selectedEpisodeIndex != null) {
            model.showListEnabled = false
            model.episodeListEnabled = false
            model.qualityListEnabled = false
            model.downloadBtnEnabled = false
            model.status = "Downloading episode \"${model.episodes[model.selectedEpisodeIndex].title}\""
            EpisodeUrl episodeUrl = model.episodeUrls[model.selectedUrlIndex]
            log.info(episodeUrl.url)
            simpletvService.downloadEpisode(episodeUrl.url, model.shows[model.selectedShowIndex], model.episodes[model.selectedEpisodeIndex], model.saveLocation, model.downloadPct, model.selectedNamingMode)
            model.downloadBtnEnabled = true
            model.showListEnabled = true
            model.episodeListEnabled = true
            model.qualityListEnabled = true
            model.status = ""
        }
    }
    def downloadAll = { e ->
        log.info("downloading all episodes")
        model.downloadAllEnabled = false
        model.batchShowListEnabled = false
        model.postCommandEnabled = false
        if (model.postCommand) {
            Properties prop = new Properties()
            File propFile = new File("stv.properties")
            if (!propFile.exists()) {
                propFile.createNewFile()
            }
            prop.load(propFile.newDataInputStream())
            prop.setProperty("postCommand", model.postCommand)
            prop.store(propFile.newWriter(), null)
        }
        model.allEpisodesPct.value = 0
        Map<String, Episode> urls = [:]
        model.status = "Getting all episode information..."
        List<Episode> episodes = simpletvService.getEpisodes(model.shows[model.selectedBatchShowIndex], model.dvrs[model.selectedDvrIndex].mediaServerId)
        episodes.eachWithIndex { Episode episode, Integer index ->
            model.status = "Getting all episode download URLs" + (".".multiply(index % 4))
            List<EpisodeUrl> episodeUrls = simpletvService.getEpisodeUrls(episode, model.dvrs[model.selectedDvrIndex].mediaServerId, model.useLocalUrl)
            episodeUrls.each {
                if (it.url.contains("4500000")) {
                    urls[it.url] = episode
                }
            }

        }
        urls.eachWithIndex { url, episode, index ->
            model.status = "Downloading episode \"${episode.title}\""
            model.allEpisodesPct.value = ((index + 1) / (episodes.size() as Double)) * 100
            simpletvService.downloadEpisode(url, model.shows[model.selectedBatchShowIndex], episode, model.saveLocation, model.batchPct, model.selectedNamingMode)
        }
        model.allEpisodesPct.value = 100
        if (model.postCommand) {
            model.status = "Executing external command"
            def proc = model.postCommand.execute()
            proc.in.eachLine { model.status = (it.size() > 30000?it.substring(0, 32) + "...":it) }
            proc.waitFor()
        }
        model.batchShowListEnabled = true
        model.downloadAllEnabled = true
        model.postCommandEnabled = true
        model.status = ""
    }

    private void disableLogin(SimpletvModel model) {
        model.loginBtnEnabled = false
        model.userNameEnabled = false
        model.passwordEnabled = false
    }
}
