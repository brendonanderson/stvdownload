package simpletv

class SimpletvController {
    SimpletvModel model
    def view
    SimpletvService simpletvService

    def login = {
        model.selectedEpisodeIndex = null
        simpletvService.login(model.username, model.password)
        model.connected = true
        List<Show> showList = simpletvService.getShows()
        model.shows.clear()
        model.shows.addAll(showList)
    }

    def getEpisodes = { Integer index ->
        println model.shows[index].name
        model.episodes.clear()
        model.episodeUrls.clear()
        model.selectedEpisodeIndex = null
        model.episodes.addAll(simpletvService.getEpisodes(model.shows[index]))
    }

    def getEpisodeUrls = { Integer index ->
        if (model.selectedEpisodeIndex != null) {
            model.episodeUrls.clear()
            Episode episode = model.episodes[model.selectedEpisodeIndex]
            List<EpisodeUrl> urls = simpletvService.getEpisodeUrls(episode)
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
