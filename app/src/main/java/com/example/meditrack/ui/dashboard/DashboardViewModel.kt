package com.example.meditrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.DatabaseInitializer
import com.example.meditrack.data.local.MediTrackDatabase
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.UserProfile
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val doseRepository: DoseRepository,
    private val database: MediTrackDatabase
) : ViewModel() {

    val userProfile: Flow<UserProfile?> = userRepository.userProfile

    val pendingReminders: Flow<List<DoseRecord>> = doseRepository.getPendingRemindersForToday()

    val groupedReminders: Flow<List<DashboardReminderItem>> = doseRepository.getPendingRemindersForToday().map { reminders ->
        val scheduleIds = reminders.map { it.scheduleId }.distinct()
        val scheduleMap = scheduleIds.associateWith { id ->
            doseRepository.getScheduleName(id)
        }
        val sortedScheduleIds = scheduleIds.sortedWith(
            compareBy({ scheduleMap[it]?.lowercase() ?: "" }, { it })
        )
        val result = mutableListOf<DashboardReminderItem>()
        for (schedId in sortedScheduleIds) {
            val schedName = scheduleMap[schedId] ?: "Schedule #$schedId"
            val schedReminders = reminders.filter { it.scheduleId == schedId }
                .sortedWith(compareBy({ it.scheduledAt }, { it.id }))
            result.add(DashboardReminderItem.ScheduleHeader(schedId, schedName, schedReminders.size))
            for (record in schedReminders) {
                result.add(DashboardReminderItem.ReminderEntry(record, schedName))
            }
        }
        result
    }

    val totalPendingCount: Flow<Int> = doseRepository.getPendingRemindersForToday().map { it.size }

    val recentSummaries: Flow<List<ScheduleAdherenceSummary>> = doseRepository.getRecentDoseSummary(daysBack = 4)

    init {
        // Safe one-time repair of any erroneous future doses & evaluate expired pending
        viewModelScope.launch {
            doseRepository.reconcileFutureDoses()
            doseRepository.markExpiredPendingAsUnconfirmed()
            // Also generate today's doses for active schedules if any exist
            doseRepository.generateDailyDosesForDate(System.currentTimeMillis())
        }
    }

    fun markDoseTaken(doseId: Long) {
        viewModelScope.launch {
            doseRepository.recordDoseTaken(doseId)
        }
    }

    fun markDoseSkipped(doseId: Long) {
        viewModelScope.launch {
            doseRepository.recordDoseSkipped(doseId)
        }
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            DatabaseInitializer.clearAllData(database)
            onComplete()
        }
    }
}

class DashboardViewModelFactory(
    private val userRepository: UserRepository,
    private val doseRepository: DoseRepository,
    private val database: MediTrackDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(userRepository, doseRepository, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
