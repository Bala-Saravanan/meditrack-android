package com.example.meditrack.ui.dose

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.DoseRecord
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DoseHistoryAdapter(
    private val highlightedDoseId: Long = -1L,
    private val onTakenClicked: (DoseRecord) -> Unit,
    private val onSkippedClicked: (DoseRecord) -> Unit
) : ListAdapter<DoseRecord, DoseHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dose_history_entry, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardEntry: MaterialCardView = itemView.findViewById(R.id.card_history_entry)
        private val tvMedName: TextView = itemView.findViewById(R.id.tv_history_med_name)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_history_status_badge)
        private val tvScheduledTime: TextView = itemView.findViewById(R.id.tv_history_scheduled_time)
        private val tvFoodInstruction: TextView = itemView.findViewById(R.id.tv_history_food_instruction)
        private val tvLateBadge: TextView = itemView.findViewById(R.id.tv_history_late_badge)
        private val layoutRecorded: View = itemView.findViewById(R.id.layout_history_recorded)
        private val tvRecordedTime: TextView = itemView.findViewById(R.id.tv_history_recorded_time)
        private val layoutActions: View = itemView.findViewById(R.id.layout_history_actions)
        private val btnTake: MaterialButton = itemView.findViewById(R.id.btn_history_take)
        private val btnSkip: MaterialButton = itemView.findViewById(R.id.btn_history_skip)

        fun bind(record: DoseRecord) {
            val context = itemView.context
            tvMedName.text = record.medicineName

            // Format scheduled date/time nicely
            val scheduledDateStr = if (isToday(record.scheduledAt)) {
                "Today, ${timeFormat.format(Date(record.scheduledAt))}"
            } else if (isYesterday(record.scheduledAt)) {
                "Yesterday, ${timeFormat.format(Date(record.scheduledAt))}"
            } else {
                dateTimeFormat.format(Date(record.scheduledAt))
            }
            tvScheduledTime.text = "Scheduled: $scheduledDateStr"

            // Food instruction snapshot
            if (record.foodInstruction.isNotEmpty()) {
                tvFoodInstruction.visibility = View.VISIBLE
                tvFoodInstruction.text = when (record.foodInstruction) {
                    "BEFORE_FOOD" -> context.getString(R.string.food_before)
                    "WITH_FOOD" -> context.getString(R.string.food_with)
                    "AFTER_FOOD" -> context.getString(R.string.food_after)
                    else -> context.getString(R.string.food_no_restriction)
                }
            } else {
                tvFoodInstruction.visibility = View.GONE
            }

            // Status Badge
            when (record.status) {
                DoseRecord.STATUS_TAKEN -> {
                    tvStatusBadge.text = context.getString(R.string.status_taken)
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_taken)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_taken))
                }
                DoseRecord.STATUS_SKIPPED -> {
                    tvStatusBadge.text = context.getString(R.string.status_skipped)
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_skipped)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_skipped))
                }
                DoseRecord.STATUS_UNCONFIRMED -> {
                    tvStatusBadge.text = context.getString(R.string.status_unconfirmed)
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_unconfirmed)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_unconfirmed))
                }
                else -> {
                    val isUpcoming = record.scheduledAt > System.currentTimeMillis()
                    tvStatusBadge.text = if (isUpcoming) context.getString(R.string.status_upcoming) else context.getString(R.string.status_pending)
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
            }

            // Late Warning
            if (record.isLate && record.recordedAt != null) {
                tvLateBadge.visibility = View.VISIBLE
                val diffMinutes = (record.recordedAt - record.scheduledAt) / (60 * 1000)
                if (diffMinutes >= 60) {
                    val hours = diffMinutes / 60
                    val remMin = diffMinutes % 60
                    tvLateBadge.text = "${context.getString(R.string.late_submission)} (+${hours}h ${remMin}m)"
                } else {
                    tvLateBadge.text = "${context.getString(R.string.late_submission)} (+${diffMinutes}m)"
                }
            } else {
                tvLateBadge.visibility = View.GONE
            }

            // Recorded Time info
            if (record.recordedAt != null) {
                layoutRecorded.visibility = View.VISIBLE
                val recordedTime = timeFormat.format(Date(record.recordedAt))
                val schedTime = timeFormat.format(Date(record.scheduledAt))
                tvRecordedTime.text = "Submitted at $recordedTime (Sched: $schedTime)"
            } else {
                layoutRecorded.visibility = View.GONE
            }

            // Action row for Pending or Unconfirmed doses (strictly prevent marking future doses taken/skipped)
            val isUpcoming = record.scheduledAt > System.currentTimeMillis()
            if (!isUpcoming && (record.status == DoseRecord.STATUS_PENDING || record.status == DoseRecord.STATUS_UNCONFIRMED)) {
                layoutActions.visibility = View.VISIBLE
                btnTake.setOnClickListener { onTakenClicked(record) }
                btnSkip.setOnClickListener { onSkippedClicked(record) }
            } else {
                layoutActions.visibility = View.GONE
            }

            // Highlight border if this is the target dose
            if (record.id == highlightedDoseId) {
                cardEntry.strokeColor = ContextCompat.getColor(context, R.color.primary)
                cardEntry.strokeWidth = 2
                cardEntry.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_low))
            } else {
                cardEntry.strokeColor = ContextCompat.getColor(context, R.color.outline_variant)
                cardEntry.strokeWidth = 1
                cardEntry.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_container_lowest))
            }
        }

        private fun isToday(millis: Long): Boolean {
            val c1 = Calendar.getInstance()
            val c2 = Calendar.getInstance().apply { timeInMillis = millis }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(millis: Long): Boolean {
            val c1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val c2 = Calendar.getInstance().apply { timeInMillis = millis }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DoseRecord>() {
        override fun areItemsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean {
            return oldItem == newItem
        }
    }
}
