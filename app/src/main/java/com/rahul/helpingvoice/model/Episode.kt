package com.rahul.helpinghand.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
        foreignKeys = [
                ForeignKey(entity = Podcast::class,
                parentColumns = ["id"],
                childColumns = ["podcastId"],
                onDelete = ForeignKey.CASCADE)
        ],
        indices = [Index("podcastId")]
)
data class Episode (
        @PrimaryKey
        var guid: String = "",
        var podcastId: Long? = null,
        var title: String = "",
        var description: String = "",
        var mediaUrl: String = "",
        var mimeType: String = "",
        var releaseDate: Date = Date(),
        var duration: String = ""
)

/*
* guid : Unique identifier provided in the RSS feed for an episode.
• title : The name of the episode.
• description : A description of the episode.
• mediaUrl : The location of the episode media. This is either an audio or video file.
• mimeType : Determines the type of file located at mediaUrl .
• releaseDate : Date the episode was released.
• duration : Duration of the episode as provided in the RSS feed.
*/