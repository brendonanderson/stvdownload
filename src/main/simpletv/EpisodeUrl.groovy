package simpletv

class EpisodeUrl {
    String url

    String toString() {
        if (url.contains("tv.500000") || url.contains("tv.800000")) {
            return "Good"
        } else if (url.contains("tv.1500000")) {
            return "Better"
        } else if (url.contains("tv.2000000")) {
            return "Better"
        } else if (url.contains("tv.4500000")) {
            return "Best"
        }
    }
}
