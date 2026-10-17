package com.example.meditrack.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.repository.MedicineRepository
import com.example.meditrack.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class MedicineDraft(
    val medicineId: Long,
    val medicineName: String,
    val medicineForm: String,
    var foodInstruction: String = ScheduleMedicine.FOOD_BEFORE,
    var doseQuantity: String = "1 Tablet",
    var isMorningEnabled: Boolean = true,
    var morningMinutes: Int = 8 * 60, // 08:00 AM
    var isAfternoonEnabled: Boolean = false,
    var afternoonMinutes: Int = 13 * 60, // 01:00 PM
    var isEveningEnabled: Boolean = false,
    var eveningMinutes: Int = 20 * 60, // 08:00 PM
    var isNightEnabled: Boolean = false,
    var nightMinutes: Int = 22 * 60, // 10:00 PM
    var remindersEnabled: Boolean = true
)

class CreateScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val medicineRepository: MedicineRepository
) : ViewModel() {

    var scheduleId: Long = -1L
    var scheduleName: String = ""
    var startDate: Long = startOfTodayTimestamp()
    var endDate: Long? = null
    var physicianName: String = ""
    var notes: String = ""
    var status: String = Schedule.STATUS_ACTIVE
    var notificationsEnabled: Boolean = true

    private var isLoaded: Boolean = false

    private val _assignedMedicines = MutableStateFlow<List<MedicineDraft>>(emptyList())
    val assignedMedicines: StateFlow<List<MedicineDraft>> = _assignedMedicines.asStateFlow()

    val allMedicines: Flow<List<Medicine>> = medicineRepository.allMedicines

    suspend fun initialize(id: Long) {
        if (isLoaded) return
        scheduleId = id
        if (id > 0) {
            val details = scheduleRepository.getFullScheduleDetailsOnce(id)
            if (details != null) {
                val s = details.schedule
                scheduleName = s.name
                startDate = s.startDate
                endDate = s.endDate
                physicianName = s.physicianName
                notes = s.notes
                status = s.status
                notificationsEnabled = s.notificationsEnabled

                val drafts = details.medicinesWithTimes
                    .filter { it.scheduleMedicine.isActive }
                    .map { item ->
                        val sm = item.scheduleMedicine
                        val m = item.medicine
                        val times = item.times.filter { it.isEnabled }

                        val morning = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_MORNING, ignoreCase = true) }
                        val afternoon = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_AFTERNOON, ignoreCase = true) }
                        val evening = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_EVENING, ignoreCase = true) }
                        val night = times.find { it.period.equals(ScheduleMedicineTime.PERIOD_NIGHT, ignoreCase = true) }

                        MedicineDraft(
                            medicineId = m.id,
                            medicineName = m.name,
                            medicineForm = m.form,
                            foodInstruction = sm.foodInstruction,
                            doseQuantity = sm.doseQuantity,
                            isMorningEnabled = morning != null,
                            morningMinutes = morning?.minutesSinceMidnight ?: (8 * 60),
                            isAfternoonEnabled = afternoon != null,
                            afternoonMinutes = afternoon?.minutesSinceMidnight ?: (13 * 60),
                            isEveningEnabled = evening != null,
                            eveningMinutes = evening?.minutesSinceMidnight ?: (20 * 60),
                            isNightEnabled = night != null,
                            nightMinutes = night?.minutesSinceMidnight ?: (22 * 60),
                            remindersEnabled = s.notificationsEnabled
                        )
                    }
                _assignedMedicines.value = drafts
            }
        }
        isLoaded = true
    }

    fun addMedicine(medicine: Medicine): Boolean {
        val current = _assignedMedicines.value.toMutableList()
        if (current.any { it.medicineId == medicine.id }) {
            return false
        }
        val newDraft = MedicineDraft(
            medicineId = medicine.id,
            medicineName = medicine.name,
            medicineForm = medicine.form,
            foodInstruction = ScheduleMedicine.FOOD_BEFORE,
            isMorningEnabled = true,
            morningMinutes = 8 * 60
        )
        current.add(newDraft)
        _assignedMedicines.value = current
        return true
    }

    suspend fun addMedicineById(medicineId: Long): Boolean {
        val medicine = medicineRepository.getMedicineById(medicineId) ?: return false
        return addMedicine(medicine)
    }

    fun removeMedicine(medicineId: Long) {
        val current = _assignedMedicines.value.toMutableList()
        current.removeAll { it.medicineId == medicineId }
        _assignedMedicines.value = current
    }

    fun updateSlotTime(medicineId: Long, period: String, minutes: Int) {
        val current = _assignedMedicines.value
        val item = current.find { it.medicineId == medicineId } ?: return
        when (period) {
            ScheduleMedicineTime.PERIOD_MORNING -> item.morningMinutes = minutes
            ScheduleMedicineTime.PERIOD_AFTERNOON -> item.afternoonMinutes = minutes
            ScheduleMedicineTime.PERIOD_EVENING -> item.eveningMinutes = minutes
            ScheduleMedicineTime.PERIOD_NIGHT -> item.nightMinutes = minutes
        }
        _assignedMedicines.value = current.toList()
    }

    suspend fun validateAndSave(): Result<Long> {
        val trimmedName = scheduleName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a schedule name"))
        }

        if (endDate != null && endDate!! < startDate) {
            return Result.failure(IllegalArgumentException("End date cannot be earlier than start date"))
        }

        val meds = _assignedMedicines.value
        if (meds.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please assign at least one medicine"))
        }

        for (med in meds) {
            val hasSlot = med.isMorningEnabled || med.isAfternoonEnabled || med.isEveningEnabled || med.isNightEnabled
            if (!hasSlot) {
                return Result.failure(IllegalArgumentException("Please enable at least one time slot for ${med.medicineName}"))
            }
        }

        val scheduleToSave = Schedule(
            id = if (scheduleId > 0) scheduleId else 0,
            name = trimmedName,
            startDate = startDate,
            endDate = endDate,
            physicianName = physicianName.trim(),
            notes = notes.trim(),
            status = status,
            notificationsEnabled = notificationsEnabled
        )

        val medicinesWithTimes = meds.map { draft ->
            val scheduleMed = ScheduleMedicine(
                scheduleId = if (scheduleId > 0) scheduleId else 0,
                medicineId = draft.medicineId,
                foodInstruction = draft.foodInstruction,
                doseQuantity = draft.doseQuantity,
                isActive = true
            )

            val times = mutableListOf<ScheduleMedicineTime>()
            if (draft.isMorningEnabled) {
                times.add(
                    ScheduleMedicineTime(
                        scheduleMedicineId = 0,
                        period = ScheduleMedicineTime.PERIOD_MORNING,
                        minutesSinceMidnight = draft.morningMinutes,
                        isEnabled = true
                    )
                )
            }
            if (draft.isAfternoonEnabled) {
                times.add(
                    ScheduleMedicineTime(
                        scheduleMedicineId = 0,
                        period = ScheduleMedicineTime.PERIOD_AFTERNOON,
                        minutesSinceMidnight = draft.afternoonMinutes,
                        isEnabled = true
                    )
                )
            }
            if (draft.isEveningEnabled) {
                times.add(
                    ScheduleMedicineTime(
                        scheduleMedicineId = 0,
                        period = ScheduleMedicineTime.PERIOD_EVENING,
                        minutesSinceMidnight = draft.eveningMinutes,
                        isEnabled = true
                    )
                )
            }
            if (draft.isNightEnabled) {
                times.add(
                    ScheduleMedicineTime(
                        scheduleMedicineId = 0,
                        period = ScheduleMedicineTime.PERIOD_NIGHT,
                        minutesSinceMidnight = draft.nightMinutes,
                        isEnabled = true
                    )
                )
            }
            Pair(scheduleMed, times)
        }

        return try {
            if (scheduleId > 0) {
                scheduleRepository.updateScheduleWithMedicines(scheduleToSave, medicinesWithTimes)
                Result.success(scheduleId)
            } else {
                val newId = scheduleRepository.createScheduleWithMedicines(scheduleToSave, medicinesWithTimes)
                Result.success(newId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun startOfTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

class CreateScheduleViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
    private val medicineRepository: MedicineRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateScheduleViewModel(scheduleRepository, medicineRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
