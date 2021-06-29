package com.rahul.helpingvoice.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.adapter.EpisodeListAdapter
import com.rahul.helpingvoice.helper.PreferenceHelper
import com.rahul.helpingvoice.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*
import java.util.*

class PodcastDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener,
    TextToSpeech.OnInitListener {

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var feedImageView: ImageView
    private var textToSpeech: TextToSpeech? = null
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener: onPodcastDetailsListener? = null
    private var menuItem: MenuItem? = null
    private val askEpisode = 105


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)
        feedImageView = view.findViewById(R.id.feedImageView)
        setupViewModel()
        updateControls()
        setupControls()

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech!!.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                val viewData = podcastViewModel.activePodcastViewData ?: return
                speck(viewData)
            }
        }
    }

    private fun speck(viewData: PodcastViewModel.PodcastViewData) {
        val text =
            "Rahul, we are in the ${viewData.feedTitle.toString()} topic. Which episode should I open for listening ?"
        val hashMap = HashMap<String, String>()
        hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
        textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap)
        textToSpeech!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                askEpisode()
            }

            override fun onError(utteranceId: String?) {
            }

        })
    }

    private fun askEpisode() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, askEpisode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == askEpisode && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0).toString()
            if (answer == "second") {
                val text =
                    "Okay, opening second"
                val hashMap = HashMap<String, String>()
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_details, menu)
        menuItem = menu?.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    private fun setupViewModel() {
        activity?.let {
            podcastViewModel = ViewModelProviders.of(requireActivity())
                .get(PodcastViewModel::class.java)
        }
    }

    private fun updateControls() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView?.text = viewData.feedTitle
        feedDescTextView?.text = viewData.feedDesc
        activity?.let { activity ->
            Glide.with(activity).load(viewData.imageUrl)
                .into(feedImageView)
        }
//        speck(viewData)
    }


    companion object {
        fun newInstance(): PodcastDetailsFragment {
            return PodcastDetailsFragment()
        }
    }

    private fun setupControls() {
        feedDescTextView.movementMethod = ScrollingMovementMethod()

        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration =
            DividerItemDecoration(episodeRecyclerView.context, layoutManager.orientation)

        episodeRecyclerView.addItemDecoration(dividerItemDecoration)

        episodeListAdapter =
            EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)

        episodeRecyclerView.adapter = episodeListAdapter

    }

    interface onPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is onPodcastDetailsListener) {
            listener = context
        } else {
            throw RuntimeException(requireContext().toString() + " must implement onPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let {
                    if (podcastViewModel.activePodcastViewData?.subscribed!!) {
                        listener?.onUnsubscribe()
                    } else {
                        listener?.onSubscribe()
                    }
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItem() {
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title =
            if (viewData.subscribed) {
                getString(R.string.unsubscribe)
            } else {
                getString(R.string.subscribe)
            }
    }


    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }


}