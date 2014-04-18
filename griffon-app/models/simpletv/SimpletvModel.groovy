package simpletv

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.SortedList


class SimpletvModel {
    @Bindable String username
    @Bindable String password
    @Bindable Double downloadPct = new Double(0)
    @Bindable Integer selectedDvrIndex
    Integer selectedEpisodeIndex
    Integer selectedUrlIndex
    @Bindable String saveLocation
    @Bindable Boolean useLocalUrl = true

    Boolean connected = false


    SortedList<Dvr> dvrs = new SortedList(new BasicEventList(), {a,b -> a.name.toLowerCase() <=> b.name.toLowerCase()} as Comparator)
    SortedList<Show> shows = new SortedList(new BasicEventList(), {a,b -> a.name.toLowerCase() <=> b.name.toLowerCase()} as Comparator)
    SortedList<Episode> episodes = new SortedList(new BasicEventList(), {a,b -> (a.season <=> b.season ?: a.episode <=> b.episode)} as Comparator)
    SortedList<EpisodeUrl> episodeUrls = new SortedList(new BasicEventList(), {a,b -> a.toString() <=> b.toString()} as Comparator)

}