package com.rahul.helpingvoice.repository
import androidx.lifecycle.LiveData
import com.example.podcastapp.model.Podcast
import com.rahul.helpingvoice.Service.FeedService
import com.rahul.helpingvoice.Service.RSSFeedResponse
import com.rahul.helpingvoice.db.PodcastDao
import com.rahul.helpingvoice.model.Episode
import com.rahul.helpingvoice.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PodcastRepo(private val feedService: FeedService, private var podcastDao: PodcastDao) {
    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)
    fun getPodcast(feedUrl: String, callback: (Podcast?) -> Unit) {
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisdoes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
                feedService.getFeed(feedUrl) { feedResponse ->
                    var podcast: Podcast? = null
                    if (feedResponse != null) {
                        podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                    }
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }
        }
    }
    private fun rssItemsToEpisodes(episodeResponses: List<RSSFeedResponse.EpisodeResponse>): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }
    private fun rssResponseToPodcast(
        feedUrl: String, imageUrl:
        String, rssResponse: RSSFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description
        return Podcast(
            null,
            feedUrl,
            rssResponse.title,
            description,
            imageUrl,
            rssResponse.lastUpdated,
            episodes = rssItemsToEpisodes(items)
        )
    }
    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)
            for (episode in podcast.episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }
    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcast()
    }
    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }
    private fun getNewEpisodes(localPodcast: Podcast, callback: (List<Episode>) -> Unit) {
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {
                val remotePodcast =
                    rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, response)
                remotePodcast?.let {
                    val localEpisodes = podcastDao.loadEpisdoes(localPodcast.id!!)
                    val newEpisodes =
                        remotePodcast.episodes.filter { episode -> localEpisodes.find { episode.guid == it.guid } == null }
                    callback(newEpisodes)
                }
            } else {
                callback(listOf())
            }
        }
    }
    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }
    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()
        val podcasts = podcastDao.loadPodcastsStatic()
        var processCount = podcasts.count()
        for (podcast in podcasts) {
            getNewEpisodes(podcast) { newEpisodes ->
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl,
                            podcast.feedTitle,
                            newEpisodes.count()
                        )
                    )
                }
                processCount--
                if (processCount == 0) {
                    callback(updatedPodcasts)
                }
            }
        }
    }
}