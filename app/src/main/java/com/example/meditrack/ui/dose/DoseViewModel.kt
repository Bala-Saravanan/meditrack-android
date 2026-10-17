package com.example.meditrack.ui.dose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.repository.DoseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DoseFilter {
    ALL,
    UPCOMING,
    TAKEN,
    SKIPPED,
    UNCONFIRMED
}

data class FilterCounts(
    val all: Int = 0,
    val upcoming: Int = 0,
    val taken: Int = 0,
    val skipped: Int = 0,
    val unconfirmed: Int = 0
)

class DoseViewModel(
    private val doseRepository: DoseRepository
) : ViewModel() {

    private val _selectedDateMillis = MutableStateFlow(getStartOfDay(System.currentTimeMillis()))
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DoseFilter.ALL)
    val selectedFilter: StateFlow<DoseFilter> = _selectedFilter.asStateFlow()

    private val _dateStripItems = MutableStateFlow<List<DateStripItem>>(emptyList())
    val dateStripItems: StateFlow<List<DateStripItem>> = _dateStripItems.asStateFlow()

    init {
        updateDateStrip(_selectedDateMillis.value)
        refreshDosesForCurrentDate()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawDosesForDay: Flow<List<DoseRecord>> = _selectedDateMillis.flatMapLatest { dateMillis ->
        val startOfDay = getStartOfDay(dateMillis)
        val endOfDay = getEndOfDay(dateMillis)
        doseRepository.getDosesForDateRange(startOfDay, endOfDay)
    }

    val filterCounts: StateFlow<FilterCounts> = rawDosesForDay.map { doses ->
        val now = System.currentTimeMillis()
        FilterCounts(
            all = doses.size,
            upcoming = doses.count { it.status == DoseRecord.STATUS_PENDING },
            taken = doses.count { it.status == DoseRecord.STATUS_TAKEN },
            skipped = doses.count { it.status == DoseRecord.STATUS_SKIPPED },
            unconfirmed = doses.count { it.status == DoseRecord.STATUS_UNCONFIRMED }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilterCounts())

    val groupedDoseItems: StateFlow<List<DoseListItem>> = combine(
        rawDosesForDay,
        _selectedFilter
    ) { doses, filter ->
        val filtered = when (filter) {
            DoseFilter.ALL -> doses
            DoseFilter.UPCOMING -> doses.filter { it.status == DoseRecord.STATUS_PENDING }
            DoseFilter.TAKEN -> doses.filter { it.status == DoseRecord.STATUS_TAKEN }
            DoseFilter.SKIPPED -> doses.filter { it.status == DoseRecord.STATUS_SKIPPED }
            DoseFilter.UNCONFIRMED -> doses.filter { it.status == DoseRecord.STATUS_UNCONFIRMED }
        }

        val scheduleIds = filtered.map { it.scheduleId }.distinct()
        val scheduleMap = scheduleIds.associateWith { id ->
            doseRepository.getScheduleName(id)
        }

        // Sort schedules deterministically by scheduleName, then scheduleId
        val sortedScheduleIds = scheduleIds.sortedWith(
            compareBy({ scheduleMap[it]?.lowercase() ?: "" }, { it })
        )

        val resultList = mutableListOf<DoseListItem>()
        for (schedId in sortedScheduleIds) {
            val schedName = scheduleMap[schedId] ?: "Schedule #$schedId"
            val dosesForSched = filtered.filter { it.scheduleId == schedId }
                .sortedWith(compareBy({ it.scheduledAt }, { it.id }))

            resultList.add(DoseListItem.ScheduleHeader(schedId, schedName, dosesForSched.size))
            for (dose in dosesForSched) {
                resultList.add(DoseListItem.DoseEntry(dose = dose, scheduleName = schedName))
            }
        }
        resultList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalDoseCount: StateFlow<Int> = groupedDoseItems.map { items ->
        items.count { it is DoseListItem.DoseEntry }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSelectedDate(dateMillis: Long) {
        val startOfDay = getStartOfDay(dateMillis)
        _selectedDateMillis.value = startOfDay
        updateDateStrip(startOfDay)
        refreshDosesForCurrentDate()
    }

    fun setFilter(filter: DoseFilter) {
        _selectedFilter.value = filter
    }

    fun markDoseTaken(doseId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            doseRepository.recordDoseTaken(doseId)
            onComplete?.invoke()
        }
    }

    fun markDoseSkipped(doseId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            doseRepository.recordDoseSkipped(doseId)
            onComplete?.invoke()
        }
    }

    fun refreshDosesForCurrentDate() {
        val date = _selectedDateMillis.value
        viewModelScope.launch(Dispatchers.IO) {
            doseRepository.reconcileFutureDoses()
            doseRepository.generateDailyDosesForDate(date)
            doseRepository.markExpiredPendingAsUnconfirmed()
        }
    }

    private fun updateDateStrip(centerDateMillis: Long) {
        val cal = Calendar.getInstance()
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayOfMonthFormat = SimpleDateFormat("d", Locale.getDefault())

        val list = mutableListOf<DateStripItem>()
        // Generate a 7-day strip centered around the selected date (-3 to +3 days)
        for (i in -3..3) {
            cal.timeInMillis = centerDateMillis
            cal.add(Calendar.DAY_OF_YEAR, i)
            val dayStart = getStartOfDay(cal.timeInMillis)
            list.add(
                DateStripItem(
                    dateMillis = dayStart,
                    dayOfWeek = dayOfWeekFormat.format(Date(dayStart)),
                    dayOfMonth = dayOfMonthFormat.format(Date(dayStart)),
                    isToday = (dayStart == todayStart),
                    isSelected = (dayStart == centerDateMillis)
                )
            )
        }
        _dateStripItems.value = list
    }

    companion object {
        fun getStartOfDay(timeMillis: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = timeMillis
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun getEndOfDay(timeMillis: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = timeMillis
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            return c.timeInMillis
        }
    }
}

class DoseViewModelFactory(
    private val doseRepository: DoseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DoseViewModel::class.java)) {
            return DoseViewModel(doseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
