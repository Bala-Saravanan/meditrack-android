package com.example.meditrack

import android.app.Application
import com.example.meditrack.data.local.MediTrackDatabase
import com.example.meditrack.data.repository.CaregiverRepository
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.data.repository.MedicineRepository
import com.example.meditrack.data.repository.ScheduleRepository
import com.example.meditrack.data.repository.UserRepository
import com.example.meditrack.reminder.NotificationHelper
import com.example.meditrack.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediTrackApplication : Application() {

    val database: MediTrackDatabase by lazy { MediTrackDatabase.getDatabase(this) }
    val userRepository: UserRepository by lazy { UserRepository(database.userDao()) }
    val medicineRepository: MedicineRepository by lazy { MedicineRepository(database.medicineDao()) }
    val scheduleRepository: ScheduleRepository by lazy { ScheduleRepository(database.scheduleDao(), this) }
    val doseRepository: DoseRepository by lazy { DoseRepository(database.doseRecordDao(), database.scheduleDao()) }
    val caregiverRepository: CaregiverRepository by lazy { CaregiverRepository(database.caregiverDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels
        NotificationHelper.createNotificationChannels(this)

        // Initialize database and schedule active reminders in background
        CoroutineScope(Dispatchers.IO).launch {
            ReminderScheduler.scheduleAllActiveReminders(this@MediTrackApplication, database.scheduleDao())
        }
    }
}
