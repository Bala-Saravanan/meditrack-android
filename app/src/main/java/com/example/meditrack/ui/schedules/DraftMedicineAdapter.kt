package com.example.meditrack.ui.schedules

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch

class DraftMedicineAdapter(
    private val onRemoveClicked: (Long) -> Unit,
    private val onTimePickerRequested: (MedicineDraft, String) -> Unit
) : RecyclerView.Adapter<DraftMedicineAdapter.DraftViewHolder>() {

    private val items = mutableListOf<MedicineDraft>()

    fun submitList(newList: List<MedicineDraft>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft_medicine, parent, false)
        return DraftViewHolder(view)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_draft_med_name)
        private val tvForm: TextView = itemView.findViewById(R.id.tv_draft_med_form)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btn_draft_remove_med)

        private val chipGroupFood: ChipGroup = itemView.findViewById(R.id.cg_food_instruction)
        private val chipBefore: Chip = itemView.findViewById(R.id.chip_food_before)
        private val chipWith: Chip = itemView.findViewById(R.id.chip_food_with)
        private val chipAfter: Chip = itemView.findViewById(R.id.chip_food_after)
        private val chipNone: Chip = itemView.findViewById(R.id.chip_food_none)

        private val cbMorning: MaterialCheckBox = itemView.findViewById(R.id.cb_slot_morning)
        private val btnTimeMorning: MaterialButton = itemView.findViewById(R.id.btn_time_morning)

        private val cbAfternoon: MaterialCheckBox = itemView.findViewById(R.id.cb_slot_afternoon)
        private val btnTimeAfternoon: MaterialButton = itemView.findViewById(R.id.btn_time_afternoon)

        private val cbEvening: MaterialCheckBox = itemView.findViewById(R.id.cb_slot_evening)
        private val btnTimeEvening: MaterialButton = itemView.findViewById(R.id.btn_time_evening)

        private val cbNight: MaterialCheckBox = itemView.findViewById(R.id.cb_slot_night)
        private val btnTimeNight: MaterialButton = itemView.findViewById(R.id.btn_time_night)

        private val switchReminders: MaterialSwitch = itemView.findViewById(R.id.switch_reminders)

        fun bind(draft: MedicineDraft) {
            tvName.text = draft.medicineName
            tvForm.text = draft.medicineForm

            btnRemove.setOnClickListener {
                onRemoveClicked(draft.medicineId)
            }

            // Food Instruction selection
            chipGroupFood.setOnCheckedStateChangeListener(null)
            when (draft.foodInstruction) {
                ScheduleMedicine.FOOD_BEFORE -> chipBefore.isChecked = true
                ScheduleMedicine.FOOD_WITH -> chipWith.isChecked = true
                ScheduleMedicine.FOOD_AFTER -> chipAfter.isChecked = true
                else -> chipNone.isChecked = true
            }

            chipGroupFood.setOnCheckedStateChangeListener { _, checkedIds ->
                draft.foodInstruction = when (checkedIds.firstOrNull()) {
                    R.id.chip_food_before -> ScheduleMedicine.FOOD_BEFORE
                    R.id.chip_food_with -> ScheduleMedicine.FOOD_WITH
                    R.id.chip_food_after -> ScheduleMedicine.FOOD_AFTER
                    else -> ScheduleMedicine.FOOD_NO_RESTRICTION
                }
            }

            // Morning
            cbMorning.setOnCheckedChangeListener(null)
            cbMorning.isChecked = draft.isMorningEnabled
            btnTimeMorning.text = ScheduleMedicineTime.formatTime(draft.morningMinutes)
            btnTimeMorning.isEnabled = draft.isMorningEnabled
            cbMorning.setOnCheckedChangeListener { _, isChecked ->
                draft.isMorningEnabled = isChecked
                btnTimeMorning.isEnabled = isChecked
            }
            btnTimeMorning.setOnClickListener {
                onTimePickerRequested(draft, ScheduleMedicineTime.PERIOD_MORNING)
            }

            // Afternoon
            cbAfternoon.setOnCheckedChangeListener(null)
            cbAfternoon.isChecked = draft.isAfternoonEnabled
            btnTimeAfternoon.text = ScheduleMedicineTime.formatTime(draft.afternoonMinutes)
            btnTimeAfternoon.isEnabled = draft.isAfternoonEnabled
            cbAfternoon.setOnCheckedChangeListener { _, isChecked ->
                draft.isAfternoonEnabled = isChecked
                btnTimeAfternoon.isEnabled = isChecked
            }
            btnTimeAfternoon.setOnClickListener {
                onTimePickerRequested(draft, ScheduleMedicineTime.PERIOD_AFTERNOON)
            }

            // Evening
            cbEvening.setOnCheckedChangeListener(null)
            cbEvening.isChecked = draft.isEveningEnabled
            btnTimeEvening.text = ScheduleMedicineTime.formatTime(draft.eveningMinutes)
            btnTimeEvening.isEnabled = draft.isEveningEnabled
            cbEvening.setOnCheckedChangeListener { _, isChecked ->
                draft.isEveningEnabled = isChecked
                btnTimeEvening.isEnabled = isChecked
            }
            btnTimeEvening.setOnClickListener {
                onTimePickerRequested(draft, ScheduleMedicineTime.PERIOD_EVENING)
            }

            // Night
            cbNight.setOnCheckedChangeListener(null)
            cbNight.isChecked = draft.isNightEnabled
            btnTimeNight.text = ScheduleMedicineTime.formatTime(draft.nightMinutes)
            btnTimeNight.isEnabled = draft.isNightEnabled
            cbNight.setOnCheckedChangeListener { _, isChecked ->
                draft.isNightEnabled = isChecked
                btnTimeNight.isEnabled = isChecked
            }
            btnTimeNight.setOnClickListener {
                onTimePickerRequested(draft, ScheduleMedicineTime.PERIOD_NIGHT)
            }

            // Reminders
            switchReminders.setOnCheckedChangeListener(null)
            switchReminders.isChecked = draft.remindersEnabled
            switchReminders.setOnCheckedChangeListener { _, isChecked ->
                draft.remindersEnabled = isChecked
            }
        }
    }
}
