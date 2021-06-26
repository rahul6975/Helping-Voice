package  com.rahul.helpingvoice.ui

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.rahul.helpingvoice.helper.PreferenceHelper
import com.rahul.helpingvoice.repository.ItunesRepo
import com.rahul.helpingvoice.repository.PodcastRepo
import com.rahul.helpingvoice.viewmodel.PodcastViewModel
import com.rahul.helpingvoice.viewmodel.SearchViewModel
import kotlinx.android.synthetic.main.activity_podcast.*
import org.w3c.dom.Text
import java.util.*


class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener,
    PodcastDetailsFragment.onPodcastDetailsListener, TextToSpeech.OnInitListener {

    val TAG = javaClass.simpleName
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var searchMenuItem: MenuItem
    private lateinit var podcastViewModel: PodcastViewModel
    private val RQ_SPEECH = 102

    private val searchRequestCode = 103
    private val getIndexRequest = 104

    private var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_podcast)
//        setupToolbar()
        setupViewModels()
        updateControls()
        setupPodcastListView()
        addBackStackListener()
        scheduleJobs()
        PreferenceHelper.getSharedPreferences(this)
        textToSpeech = TextToSpeech(this, this)

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech!!.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                speck()
            }
        }
    }

    fun speck() {
        val name = PreferenceHelper.getStringFromPreference("name")
        val text =
            "Hello, $name. You are on the home page now, So how can i help you"
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                getAnswer()
            }

            override fun onError(utteranceId: String?) {
            }

        })
    }

    fun getAnswer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, RQ_SPEECH)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RQ_SPEECH && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0).toString()
            val listOfAns = answer.split(" ")
            for (element in listOfAns) {
                if (element == "search") {
                    askWhichSearch()
                    break
                }
            }
        } else if (requestCode == searchRequestCode && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0).toString()
            ohkSearchingFor(answer)
        } else if (requestCode == getIndexRequest && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0).toString()
            if (answer == "first") {
                val whichSearch = "Okay opening the first one"
                val hashMap = HashMap<String, String>()
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                textToSpeech!!.speak(whichSearch, TextToSpeech.QUEUE_FLUSH, hashMap)
                textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {

                    }

                    override fun onDone(utteranceId: String?) {

                    }

                    override fun onError(utteranceId: String?) {
                    }

                })
            }
        }
    }

    fun askWhichSearch() {
        val whichSearch = "What do you want to me to search for you"
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(whichSearch, TextToSpeech.QUEUE_FLUSH, hashMap)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Toast.makeText(applicationContext, "what u want to me search", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onDone(utteranceId: String?) {
                getSearchAnswer()
            }

            override fun onError(utteranceId: String?) {
            }

        })

    }

    private fun ohkSearchingFor(query: String) {
        val whichSearch = "Okay! hold tight. I'm searching for $query"
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(whichSearch, TextToSpeech.QUEUE_FLUSH, hashMap)
        searchQuery(query)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
//                searchQuery(query)

            }

            override fun onError(utteranceId: String?) {
            }

        })
    }

    fun getSearchAnswer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, searchRequestCode)
        }
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
        val searchView = searchMenuItem.actionView as SearchView
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
//            performSearch(query.toString())
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
//    private fun setupToolbar() {
//        setSupportActionBar(toolbar)
//    }

    private fun searchQuery(term: String) {
        showProgressBar()
        searchViewModel.searchPodcast(term) { results ->
            Log.d(TAG, "RESULTS : $results")
            hideProgressBar()
            toolbar.title = getString(R.string.search_results)
            podcastListAdapter.setSearchData(results)
            askWhichToOpen(results)
        }
    }

    private fun askWhichToOpen(results: List<SearchViewModel.PodcastSummaryViewData>) {
        val iFoundFew = "I found new results based on your input, top 3 are      "
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(iFoundFew, TextToSpeech.QUEUE_FLUSH, hashMap)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
                val first = "First, ${results.get(0).name.toString()}"
                val hashmap1 = HashMap<String, String>()
                hashmap1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                textToSpeech!!.speak(first, TextToSpeech.QUEUE_FLUSH, hashmap1)
                textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {

                    }

                    override fun onDone(utteranceId: String?) {
                        val second = "second, ${results.get(1).name.toString()}"
                        val hashmap2 = HashMap<String, String>()
                        hashmap2.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                        textToSpeech!!.speak(second, TextToSpeech.QUEUE_FLUSH, hashmap2)
                        textToSpeech!!.setOnUtteranceProgressListener(object :
                            UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {

                            }

                            override fun onDone(utteranceId: String?) {
                                val third = "third, ${results.get(2).name.toString()}"
                                val hashmap3 = HashMap<String, String>()
                                hashmap3.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                                textToSpeech!!.speak(third, TextToSpeech.QUEUE_FLUSH, hashmap3)
                                textToSpeech!!.setOnUtteranceProgressListener(object :
                                    UtteranceProgressListener() {
                                    override fun onStart(utteranceId: String?) {

                                    }

                                    override fun onDone(utteranceId: String?) {
                                        val askWhich = "Which one should i open ?"
                                        val hashmap4 = HashMap<String, String>()
                                        hashmap4.put(
                                            TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
                                            "good"
                                        )
                                        textToSpeech!!.speak(
                                            askWhich,
                                            TextToSpeech.QUEUE_FLUSH,
                                            hashmap4
                                        )
                                        textToSpeech!!.setOnUtteranceProgressListener(object :
                                            UtteranceProgressListener() {
                                            override fun onStart(utteranceId: String?) {

                                            }

                                            override fun onDone(utteranceId: String?) {
                                                getIndex()

                                            }

                                            override fun onError(utteranceId: String?) {
                                            }


                                        })
                                    }

                                    override fun onError(utteranceId: String?) {
                                    }


                                })

                            }

                            override fun onError(utteranceId: String?) {
                            }


                        })
                    }

                    override fun onError(utteranceId: String?) {
                    }


                })
            }

            override fun onError(utteranceId: String?) {
            }

        })
    }

    private fun getIndex() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, getIndexRequest)
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

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech!!.stop()
        textToSpeech!!.shutdown()
    }

}