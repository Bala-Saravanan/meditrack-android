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

class UpcomingReminderAdapter(
    private val onTakenClicked: (DoseRecord) -> Unit,
    private val onSkippedClicked: (DoseRecord) -> Unit,
    private val onItemClicked: ((DoseRecord) -> Unit)? = null
) : ListAdapter<DoseRecord, UpcomingReminderAdapter.ReminderViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_upcoming_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
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

        fun bind(record: DoseRecord) {
            tvTime.text = timeFormat.format(Date(record.scheduledAt))
            
            val isUpcoming = record.scheduledAt > System.currentTimeMillis()
            tvStatus.text = if (isUpcoming) itemView.context.getString(R.string.status_upcoming) else itemView.context.getString(R.string.status_pending)

            tvScheduleName.text = "Schedule #${record.scheduleId}"
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

            btnTaken.setOnClickListener { onTakenClicked(record) }
            btnSkip.setOnClickListener { onSkippedClicked(record) }
            itemView.setOnClickListener { onItemClicked?.invoke(record) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DoseRecord>() {
        override fun areItemsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DoseRecord, newItem: DoseRecord): Boolean =
            oldItem == newItem
    }
}
