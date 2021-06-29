package com.rahul.helpingvoice.db

import android.content.Context
import androidx.room.*
import com.rahul.helpingvoice.model.Episode
import com.rahul.helpingvoice.model.Podcast
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimeStamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    @TypeConverter
    fun toTimeStamp(date: Date?): Long? {
        return (date?.time)
    }
}

@Database(entities = arrayOf(Podcast::class, Episode::class), version = 1)
@TypeConverters(Converters::class)
abstract class PodPlayDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao

    companion object {
        private var instance: PodPlayDatabase? = null
        fun getInstance(context: Context): PodPlayDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PodPlayDatabase::class.java,
                    "Podplayer"
                ).build()
            }
            return instance as PodPlayDatabase
        }
    }
}