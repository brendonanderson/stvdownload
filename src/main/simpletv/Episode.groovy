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
        "${title} - s${season?:"XX"}e${episode?:"YY"}"
    }
}
