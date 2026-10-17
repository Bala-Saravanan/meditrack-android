package com.example.meditrack.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.model.FullScheduleDetails
import com.example.meditrack.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScheduleDetailsViewModel(
    private val scheduleId: Long,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    val scheduleDetails: Flow<FullScheduleDetails?> = scheduleRepository.getFullScheduleDetails(scheduleId)

    fun updateStatus(status: String) {
        viewModelScope.launch {
            scheduleRepository.updateScheduleStatus(scheduleId, status)
        }
    }
}

class ScheduleDetailsViewModelFactory(
    private val scheduleId: Long,
    private val scheduleRepository: ScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleDetailsViewModel(scheduleId, scheduleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
