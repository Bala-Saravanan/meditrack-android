package com.example.meditrack.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.DoseRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DashboardReminderItem {
    data class ScheduleHeader(
        val scheduleId: Long,
        val scheduleName: String,
        val count: Int
    ) : DashboardReminderItem()

    data class ReminderEntry(
        val dose: DoseRecord,
        val scheduleName: String
    ) : DashboardReminderItem()
}

class UpcomingReminderAdapter(
    private val onTakenClicked: (DoseRecord) -> Unit,
    private val onSkippedClicked: (DoseRecord) -> Unit,
    private val onItemClicked: ((DoseRecord) -> Unit)? = null
) : ListAdapter<DashboardReminderItem, RecyclerView.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_REMINDER = 1

        object DiffCallback : DiffUtil.ItemCallback<DashboardReminderItem>() {
            override fun areItemsTheSame(
                oldItem: DashboardReminderItem,
                newItem: DashboardReminderItem
            ): Boolean {
                return when {
                    oldItem is DashboardReminderItem.ScheduleHeader && newItem is DashboardReminderItem.ScheduleHeader ->
                        oldItem.scheduleId == newItem.scheduleId
                    oldItem is DashboardReminderItem.ReminderEntry && newItem is DashboardReminderItem.ReminderEntry ->
                        oldItem.dose.id == newItem.dose.id
                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: DashboardReminderItem,
                newItem: DashboardReminderItem
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DashboardReminderItem.ScheduleHeader -> TYPE_HEADER
            is DashboardReminderItem.ReminderEntry -> TYPE_REMINDER
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
                val view = inflater.inflate(R.layout.item_upcoming_reminder, parent, false)
                ReminderViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DashboardReminderItem.ScheduleHeader -> (holder as HeaderViewHolder).bind(item)
            is DashboardReminderItem.ReminderEntry -> (holder as ReminderViewHolder).bind(item)
        }
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_schedule_group_title)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_schedule_group_count)

        fun bind(header: DashboardReminderItem.ScheduleHeader) {
            tvTitle.text = header.scheduleName
            tvCount.text = "${header.count} ${if (header.count == 1) "Reminder" else "Reminders"}"
        }
    }

    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_reminder_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_reminder_status)
        private val tvScheduleName: TextView = itemView.findViewById(R.id.tv_schedule_name)
        private val tvMedicineName: TextView = itemView.findViewById(R.id.tv_medicine_name)
        private val tvPeriod: TextView = itemView.findViewById(R.id.tv_dose_period)
        private val tvFoodInstruction: TextView = itemView.findViewById(R.id.tv_food_instruction)
        private val layoutFood: View = itemView.findViewById(R.id.layout_food_instruction)
        private val btnSkip: Button = itemView.findViewById(R.id.btn_action_skip)
        private val btnTaken: Button = itemView.findViewById(R.id.btn_action_taken)

        fun bind(item: DashboardReminderItem.ReminderEntry) {
            val record = item.dose
            val now = System.currentTimeMillis()
            val isUpcoming = record.scheduledAt > now

            tvTime.text = timeFormat.format(Date(record.scheduledAt)).uppercase()

            if (isUpcoming) {
                tvStatus.text = "⏱ " + itemView.context.getString(R.string.status_upcoming)
                tvStatus.setBackgroundResource(R.drawable.bg_pill_reminder_upcoming)
            } else {
                tvStatus.text = "• " + itemView.context.getString(R.string.status_pending)
                tvStatus.setBackgroundResource(R.drawable.bg_pill_reminder_pending)
            }

            tvScheduleName.text = item.scheduleName
            tvMedicineName.text = record.medicineName
            tvPeriod.text = record.period

            if (record.foodInstruction.isNotEmpty()) {
                layoutFood.visibility = View.VISIBLE
                tvFoodInstruction.text = when (record.foodInstruction) {
                    "BEFORE_FOOD" -> "Take before meal"
                    "WITH_FOOD" -> "Take with meal"
                    "AFTER_FOOD" -> "Take after meal"
                    "NO_RESTRICTION" -> "No dietary restriction"
                    else -> record.foodInstruction
                }
            } else {
                layoutFood.visibility = View.GONE
            }

            // Guard against marking future doses taken/skipped:
            if (isUpcoming) {
                btnTaken.visibility = View.GONE
                btnSkip.visibility = View.GONE
            } else {
                btnTaken.visibility = View.VISIBLE
                btnSkip.visibility = View.VISIBLE
                btnTaken.setOnClickListener { onTakenClicked(record) }
                btnSkip.setOnClickListener { onSkippedClicked(record) }
            }

            itemView.setOnClickListener { onItemClicked?.invoke(record) }
        }
    }
}
