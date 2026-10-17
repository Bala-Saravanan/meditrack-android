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
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val doseRepository: DoseRepository,
    private val database: MediTrackDatabase
) : ViewModel() {

    val userProfile: Flow<UserProfile?> = userRepository.userProfile

    val pendingReminders: Flow<List<DoseRecord>> = doseRepository.getPendingRemindersForToday()

    val recentSummaries: Flow<List<ScheduleAdherenceSummary>> = doseRepository.getRecentDoseSummary(daysBack = 4)

    init {
        // Evaluate expired pending doses (>2h) as unconfirmed on launch
        viewModelScope.launch {
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
