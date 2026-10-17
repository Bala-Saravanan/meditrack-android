package com.example.meditrack.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.model.ScheduleReportCardItem
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val isLoading: Boolean = true,
    val cards: List<ScheduleReportCardItem> = emptyList(),
    val userInitial: String = "U",
    val error: String? = null
)

class ReportsViewModel(
    private val doseRepository: DoseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter

    val uiState: StateFlow<ReportsUiState> = combine(
        doseRepository.getAllScheduleReportCards(),
        userRepository.userProfile,
        _statusFilter
    ) { allCards, user, filter ->
        val filteredCards = when (filter) {
            "Active" -> allCards.filter { it.scheduleStatus == "Active" }
            "Completed" -> allCards.filter { it.scheduleStatus == "Completed" }
            "Discontinued" -> allCards.filter { it.scheduleStatus == "Discontinued" }
            else -> allCards
        }
        val initial = user?.fullName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

        ReportsUiState(
            isLoading = false,
            cards = filteredCards,
            userInitial = initial,
            error = null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }
}

class ReportsViewModelFactory(
    private val doseRepository: DoseRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            return ReportsViewModel(doseRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
