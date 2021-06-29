package com.rahul.helpinghand.ui

import android.animation.ValueAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.Service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.rahul.helpingvoice.Service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.rahul.helpingvoice.Service.PodplayMediaService
import com.rahul.helpingvoice.util.HtmlUtils
import com.rahul.helpingvoice.util.SpeedUtil
import com.rahul.helpingvoice.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_episode_player.*
import java.util.*

class EpisodePlayerFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private lateinit var playToggleButton: Button
    private lateinit var speedButton: Button
    private var playerSpeed: Float = 1.0f
    private var textToSpeech: TextToSpeech? = null
    private lateinit var forwardButton: ImageButton
    private lateinit var replayButton: ImageButton
    private var moveHomePage = 206

    private var episodeDuration: Long = 0
    private lateinit var endTimeTextView: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var seekBar: SeekBar
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()
        if (timeRemaining < 0) {
            return;
        }
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt()
        )
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
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

    private fun speck() {
        val text =
            "Rahul, we are in the second episode and now playing"
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

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(
            episodeDuration / 1000
        )
        seekBar.max = episodeDuration.toInt()
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f
        if (!SpeedUtil.checkSpeed(playerSpeed)) {
            playerSpeed = 0.75f
        }
        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)
        speedButton.text = "${playerSpeed}x"
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            print("metadata changed to ${metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)}")
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            print("state changed to $state")
            val state = state ?: return
            handleStateChange(state.getState(), state.position, state.playbackSpeed)
        }
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()
            registerMediaController(mediaBrowser.sessionToken)
            println("onConnected")
            updateControlsFromController()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
        }
    }


    private fun togglePlayPause() {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }

    private fun sayMove() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "say something")
            startActivityForResult(i, moveHomePage)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == moveHomePage && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val answer = result?.get(0).toString()
            val listOfAns = answer.split(" ")
            for (element in listOfAns) {
                if (element == "home") {
                    activity?.onBackPressed()
                    break
                }
            }
        }
    }

    private fun setupControls() {
        playToggleButton.setOnClickListener {
            togglePlayPause()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener {

                val text =
                    "how can i help you ?"
                val hashMap = HashMap<String, String>()
                hashMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "good")
                textToSpeech!!.speak(text, TextToSpeech.QUEUE_FLUSH, hashMap)
                textToSpeech!!.setOnUtteranceProgressListener(object :
                    UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                    }

                    override fun onDone(utteranceId: String?) {
                        sayMove()
                    }

                    override fun onError(utteranceId: String?) {
                    }

                })

//                changeSpeed()
            }
        } else {
            speedButton.visibility = View.INVISIBLE
        }
        forwardButton.setOnClickListener {
            seekBy(30)
        }
        replayButton.setOnClickListener {
            seekBy(-10)
        }
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar, progress: Int,
                    fromUser: Boolean
                ) {
                    currentTimeTextView.text = DateUtils.formatElapsedTime(
                        (progress / 1000).toLong()
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = false
                    val fragmentActivity = activity as FragmentActivity
                    val controller =
                        MediaControllerCompat.getMediaController(fragmentActivity)
                    if (controller.playbackState != null) {
                        controller.transportControls.seekTo(seekBar.progress.toLong())
                    } else {
                        seekBar.progress = 0
                    }
                }
            })
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {
        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }
        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying
        val progress = position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"
        if (isPlaying) {
            animateScrubber(progress, speed)
        }
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity,
            ComponentName(fragmentActivity, PodplayMediaService::class.java),
            MediaBrowserCallBacks(), null
        )
    }

    private fun setupViewModel() {
        val fragmentActivity = activity as FragmentActivity
        podcastViewModel =
            ViewModelProviders.of(fragmentActivity).get(PodcastViewModel::class.java)
    }

    private fun updateControls() {
        episodeTitleTextView.text = podcastViewModel.activeEpisodeViewData?.title
        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""
        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)
        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()
        val fragmentActivity = activity as FragmentActivity
        Glide.with(fragmentActivity).load(podcastViewModel.activePodcastViewData?.imageUrl)
            .into(episodeImageView)
    }

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setupViewModel()
        initMediaBrowser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_episode_player,
            container, false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playToggleButton = view.findViewById(R.id.playToggleButton)
        speedButton = view.findViewById(R.id.speedButton)
        forwardButton = view.findViewById(R.id.forwardButton)
        replayButton = view.findViewById(R.id.replayButton)
        endTimeTextView = view.findViewById(R.id.endTimeTextView)
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView)
        seekBar = view.findViewById(R.id.seekBar)
        textToSpeech = TextToSpeech(requireContext(), this)
        setupControls()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (mediaBrowser.isConnected) {
            val fragmentActivity = activity as FragmentActivity
            if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                registerMediaController(mediaBrowser.sessionToken)
            }
            updateControlsFromController()
        } else {
            mediaBrowser.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()
        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity)
                    .unregisterCallback(it)
            }
        }
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position, playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_TITLE,
            episodeViewData.title
        )
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            viewData.feedTitle
        )
        bundle.putString(
            MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
            viewData.imageUrl
        )
        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity
        val mediaController = MediaControllerCompat(fragmentActivity, token)
        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)
        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }


}