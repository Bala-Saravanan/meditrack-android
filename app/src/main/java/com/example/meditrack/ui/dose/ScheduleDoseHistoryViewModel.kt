package com.example.meditrack.ui.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.repository.DoseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleDoseHistoryViewModel(
    val scheduleId: Long,
    val targetDoseId: Long,
    private val doseRepository: DoseRepository
) : ViewModel() {

    private val _schedule = MutableStateFlow<Schedule?>(null)
    val schedule: StateFlow<Schedule?> = _schedule.asStateFlow()

    val historyDoses: StateFlow<List<DoseRecord>> = doseRepository
        .getDosesForSchedule(scheduleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _highlightedDose = MutableStateFlow<DoseRecord?>(null)
    val highlightedDose: StateFlow<DoseRecord?> = _highlightedDose.asStateFlow()

    init {
        loadSchedule()
        loadHighlightedDose()
    }

    private fun loadSchedule() {
        viewModelScope.launch(Dispatchers.IO) {
            _schedule.value = doseRepository.getScheduleById(scheduleId)
        }
    }

    private fun loadHighlightedDose() {
        viewModelScope.launch(Dispatchers.IO) {
            if (targetDoseId > 0) {
                _highlightedDose.value = doseRepository.getDoseById(targetDoseId)
            }
        }
    }

    fun markDoseTaken(doseId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            doseRepository.recordDoseTaken(doseId)
            // Reload highlighted dose if it was the one taken
            if (_highlightedDose.value?.id == doseId) {
                _highlightedDose.value = doseRepository.getDoseById(doseId)
            }
            onComplete?.invoke()
        }
    }

    fun markDoseSkipped(doseId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            doseRepository.recordDoseSkipped(doseId)
            // Reload highlighted dose if it was the one skipped
            if (_highlightedDose.value?.id == doseId) {
                _highlightedDose.value = doseRepository.getDoseById(doseId)
            }
            onComplete?.invoke()
        }
    }
}

class ScheduleDoseHistoryViewModelFactory(
    private val scheduleId: Long,
    private val targetDoseId: Long,
    private val doseRepository: DoseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleDoseHistoryViewModel::class.java)) {
            return ScheduleDoseHistoryViewModel(scheduleId, targetDoseId, doseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
