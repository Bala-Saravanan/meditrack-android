package com.example.meditrack.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.model.FullScheduleDetails
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SchedulesViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val doseRepository: DoseRepository? = null
) : ViewModel() {

    val selectedFilter = MutableStateFlow("ALL")

    val allSchedules: Flow<List<FullScheduleDetails>> = scheduleRepository.allFullSchedules

    val activeCount: Flow<Int> = allSchedules.map { list ->
        list.count { it.schedule.status == Schedule.STATUS_ACTIVE }
    }

    val completedCount: Flow<Int> = allSchedules.map { list ->
        list.count { it.schedule.status == Schedule.STATUS_COMPLETED }
    }

    val discontinuedCount: Flow<Int> = allSchedules.map { list ->
        list.count { it.schedule.status == Schedule.STATUS_DISCONTINUED }
    }

    val totalCount: Flow<Int> = allSchedules.map { it.size }

    val dailyAdherencePercent: Flow<Int> = if (doseRepository != null) {
        doseRepository.getTodayDoses().map { doses ->
            val now = System.currentTimeMillis()
            val eligible = doses.count {
                it.scheduledAt <= now && (it.status == DoseRecord.STATUS_TAKEN ||
                        it.status == DoseRecord.STATUS_SKIPPED ||
                        it.status == DoseRecord.STATUS_UNCONFIRMED)
            }
            val taken = doses.count { it.scheduledAt <= now && it.status == DoseRecord.STATUS_TAKEN }
            if (eligible > 0) ((taken * 100) / eligible).coerceIn(0, 100) else 100
        }
    } else {
        flowOf(100)
    }

    val filteredSchedules: Flow<List<FullScheduleDetails>> = combine(allSchedules, selectedFilter) { schedules, filter ->
        when (filter) {
            "ALL" -> schedules
            else -> schedules.filter { it.schedule.status.equals(filter, ignoreCase = true) }
        }
    }

    fun setFilter(filter: String) {
        selectedFilter.value = filter
    }
}

class SchedulesViewModelFactory(
    private val scheduleRepository: ScheduleRepository,
    private val doseRepository: DoseRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchedulesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchedulesViewModel(scheduleRepository, doseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
