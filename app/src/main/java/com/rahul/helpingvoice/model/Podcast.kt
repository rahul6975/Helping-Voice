package com.rahul.helpinghand.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Podcast(
        @PrimaryKey(autoGenerate = true) var id : Long? = null,
        var feedUrl: String = "",
        var feedTitle: String = "",
        var feedDesc: String = "",
        var imageUrl: String = "",
        var lastUpdated: Date = Date(),
        @Ignore
        var episodes: List<Episode> = listOf()
)

//• feedUrl : Location of the RSS feed.
//• feedTitle : Title of the podcast.
//• feedDesc : Description of the podcast.
//• imageUrl : Location of the podcast album art.
//• lastUpdated : Date the podcast was last updated.
//• episodes : List of episodes for the podcast.