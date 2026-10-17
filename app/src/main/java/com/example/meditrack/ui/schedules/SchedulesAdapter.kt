package com.example.meditrack.ui.schedules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.model.FullScheduleDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchedulesAdapter(
    private val onScheduleClicked: (Long) -> Unit
) : ListAdapter<FullScheduleDetails, SchedulesAdapter.ScheduleViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_card, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_schedule_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_schedule_status)
        private val tvDates: TextView = itemView.findViewById(R.id.tv_schedule_dates)
        private val tvSummary: TextView = itemView.findViewById(R.id.tv_medicines_summary)
        private val tvNotes: TextView = itemView.findViewById(R.id.tv_schedule_notes)
        private val tvPhysician: TextView = itemView.findViewById(R.id.tv_physician_note)

        private val ivIcon: android.widget.ImageView = itemView.findViewById(R.id.iv_schedule_icon)
        private val tvFrequency: TextView? = itemView.findViewById(R.id.tv_schedule_frequency)

        fun bind(item: FullScheduleDetails) {
            val schedule = item.schedule
            tvName.text = schedule.name

            // Status styling
            when (schedule.status) {
                Schedule.STATUS_ACTIVE -> {
                    tvStatus.text = "✔ Active"
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_status_active)
                    tvStatus.backgroundTintList = null
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.tertiary))
                }
                Schedule.STATUS_COMPLETED -> {
                    tvStatus.text = "✔ Completed"
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_status_completed)
                    tvStatus.backgroundTintList = null
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.secondary))
                }
                else -> {
                    tvStatus.text = schedule.status
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_status_default)
                    tvStatus.backgroundTintList = null
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface_variant))
                }
            }

            // Schedule icon based on name
            val lowerName = schedule.name.lowercase(Locale.ROOT)
            when {
                lowerName.contains("heart") || lowerName.contains("cardio") || lowerName.contains("bp") -> {
                    ivIcon.setImageResource(R.drawable.ic_favorite)
                }
                lowerName.contains("ulcer") || lowerName.contains("stomach") || lowerName.contains("diabetes") -> {
                    ivIcon.setImageResource(R.drawable.ic_medication)
                }
                else -> {
                    ivIcon.setImageResource(R.drawable.ic_calendar)
                }
            }

            // Dates
            val startStr = dateFormat.format(Date(schedule.startDate))
            val endStr = if (schedule.endDate != null && schedule.endDate > 0) {
                dateFormat.format(Date(schedule.endDate))
            } else {
                itemView.context.getString(R.string.ongoing)
            }
            tvDates.text = "$startStr – $endStr"

            // Medicines
            val activeMeds = item.medicinesWithTimes.filter { it.scheduleMedicine.isActive }
            if (activeMeds.isEmpty()) {
                tvSummary.text = "No medicines configured"
                tvFrequency?.visibility = View.GONE
            } else {
                val names = activeMeds.map { it.medicine.name }.joinToString(", ")
                val count = activeMeds.size
                tvSummary.text = "$count ${if (count == 1) "Medicine" else "Medicines"} ($names)"
                
                // Frequency
                val totalTimes = activeMeds.sumOf { it.times.size }
                tvFrequency?.visibility = View.VISIBLE
                tvFrequency?.text = when {
                    totalTimes >= 3 -> "3x Daily"
                    totalTimes == 2 -> "Twice Daily"
                    totalTimes == 1 -> "Daily"
                    else -> "Scheduled"
                }
            }

            // Notes
            if (schedule.notes.isNotBlank()) {
                tvNotes.text = schedule.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            // Physician / Status Note
            if (schedule.physicianName.isNotBlank()) {
                tvPhysician.text = if (schedule.physicianName.startsWith("Dr.", ignoreCase = true)) {
                    schedule.physicianName
                } else {
                    "Dr. ${schedule.physicianName}"
                }
                tvPhysician.visibility = View.VISIBLE
            } else {
                tvPhysician.text = if (schedule.status == Schedule.STATUS_ACTIVE) "Active regimen" else "Completed regimen"
                tvPhysician.visibility = View.VISIBLE
            }

            itemView.setOnClickListener {
                onScheduleClicked(schedule.id)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<FullScheduleDetails>() {
            override fun areItemsTheSame(oldItem: FullScheduleDetails, newItem: FullScheduleDetails): Boolean {
                return oldItem.schedule.id == newItem.schedule.id
            }

            override fun areContentsTheSame(oldItem: FullScheduleDetails, newItem: FullScheduleDetails): Boolean {
                return oldItem == newItem
            }
        }
    }
}
