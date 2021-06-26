package  com.rahul.helpingvoice.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.jobdispatcher.*
import com.rahul.helpinghand.ui.EpisodePlayerFragment
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.Service.EpisodeUpdateService
import com.rahul.helpingvoice.Service.FeedService
import com.rahul.helpingvoice.Service.ItunesService
import com.rahul.helpingvoice.adapter.PodcastListAdapter
import com.rahul.helpingvoice.db.PodPlayDatabase
import com.rahul.helpingvoice.repository.ItunesRepo
import com.rahul.helpingvoice.repository.PodcastRepo
import com.rahul.helpingvoice.viewmodel.PodcastViewModel
import com.rahul.helpingvoice.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*


class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.onPodcastDetailsListener {

    val TAG = javaClass.simpleName
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private lateinit var podcastViewModel: PodcastViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_podcast)
        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        addBackStackListener()
        scheduleJobs()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        searchMenuItem = menu.findItem(R.id.search_item)
        searchMenuItem.setOnActionExpandListener(object :
            MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(p0: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem?): Boolean {
                showSubscribedPodcasts()
                return true
            }
        })
        val searchView = searchMenuItem?.actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (PodcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }
        return true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            performSearch(query.toString())
        }
        val podcastFeedUrl = intent.getStringExtra(EpisodeUpdateService.EXTRA_FEED_URL)
        if (podcastFeedUrl != null) {
            podcastViewModel.setActivePodcast(podcastFeedUrl) {
                it?.let { podcastSummaryView ->
                    onShowDetails(podcastSummaryView)
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }


    private fun performSearch(term: String) {
        showProgressBar()
        searchViewModel.searchPodcast(term) { results ->
            Log.d(TAG, "RESULTS : $results")
            hideProgressBar()
            toolbar.title = getString(R.string.search_results)
            podcastListAdapter.setSearchData(results)
        }
    }

    private fun setupViewModels() {

        val service = ItunesService.instance
        searchViewModel = ViewModelProviders.of(this).get(
            SearchViewModel::class.java
        )
        searchViewModel.itunesRepo = ItunesRepo(service)

        podcastViewModel = ViewModelProviders.of(this)
            .get(PodcastViewModel::class.java)
        val rssService = FeedService.instance
        val db = PodPlayDatabase.getInstance(this)
        val podcastDao = db.podcastDao()
        podcastViewModel.podcastRepo = PodcastRepo(rssService, podcastDao)
    }

    private fun updateControls() {
        PodcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        PodcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration =
            DividerItemDecoration(PodcastRecyclerView.context, layoutManager.orientation)
        PodcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        PodcastRecyclerView.adapter = podcastListAdapter
    }

    private fun showProgressBar() {
        progress_bar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progress_bar.visibility = View.INVISIBLE
    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        val feedUrl = podcastSummaryViewData.feedUrl
        showProgressBar()
        podcastViewModel.getPodcast(podcastSummaryViewData) {
            hideProgressBar()
            if (it != null) {
                searchMenuItem.isVisible = false
                showDetailsFragment()
            } else {
                showError("Error loading feed $feedUrl")
            }
        }
    }

    companion object {
        private val TAG_DETAILS_FRAGMENT = "DetailsFragment"
        private const val TAG_PLAYER_FRAGMENT = "PlayerFragment"
        private val TAG_EPISODE_UPDATE_JOB = "com.podcastapp.episodes"
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {
        var podcastDetailsFragment =
            supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {
        val podcastDetailsFragment = createPodcastDetailsFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.podcastDetailsContainer, podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
        PodcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                PodcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onSubscribe() {
        podcastViewModel.saveActivePodcat()
        supportFragmentManager.popBackStack()
    }

    private fun showSubscribedPodcasts() {
        val podcasts = podcastViewModel.getPodcasts()?.value
        if (podcasts != null) {
            toolbar.title = getString(R.string.subscribed_podcasts)
            podcastListAdapter.setSearchData(podcasts)
        }
    }

    private fun setupPodcastListView() {
        podcastViewModel.getPodcasts()?.observe(this, Observer {
            if (it != null) {
                showSubscribedPodcasts()
            }
        })
    }

    override fun onUnsubscribe() {
        podcastViewModel.deleteActivePodcast()
        supportFragmentManager.popBackStack()
    }

    private fun scheduleJobs() {

        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val oneHourInSeconds = 60 * 60
        val tenMinutesInSeconds = 60 * 10
        val episodeUpdateJob = dispatcher.newJobBuilder()
            .setService(EpisodeUpdateService::class.java)
            .setTag(TAG_EPISODE_UPDATE_JOB)
            .setRecurring(true)
            .setTrigger(
                Trigger.executionWindow(
                    oneHourInSeconds,
                    (oneHourInSeconds + tenMinutesInSeconds)
                )
            )
            .setLifetime(Lifetime.FOREVER)
            .setConstraints(
                //Constraint.ON_UNMETERED_NETWORK,
                Constraint.DEVICE_CHARGING
            )
            .build()
        dispatcher.mustSchedule(episodeUpdateJob)
    }

    override fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData) {
        podcastViewModel.activeEpisodeViewData = episodeViewData
        showPlayerFragment()
    }

    private fun createEpisodePlayerFragment(): EpisodePlayerFragment {
        var episodePlayerFragment =
            supportFragmentManager.findFragmentByTag(TAG_PLAYER_FRAGMENT) as
                    EpisodePlayerFragment?
        if (episodePlayerFragment == null) {
            episodePlayerFragment = EpisodePlayerFragment.newInstance()
        }
        return episodePlayerFragment
    }

    private fun showPlayerFragment() {
        val episodePlayerFragment = createEpisodePlayerFragment()
        supportFragmentManager.beginTransaction().replace(
            R.id.podcastDetailsContainer,
            episodePlayerFragment,
            TAG_PLAYER_FRAGMENT
        ).addToBackStack("PlayerFragment").commit()
        PodcastRecyclerView.visibility = View.INVISIBLE
        searchMenuItem.isVisible = false
    }
}