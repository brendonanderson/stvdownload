package simpletv

class SimpletvController {
    SimpletvModel model
    def view
    SimpletvService simpletvService

    def login = {
        model.selectedEpisodeIndex = null
        simpletvService.login(model.username, model.password)
        model.connected = true
        List<Dvr> dvrs = simpletvService.getDvrs()
        model.dvrs.clear()
        model.dvrs.addAll(dvrs)
        if (dvrs?.size() >= 1) {
            model.selectedDvrIndex = 0
        }
    }

    def getShows = { Integer index ->
        simpletvService.getUrls(model.dvrs[model.selectedDvrIndex].mediaServerId)
        edt {
            List<Show> showList = simpletvService.getShows(model.dvrs[model.selectedDvrIndex].mediaServerId)
            model.episodes.clear()
            model.episodeUrls.clear()
            model.selectedEpisodeIndex = null
            model.shows.clear()
            model.shows.addAll(showList)
        }
    }
    def getEpisodes = { Integer index ->
        println model.shows[index].name
        println model.shows[index].groupId
        model.episodes.clear()
        model.episodeUrls.clear()
        model.selectedEpisodeIndex = null
        model.episodes.addAll(simpletvService.getEpisodes(model.shows[index]))
    }

    def getEpisodeUrls = { Integer index ->
        if (model.selectedEpisodeIndex != null) {
            model.episodeUrls.clear()
            Episode episode = model.episodes[model.selectedEpisodeIndex]
            List<EpisodeUrl> urls = simpletvService.getEpisodeUrls(episode, model.dvrs[model.selectedDvrIndex].mediaServerId, model.useLocalUrl)
            model.episodeUrls.addAll(urls)
        }
    }

    def download = { e ->
        log.info("downloading!")
        if (model.selectedEpisodeIndex != null) {
            EpisodeUrl episodeUrl = model.episodeUrls[model.selectedUrlIndex]
            log.info(episodeUrl.url)
            simpletvService.downloadEpisode(episodeUrl.url, model)
        }
    }
}
