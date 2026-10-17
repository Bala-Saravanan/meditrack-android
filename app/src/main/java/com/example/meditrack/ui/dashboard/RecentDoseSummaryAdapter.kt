package com.example.meditrack.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary

class RecentDoseSummaryAdapter(
    private val onDetailsClicked: (ScheduleAdherenceSummary) -> Unit
) : ListAdapter<ScheduleAdherenceSummary, RecentDoseSummaryAdapter.SummaryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_dose_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_summary_schedule_name)
        private val tvSub: TextView = itemView.findViewById(R.id.tv_summary_schedule_sub)
        private val tvAdherence: TextView = itemView.findViewById(R.id.tv_adherence_percent)
        private val segTaken: View = itemView.findViewById(R.id.seg_taken)
        private val segSkipped: View = itemView.findViewById(R.id.seg_skipped)
        private val segUnconfirmed: View = itemView.findViewById(R.id.seg_unconfirmed)
        private val tvTaken: TextView = itemView.findViewById(R.id.tv_count_taken)
        private val tvSkipped: TextView = itemView.findViewById(R.id.tv_count_skipped)
        private val tvUnconfirmed: TextView = itemView.findViewById(R.id.tv_count_unconfirmed)
        private val btnDetails: View = itemView.findViewById(R.id.btn_summary_details)

        fun bind(summary: ScheduleAdherenceSummary) {
            tvName.text = summary.scheduleName
            tvSub.text = "Past 4 Days • ${summary.totalScheduled} total doses"

            if (summary.adherencePercentage == 100 && summary.totalScheduled > 0) {
                tvAdherence.text = "100% Perfect Record"
                tvAdherence.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_taken))
            } else {
                tvAdherence.text = "${summary.adherencePercentage}% Adherent"
                tvAdherence.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
            }

            // Adjust segmented bar weights
            val total = summary.totalScheduled.coerceAtLeast(1)
            val takenWeight = (summary.takenCount.toFloat() / total) * 100f
            val skippedWeight = (summary.skippedCount.toFloat() / total) * 100f
            val unconfirmedWeight = (summary.unconfirmedCount.toFloat() / total) * 100f

            val lpTaken = segTaken.layoutParams as LinearLayout.LayoutParams
            lpTaken.weight = takenWeight
            segTaken.layoutParams = lpTaken
            segTaken.visibility = if (takenWeight > 0f) View.VISIBLE else View.GONE

            val lpSkipped = segSkipped.layoutParams as LinearLayout.LayoutParams
            lpSkipped.weight = skippedWeight
            segSkipped.layoutParams = lpSkipped
            segSkipped.visibility = if (skippedWeight > 0f) View.VISIBLE else View.GONE

            val lpUnconf = segUnconfirmed.layoutParams as LinearLayout.LayoutParams
            lpUnconf.weight = unconfirmedWeight
            segUnconfirmed.layoutParams = lpUnconf
            segUnconfirmed.visibility = if (unconfirmedWeight > 0f) View.VISIBLE else View.GONE

            // Counts
            tvTaken.text = summary.takenCount.toString()
            tvSkipped.text = summary.skippedCount.toString()
            tvUnconfirmed.text = summary.unconfirmedCount.toString()

            btnDetails.setOnClickListener { onDetailsClicked(summary) }
            itemView.setOnClickListener { onDetailsClicked(summary) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleAdherenceSummary>() {
        override fun areItemsTheSame(oldItem: ScheduleAdherenceSummary, newItem: ScheduleAdherenceSummary): Boolean =
            oldItem.scheduleId == newItem.scheduleId

        override fun areContentsTheSame(oldItem: ScheduleAdherenceSummary, newItem: ScheduleAdherenceSummary): Boolean =
            oldItem == newItem
    }
}
