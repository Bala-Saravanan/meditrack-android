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

        fun bind(item: FullScheduleDetails) {
            val schedule = item.schedule
            tvName.text = schedule.name

            // Status styling
            tvStatus.text = schedule.status
            when (schedule.status) {
                Schedule.STATUS_ACTIVE -> {
                    tvStatus.setBackgroundResource(R.drawable.ic_launcher_background)
                    tvStatus.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.tertiary_fixed)
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.tertiary))
                }
                Schedule.STATUS_COMPLETED -> {
                    tvStatus.setBackgroundResource(R.drawable.ic_launcher_background)
                    tvStatus.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.primary_fixed)
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                }
                else -> {
                    tvStatus.setBackgroundResource(R.drawable.ic_launcher_background)
                    tvStatus.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.surface_container_high)
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface_variant))
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
            } else {
                val names = activeMeds.map { it.medicine.name }.joinToString(", ")
                val count = activeMeds.size
                tvSummary.text = "$count ${if (count == 1) "Medicine" else "Medicines"} ($names)"
            }

            // Notes
            if (schedule.notes.isNotBlank()) {
                tvNotes.text = schedule.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            // Physician
            if (schedule.physicianName.isNotBlank()) {
                tvPhysician.text = "Physician: ${schedule.physicianName}"
                tvPhysician.visibility = View.VISIBLE
            } else {
                tvPhysician.visibility = View.GONE
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
