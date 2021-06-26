package com.rahul.helpingvoice.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.viewmodel.SearchViewModel

class PodcastListAdapter(
    private var podcastSummaryViewList: List<SearchViewModel.PodcastSummaryViewData>?,
    private val podcastListAdapterListener: PodcastListAdapterListener,
    private val parentActivity: Activity
) : RecyclerView.Adapter<PodcastListAdapter.ViewHolder>() {
    interface PodcastListAdapterListener {
        fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData)
    }

    inner class ViewHolder(
        v: View,
        private val podcastListAdapterListener: PodcastListAdapterListener
    ) : RecyclerView.ViewHolder(v) {
        var podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData? = null
        val nameTextView: TextView =
            v.findViewById(R.id.podcastNameTextView)
        val lastUpdatedTextView: TextView =
            v.findViewById(R.id.podcastLastUpdatedTextView)
        val linearLayout: LinearLayout = v.findViewById(R.id.linearLayout)
        val podcastImageView: ImageView =
            v.findViewById(R.id.podcastImage)

        init {
            v.setOnClickListener {
                podcastSummaryViewData?.let {
                    podcastListAdapterListener.onShowDetails(it)
                }
            }
        }
    }

    fun setSearchData(podcastSummaryViewData: List<SearchViewModel.PodcastSummaryViewData>) {
        podcastSummaryViewList = podcastSummaryViewData
        this.notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PodcastListAdapter.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false),
            podcastListAdapterListener
        )
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val searchViewList = podcastSummaryViewList ?: return
        val searchView = searchViewList[position]
        holder.podcastSummaryViewData = searchView
        holder.linearLayout.animation =
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.corner_anim)
        holder.nameTextView.text = searchView.name
        holder.lastUpdatedTextView.text = searchView.lastUpdated.toString()
        Glide.with(parentActivity)
            .load(searchView.imageUrl)
            .into(holder.podcastImageView)
    }
}