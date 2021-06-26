package com.rahul.helpingvoice.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.rahul.helpingvoice.R
import com.rahul.helpingvoice.util.DateUtils
import com.rahul.helpingvoice.util.HtmlUtils
import com.rahul.helpingvoice.viewmodel.PodcastViewModel

class EpisodeListAdapter(
    private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?,
    private val episodeListAdapterListener: EpisodeListAdapterListener
) : RecyclerView.Adapter<EpisodeListAdapter.ViewHolder>() {


    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }


    class ViewHolder(v: View, private val episodeListAdapterListener: EpisodeListAdapterListener) :
        RecyclerView.ViewHolder(v) {
        var episodeViewData: PodcastViewModel.EpisodeViewData? = null
        val titleTextView: TextView = v.findViewById(R.id.titleView)
        val descTextView: TextView = v.findViewById(R.id.descView)
        val contraintView: ConstraintLayout = v.findViewById(R.id.constraintView)
        val durationTextView: TextView = v.findViewById(R.id.durationView)
        val releaseDateTextView: TextView = v.findViewById(R.id.releaseDateView)

        init {
            v.setOnClickListener {
                episodeViewData?.let {
                    episodeListAdapterListener.onSelectedEpisode(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EpisodeListAdapter.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.episode_item, parent, false),
            episodeListAdapterListener
        )
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]
        holder.episodeViewData = episodeView
        holder.contraintView.animation =
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.creative_anim)
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(episodeView.description ?: "")
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releaseDate?.let {
            DateUtils.dateToShortDate(it)
        }
    }


}