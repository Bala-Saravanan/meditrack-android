package com.example.meditrack.ui.reports

import android.content.res.ColorStateList
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
import com.example.meditrack.data.local.model.ScheduleReportCardItem
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleReportCardAdapter(
    private val onViewReportClicked: (scheduleId: Long) -> Unit
) : ListAdapter<ScheduleReportCardItem, ScheduleReportCardAdapter.CardViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_report_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_schedule_name)
        private val tvDateRange: TextView = itemView.findViewById(R.id.tv_schedule_date_range)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_schedule_status_badge)
        private val tvAdherencePercent: TextView = itemView.findViewById(R.id.tv_adherence_percent)
        private val barTaken: View = itemView.findViewById(R.id.view_bar_taken)
        private val barSkipped: View = itemView.findViewById(R.id.view_bar_skipped)
        private val barUnconfirmed: View = itemView.findViewById(R.id.view_bar_unconfirmed)
        private val tvTaken: TextView = itemView.findViewById(R.id.tv_taken_count)
        private val tvSkipped: TextView = itemView.findViewById(R.id.tv_skipped_count)
        private val tvUnconfirmed: TextView = itemView.findViewById(R.id.tv_unconfirmed_count)
        private val btnViewReport: MaterialButton = itemView.findViewById(R.id.btn_view_report)

        fun bind(item: ScheduleReportCardItem) {
            val context = itemView.context
            tvName.text = item.scheduleName

            val startStr = dateFormat.format(Date(item.startDate))
            val endStr = if (item.endDate != null && item.endDate > 0) {
                dateFormat.format(Date(item.endDate))
            } else {
                "Ongoing"
            }
            tvDateRange.text = "$startStr – $endStr"

            // Status Badge
            tvStatusBadge.text = item.scheduleStatus
            when (item.scheduleStatus) {
                "Active" -> {
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
                    tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_taken)
                }
                "Completed" -> {
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.secondary))
                    tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_due)
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_fixed))
                }
                else -> {
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.outline))
                    tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill_due)
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.surface_container_high))
                }
            }

            // Summary Stats
            val summary = item.adherenceSummary
            tvAdherencePercent.text = "${summary.adherencePercentage}% Adherence"

            // Segmented Progress Bar
            val eligible = summary.eligibleDoses
            if (eligible > 0) {
                val takenWeight = (summary.takenCount.toFloat() / eligible.toFloat()) * 100f
                val skippedWeight = (summary.skippedCount.toFloat() / eligible.toFloat()) * 100f
                val unconfWeight = (summary.unconfirmedCount.toFloat() / eligible.toFloat()) * 100f

                setBarWeight(barTaken, takenWeight)
                setBarWeight(barSkipped, skippedWeight)
                setBarWeight(barUnconfirmed, unconfWeight)
            } else {
                setBarWeight(barTaken, 0f)
                setBarWeight(barSkipped, 0f)
                setBarWeight(barUnconfirmed, 100f)
            }

            tvTaken.text = "${summary.takenCount} Taken"
            tvSkipped.text = "${summary.skippedCount} Skipped"
            tvUnconfirmed.text = "${summary.unconfirmedCount} Unconfirmed"

            btnViewReport.setOnClickListener {
                onViewReportClicked(item.scheduleId)
            }
        }

        private fun setBarWeight(view: View, weight: Float) {
            val params = view.layoutParams as LinearLayout.LayoutParams
            params.weight = weight.coerceAtLeast(0f)
            view.layoutParams = params
            view.visibility = if (weight > 0f) View.VISIBLE else View.GONE
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleReportCardItem>() {
        override fun areItemsTheSame(oldItem: ScheduleReportCardItem, newItem: ScheduleReportCardItem): Boolean {
            return oldItem.scheduleId == newItem.scheduleId
        }

        override fun areContentsTheSame(oldItem: ScheduleReportCardItem, newItem: ScheduleReportCardItem): Boolean {
            return oldItem == newItem
        }
    }
}
