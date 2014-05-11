package simpletv


class Episode {
    String title
    String groupId
    String itemId
    String instanceId
    String date
    Integer season
    Integer episode

    String toString() {
        def sss = (season?:"XX").toString().padLeft(2, "0")
        def eee = (episode?:"YY").toString().padLeft(2, "0")
        "s${sss}e${eee}-${title}"
    }
}
