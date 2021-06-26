package com.example.podcastapp.ui

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.podcastapp.R
import com.example.podcastapp.Service.PodplayMediaService
import com.example.podcastapp.adapter.EpisodeListAdapter
import com.example.podcastapp.viewmodel.PodcastViewModel
import kotlinx.android.synthetic.main.fragment_podcast_details.*

class PodcastDetailsFragment : Fragment(), EpisodeListAdapter.EpisodeListAdapterListener {

    private lateinit var podcastViewModel: PodcastViewModel
    private lateinit var feedImageView : ImageView
    private lateinit var episodeListAdapter: EpisodeListAdapter
    private var listener : onPodcastDetailsListener ? = null
    private var menuItem : MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedImageView = view.findViewById(R.id.feedImageView)
        setupViewModel()
        updateControls()
        setupControls()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_podcast_details, container, false)
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop(){
        super.onStop()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.menu_details, menu)
        menuItem = menu?.findItem(R.id.menu_feed_action)
        updateMenuItem()
    }

    private fun setupViewModel(){
        activity?.let{
            podcastViewModel = ViewModelProviders.of(activity!!)
                    .get(PodcastViewModel::class.java)
        }
    }

    private fun updateControls(){
        val viewData = podcastViewModel.activePodcastViewData ?: return
        feedTitleTextView?.text = viewData.feedTitle
        feedDescTextView?.text = viewData.feedDesc
        activity?.let{activity ->
            Glide.with(activity).load(viewData.imageUrl)
                    .into(feedImageView)
        }
    }

    companion object{
        fun newInstance(): PodcastDetailsFragment{
            return PodcastDetailsFragment()
        }
    }

    private fun setupControls(){
        feedDescTextView.movementMethod = ScrollingMovementMethod()

        episodeRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(activity)
        episodeRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(episodeRecyclerView.context, layoutManager.orientation)

        episodeRecyclerView.addItemDecoration(dividerItemDecoration)

        episodeListAdapter = EpisodeListAdapter(podcastViewModel.activePodcastViewData?.episodes, this)

        episodeRecyclerView.adapter = episodeListAdapter

    }

    interface onPodcastDetailsListener {
        fun onSubscribe()
        fun onUnsubscribe()
        fun onShowEpisodePlayer(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is onPodcastDetailsListener){
            listener = context
        }else{
            throw RuntimeException(context!!.toString() + " must implement onPodcastDetailsListener")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId ){
            R.id.menu_feed_action -> {
                podcastViewModel.activePodcastViewData?.feedUrl?.let{
                    if(podcastViewModel.activePodcastViewData?.subscribed!!){
                        listener?.onUnsubscribe()
                    }else{
                        listener?.onSubscribe()
                    }
                }
                return true
            }else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateMenuItem(){
        val viewData = podcastViewModel.activePodcastViewData ?: return
        menuItem?.title =
                if(viewData.subscribed ){
                            getString(R.string.unsubscribe)
                        }else{
                        getString(R.string.subscribe)
                }
    }



    override fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData) {
        listener?.onShowEpisodePlayer(episodeViewData)
    }

}