package com.example.meditrack.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.model.FullScheduleDetails
import com.example.meditrack.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SchedulesViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val selectedFilter = MutableStateFlow("ALL")

    val allSchedules: Flow<List<FullScheduleDetails>> = scheduleRepository.allFullSchedules

    val activeCount: Flow<Int> = allSchedules.map { list ->
        list.count { it.schedule.status == Schedule.STATUS_ACTIVE }
    }

    val totalCount: Flow<Int> = allSchedules.map { it.size }

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
    private val scheduleRepository: ScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchedulesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchedulesViewModel(scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
