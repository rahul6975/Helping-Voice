package com.rahul.helpinghand.Service

import java.util.*

data class RSSFeedResponse(var title: String = "",
                           var description: String = "",
                           var summary: String = "",
                           var lastUpdated: Date = Date(),
                           var episodes: MutableList<EpisodeResponse>? = null){
    data class EpisodeResponse(
            var title: String? = null,
            var link: String? = null,
            var description: String? = null,
            var guid: String? = null,
            var pubDate: String? = null,
            var duration: String? = null,
            var url: String? = null,
            var type: String? = null)
}

/*
• title : The podcast title.
• description : The podcast description.
• summary : The podcast summary.
• lastUpdated : The last update date for the podcast.
• episodes : The list of episodes for the podcast.
• title : The episode title.
• link : URL link to the episode media file.
• description : The episode description.
• guid : Unique ID for the episode.
• pubDate : Publication date of the episode.
• duration : Episode duration.
• url : URL to the episode landing page.
• type : Type of media for the episode ('audio' or 'video').
*/