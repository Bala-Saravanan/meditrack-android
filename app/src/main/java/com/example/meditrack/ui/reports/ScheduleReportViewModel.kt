package com.example.meditrack.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.model.DateRangeType
import com.example.meditrack.data.local.model.ScheduleDetailedReport
import com.example.meditrack.data.repository.CaregiverRepository
import com.example.meditrack.data.repository.DoseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class ScheduleReportViewModel(
    private val scheduleId: Long,
    private val doseRepository: DoseRepository,
    private val caregiverRepository: CaregiverRepository
) : ViewModel() {

    private val _selectedRangeType = MutableStateFlow(DateRangeType.LAST_30_DAYS)
    val selectedRangeType: StateFlow<DateRangeType> = _selectedRangeType

    private val _customStart = MutableStateFlow<Long?>(null)
    private val _customEnd = MutableStateFlow<Long?>(null)

    val caregiver: StateFlow<Caregiver?> = caregiverRepository.caregiver.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val report: StateFlow<ScheduleDetailedReport?> = combine(
        _selectedRangeType,
        _customStart,
        _customEnd
    ) { rangeType, start, end ->
        Triple(rangeType, start, end)
    }.flatMapLatest { (rangeType, start, end) ->
        doseRepository.getScheduleDetailedReport(scheduleId, rangeType, start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setRangeType(rangeType: DateRangeType) {
        _selectedRangeType.value = rangeType
    }

    fun setCustomRange(start: Long, end: Long) {
        val validStart = minOf(start, end)
        val validEnd = maxOf(start, end)
        _customStart.value = validStart
        _customEnd.value = validEnd
        _selectedRangeType.value = DateRangeType.CUSTOM
    }

    suspend fun getReportOnce(): ScheduleDetailedReport? {
        return doseRepository.getScheduleDetailedReportOnce(
            scheduleId = scheduleId,
            rangeType = _selectedRangeType.value,
            customStart = _customStart.value,
            customEnd = _customEnd.value
        )
    }
}

class ScheduleReportViewModelFactory(
    private val scheduleId: Long,
    private val doseRepository: DoseRepository,
    private val caregiverRepository: CaregiverRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleReportViewModel::class.java)) {
            return ScheduleReportViewModel(scheduleId, doseRepository, caregiverRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
