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
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DoseListItem {
    data class ScheduleHeader(
        val scheduleId: Long,
        val scheduleName: String,
        val count: Int
    ) : DoseListItem()

    data class DoseEntry(
        val dose: DoseRecord,
        val scheduleName: String
    ) : DoseListItem()
}

class DoseAdapter(
    private val onItemClicked: (DoseRecord) -> Unit,
    private val onTakenClicked: (DoseRecord) -> Unit,
    private val onSkippedClicked: (DoseRecord) -> Unit
) : ListAdapter<DoseListItem, RecyclerView.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_DOSE = 1

        object DiffCallback : DiffUtil.ItemCallback<DoseListItem>() {
            override fun areItemsTheSame(oldItem: DoseListItem, newItem: DoseListItem): Boolean {
                return when {
                    oldItem is DoseListItem.ScheduleHeader && newItem is DoseListItem.ScheduleHeader ->
                        oldItem.scheduleId == newItem.scheduleId
                    oldItem is DoseListItem.DoseEntry && newItem is DoseListItem.DoseEntry ->
                        oldItem.dose.id == newItem.dose.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: DoseListItem, newItem: DoseListItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DoseListItem.ScheduleHeader -> TYPE_HEADER
            is DoseListItem.DoseEntry -> TYPE_DOSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = inflater.inflate(R.layout.item_schedule_group_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_dose_card, parent, false)
                DoseViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DoseListItem.ScheduleHeader -> (holder as HeaderViewHolder).bind(item)
            is DoseListItem.DoseEntry -> (holder as DoseViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_schedule_group_title)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_schedule_group_count)

        fun bind(header: DoseListItem.ScheduleHeader) {
            tvTitle.text = header.scheduleName
            tvCount.text = "${header.count} ${if (header.count == 1) "Dose" else "Doses"}"
        }
    }

    inner class DoseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_dose_status_badge)
        private val tvMedicineName: TextView = itemView.findViewById(R.id.tv_dose_medicine_name)
        private val tvScheduledTime: TextView = itemView.findViewById(R.id.tv_dose_scheduled_time)
        private val tvFoodInstruction: TextView = itemView.findViewById(R.id.tv_dose_food_instruction)
        private val tvLateBadge: TextView = itemView.findViewById(R.id.tv_dose_late_badge)
        private val tvRecordedInfo: TextView = itemView.findViewById(R.id.tv_dose_recorded_info)
        private val layoutActions: View = itemView.findViewById(R.id.layout_dose_actions)
        private val btnTake: MaterialButton = itemView.findViewById(R.id.btn_dose_take)
        private val btnSkip: MaterialButton = itemView.findViewById(R.id.btn_dose_skip)

        fun bind(item: DoseListItem.DoseEntry) {
            val dose = item.dose
            val context = itemView.context
            val now = System.currentTimeMillis()
            val isUpcoming = dose.scheduledAt > now

            tvMedicineName.text = dose.medicineName

            val formattedTime = timeFormat.format(Date(dose.scheduledAt))
            val periodLabel = dose.period.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            tvScheduledTime.text = "$formattedTime • $periodLabel"

            if (dose.foodInstruction.isNotEmpty()) {
                tvFoodInstruction.visibility = View.VISIBLE
                tvFoodInstruction.text = when (dose.foodInstruction) {
                    "BEFORE_FOOD" -> context.getString(R.string.food_before)
                    "WITH_FOOD" -> context.getString(R.string.food_with)
                    "AFTER_FOOD" -> context.getString(R.string.food_after)
                    else -> context.getString(R.string.food_no_restriction)
                }
            } else {
                tvFoodInstruction.visibility = View.GONE
            }

            // Status Badge styling
            when (dose.status) {
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
                    tvStatusBadge.text = if (isUpcoming) context.getString(R.string.status_upcoming) else context.getString(R.string.status_pending)
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pill_pending)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
            }

            // Late Warning Badge (only valid if dose is completed/recorded and actually late)
            if (dose.isLate && dose.recordedAt != null && !isUpcoming) {
                tvLateBadge.visibility = View.VISIBLE
                val diffMinutes = (dose.recordedAt - dose.scheduledAt) / (60 * 1000)
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

            // Recorded info timestamp (only for completed doses with valid recorded timestamp)
            if (dose.recordedAt != null && dose.recordedAt > 0 && !isUpcoming) {
                tvRecordedInfo.visibility = View.VISIBLE
                val recordedTime = timeFormat.format(Date(dose.recordedAt))
                tvRecordedInfo.text = "Submitted at $recordedTime (User-recorded)"
            } else {
                tvRecordedInfo.visibility = View.GONE
            }

            // Action row: STRICTLY PREVENT marking future doses as taken or skipped!
            // Only allow taking/skipping when dose is past or due (scheduledAt <= now) and status is PENDING or UNCONFIRMED
            if (!isUpcoming && (dose.status == DoseRecord.STATUS_PENDING || dose.status == DoseRecord.STATUS_UNCONFIRMED)) {
                layoutActions.visibility = View.VISIBLE
                btnTake.setOnClickListener { onTakenClicked(dose) }
                btnSkip.setOnClickListener { onSkippedClicked(dose) }
            } else {
                layoutActions.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClicked(dose)
            }
        }
    }
}
