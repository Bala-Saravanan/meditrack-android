package com.example.meditrack.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import java.util.Calendar

object ReminderScheduler {

    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"
    const val EXTRA_SCHEDULE_MEDICINE_ID = "extra_schedule_medicine_id"
    const val EXTRA_SCHEDULE_MEDICINE_TIME_ID = "extra_schedule_medicine_time_id"
    const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"
    const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    const val EXTRA_PERIOD = "extra_period"
    const val EXTRA_FOOD_INSTRUCTION = "extra_food_instruction"

    fun calculateNextOccurrence(
        schedule: Schedule,
        time: ScheduleMedicineTime,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (schedule.status != Schedule.STATUS_ACTIVE) return null
        if (!schedule.notificationsEnabled) return null
        if (!time.isEnabled) return null

        val scheduleStartDay = getStartOfDay(schedule.startDate)
        val scheduleEndDay = schedule.endDate?.let { getEndOfDay(it) }

        val todayStart = getStartOfDay(nowMillis)
        val todaySlotTime = todayStart + (time.minutesSinceMidnight * 60 * 1000L)

        // Case 1: Today's slot is in the future and today is within schedule range
        if (todaySlotTime > nowMillis && todayStart >= scheduleStartDay && (scheduleEndDay == null || todayStart <= scheduleEndDay)) {
            return todaySlotTime
        }

        // Case 2: Today's slot has passed or today is before start date. Check upcoming days.
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If next calendar day is still before schedule start date, jump to start date
        if (cal.timeInMillis < scheduleStartDay) {
            cal.timeInMillis = scheduleStartDay
        }

        val candidateDayStart = cal.timeInMillis
        if (scheduleEndDay != null && candidateDayStart > scheduleEndDay) {
            return null // Schedule has ended
        }

        return candidateDayStart + (time.minutesSinceMidnight * 60 * 1000L)
    }

    fun scheduleReminderForSlot(
        context: Context,
        schedule: Schedule,
        scheduleMedicine: ScheduleMedicine,
        medicine: Medicine,
        time: ScheduleMedicineTime,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val nextTrigger = calculateNextOccurrence(schedule, time, nowMillis)
        if (nextTrigger == null || !scheduleMedicine.isActive) {
            cancelReminder(context, time.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_SCHEDULE_NAME, schedule.name)
            putExtra(EXTRA_SCHEDULE_MEDICINE_ID, scheduleMedicine.id)
            putExtra(EXTRA_SCHEDULE_MEDICINE_TIME_ID, time.id)
            putExtra(EXTRA_SCHEDULED_AT, nextTrigger)
            putExtra(EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(EXTRA_PERIOD, time.period)
            putExtra(EXTRA_FOOD_INSTRUCTION, scheduleMedicine.foodInstruction)
        }

        val requestCode = time.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set inexact alarm with doze support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, timeId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = timeId.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    suspend fun scheduleAllActiveReminders(context: Context, scheduleDao: ScheduleDao) {
        val activeFullSchedules = scheduleDao.getAllActiveFullSchedulesOnce()
        for (fullSchedule in activeFullSchedules) {
            val schedule = fullSchedule.schedule
            for (medWithTimes in fullSchedule.medicinesWithTimes) {
                val sm = medWithTimes.scheduleMedicine
                val med = medWithTimes.medicine
                for (time in medWithTimes.times) {
                    if (schedule.notificationsEnabled && sm.isActive && time.isEnabled) {
                        scheduleReminderForSlot(context, schedule, sm, med, time)
                    } else {
                        cancelReminder(context, time.id)
                    }
                }
            }
        }
    }

    private fun getStartOfDay(timeMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    private fun getEndOfDay(timeMillis: Long): Long {
        val c = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return c.timeInMillis
    }
}
