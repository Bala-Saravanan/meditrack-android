package com.example.meditrack.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.data.local.entity.DoseRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULE_ID, -1L)
        val scheduleMedicineId = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULE_MEDICINE_ID, -1L)
        val scheduleMedicineTimeId = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULE_MEDICINE_TIME_ID, -1L)
        val scheduledAt = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, -1L)
        val medicineName = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICINE_NAME) ?: "Medication"
        val period = intent.getStringExtra(ReminderScheduler.EXTRA_PERIOD) ?: "Morning"
        val foodInstruction = intent.getStringExtra(ReminderScheduler.EXTRA_FOOD_INSTRUCTION) ?: ""
        val scheduleName = intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULE_NAME) ?: "Schedule #$scheduleId"

        if (scheduleId <= 0 || scheduleMedicineTimeId <= 0 || scheduledAt <= 0) return

        val pendingResult = goAsync()
        val app = context.applicationContext as MediTrackApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure dose record exists in Room atomically
                val dose = app.doseRepository.getOrCreateDoseRecord(
                    scheduleId = scheduleId,
                    scheduleMedicineId = scheduleMedicineId,
                    scheduleMedicineTimeId = scheduleMedicineTimeId,
                    medicineName = medicineName,
                    period = period,
                    scheduledAt = scheduledAt,
                    foodInstruction = foodInstruction
                )

                // Only post notification if the dose is still pending or unconfirmed (not already taken/skipped)
                if (dose.status != DoseRecord.STATUS_TAKEN && dose.status != DoseRecord.STATUS_SKIPPED) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        NotificationHelper.showReminderNotification(context, dose, scheduleName)
                    }
                }

                // Reschedule the next eligible occurrence for this time slot (e.g. tomorrow)
                val fullSchedule = app.scheduleRepository.getFullScheduleDetailsOnce(scheduleId)
                if (fullSchedule != null) {
                    val smWithTimes = fullSchedule.medicinesWithTimes.find { it.scheduleMedicine.id == scheduleMedicineId }
                    val timeSlot = smWithTimes?.times?.find { it.id == scheduleMedicineTimeId }
                    if (smWithTimes != null && timeSlot != null) {
                        ReminderScheduler.scheduleReminderForSlot(
                            context = context,
                            schedule = fullSchedule.schedule,
                            scheduleMedicine = smWithTimes.scheduleMedicine,
                            medicine = smWithTimes.medicine,
                            time = timeSlot,
                            nowMillis = scheduledAt + 1000L
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
