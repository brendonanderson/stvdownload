package simpletv


class Episode {
    String title
    String groupId
    String itemId
    String instanceId
    String date
    Integer season
    Integer episode

	/**
	 * Get the string showing season and episode. Season and episode will both
	 * be padded with leading zeroes to two characters. Missing information
	 * will be replaced with sentinel values.
	 * <p/>
	 * Some examples:
	 * <ul>
	 *     <li>s01e01 - padded with leading zeroes</li>
	 *     <li>s10e10 - two digist numbers are not padded</li>
	 *     <li>s01e100 - three digits will display fully</li>
	 *     <li>sXXe01 - missing season number</li>
	 *     <li>s01eYY - missing episode number</li>
	 *     <li>sXXeYY - missing both season and episode</li>
	 * </ul>
	 * 
	 * @return The properly formatted/padded string with no leading or trailing
	 * spaces.
	 */
	private String getSeasonEpisodeToken(){
		def episode = (episode?:"YY").toString().padLeft(2, "0")
		return "s${getPaddedSeason()}e${episode}"
	}

	/**
	 * Also see documentation on #getSeasonEpisodeToken for examples.
	 * 
	 * @return Get the season, padded with to two characters. Leading zeroes if
	 * needed but if season is unknown it will be XX
	 */
	String getPaddedSeason()
	{
		return (season?:"XX").toString().padLeft(2, "0")
	}

    String toString() {
		// the format of ' - ' (whitespaces included) is expected by Plex
		// if changed, be sure to account for "plex compat" mode
		def se = getSeasonEpisodeToken()
        "${se} - ${title}"
    }
}
