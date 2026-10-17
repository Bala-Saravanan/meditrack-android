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
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.ScheduleMedicineWithTimes

class ScheduleDetailsAdapter :
    ListAdapter<ScheduleMedicineWithTimes, ScheduleDetailsAdapter.DetailViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_medicine_detail, parent, false)
        return DetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_detail_med_name)
        private val tvForm: TextView = itemView.findViewById(R.id.tv_detail_med_form)
        private val tvFood: TextView = itemView.findViewById(R.id.tv_detail_food_instruction)
        private val tvInstructions: TextView = itemView.findViewById(R.id.tv_detail_instructions)

        private val layoutMorning: View = itemView.findViewById(R.id.layout_slot_morning)
        private val tvMorning: TextView = itemView.findViewById(R.id.tv_slot_morning_time)
        private val layoutAfternoon: View = itemView.findViewById(R.id.layout_slot_afternoon)
        private val tvAfternoon: TextView = itemView.findViewById(R.id.tv_slot_afternoon_time)
        private val layoutEvening: View = itemView.findViewById(R.id.layout_slot_evening)
        private val tvEvening: TextView = itemView.findViewById(R.id.tv_slot_evening_time)
        private val layoutNight: View = itemView.findViewById(R.id.layout_slot_night)
        private val tvNight: TextView = itemView.findViewById(R.id.tv_slot_night_time)

        fun bind(item: ScheduleMedicineWithTimes) {
            val med = item.medicine
            val scheduleMed = item.scheduleMedicine
            tvName.text = med.name
            tvForm.text = "${med.form} • ${scheduleMed.doseQuantity}"

            // Food instruction
            val foodDisplayName = when (scheduleMed.foodInstruction) {
                ScheduleMedicine.FOOD_BEFORE, "Before Food" -> itemView.context.getString(R.string.food_before)
                ScheduleMedicine.FOOD_WITH, "With Food" -> itemView.context.getString(R.string.food_with)
                ScheduleMedicine.FOOD_AFTER, "After Food" -> itemView.context.getString(R.string.food_after)
                else -> itemView.context.getString(R.string.food_no_restriction)
            }
            tvFood.text = foodDisplayName

            // Instructions
            if (med.instructions.isNotBlank()) {
                tvInstructions.text = med.instructions
                tvInstructions.visibility = View.VISIBLE
            } else {
                tvInstructions.visibility = View.GONE
            }

            // Time slots
            val times = item.times.filter { it.isEnabled }
            val morning = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_MORNING, ignoreCase = true) }
            val afternoon = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_AFTERNOON, ignoreCase = true) }
            val evening = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_EVENING, ignoreCase = true) }
            val night = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_NIGHT, ignoreCase = true) }

            bindSlot(layoutMorning, tvMorning, morning)
            bindSlot(layoutAfternoon, tvAfternoon, afternoon)
            bindSlot(layoutEvening, tvEvening, evening)
            bindSlot(layoutNight, tvNight, night)
        }

        private fun bindSlot(layout: View, textView: TextView, time: ScheduleMedicineTime?) {
            if (time != null && time.isEnabled) {
                textView.text = ScheduleMedicineTime.formatTime(time.minutesSinceMidnight)
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface))
                layout.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.tertiary_fixed)
            } else {
                textView.text = "—"
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface_variant))
                layout.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.surface_container_low)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ScheduleMedicineWithTimes>() {
            override fun areItemsTheSame(oldItem: ScheduleMedicineWithTimes, newItem: ScheduleMedicineWithTimes): Boolean {
                return oldItem.scheduleMedicine.id == newItem.scheduleMedicine.id
            }

            override fun areContentsTheSame(oldItem: ScheduleMedicineWithTimes, newItem: ScheduleMedicineWithTimes): Boolean {
                return oldItem == newItem
            }
        }
    }
}
