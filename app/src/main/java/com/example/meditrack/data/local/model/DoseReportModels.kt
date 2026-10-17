package com.example.meditrack.data.local.model

import com.example.meditrack.data.local.entity.DoseRecord

enum class DateRangeType(val label: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ENTIRE_PERIOD("Entire Schedule Period"),
    CUSTOM("Custom")
}

data class ScheduleAdherenceSummary(
    val scheduleId: Long,
    val scheduleName: String,
    val totalScheduled: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val unconfirmedCount: Int,
    val pendingCount: Int,
    val lateCount: Int
) {
    val completedActionCount: Int
        get() = takenCount + skippedCount

    val eligibleDoses: Int
        get() = (totalScheduled - pendingCount).coerceAtLeast(0)

    // Adherence percentage based strictly on self-reported taken vs eligible scheduled (excluding future pending doses)
    val adherencePercentage: Int
        get() {
            if (eligibleDoses <= 0) return 0
            return ((takenCount.toDouble() / eligibleDoses.toDouble()) * 100).toInt().coerceIn(0, 100)
        }

    val takenPercentage: Double
        get() = if (eligibleDoses > 0) (takenCount.toDouble() / eligibleDoses.toDouble()) * 100.0 else 0.0

    val skippedPercentage: Double
        get() = if (eligibleDoses > 0) (skippedCount.toDouble() / eligibleDoses.toDouble()) * 100.0 else 0.0

    val unconfirmedPercentage: Double
        get() = if (eligibleDoses > 0) (unconfirmedCount.toDouble() / eligibleDoses.toDouble()) * 100.0 else 0.0
}

data class MedicineAdherenceSummary(
    val medicineName: String,
    val totalScheduled: Int,
    val takenCount: Int,
    val skippedCount: Int,
    val unconfirmedCount: Int,
    val lateCount: Int
) {
    val adherencePercentage: Int
        get() {
            if (totalScheduled <= 0) return 0
            return ((takenCount.toDouble() / totalScheduled.toDouble()) * 100).toInt().coerceIn(0, 100)
        }
}

data class ScheduleReportCardItem(
    val scheduleId: Long,
    val scheduleName: String,
    val scheduleStatus: String,
    val startDate: Long,
    val endDate: Long?,
    val adherenceSummary: ScheduleAdherenceSummary
)

data class ScheduleDetailedReport(
    val scheduleId: Long,
    val scheduleName: String,
    val scheduleStatus: String,
    val startDate: Long,
    val endDate: Long?,
    val physicianName: String,
    val notes: String,
    val dateRangeType: DateRangeType,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val adherenceSummary: ScheduleAdherenceSummary,
    val medicineSummaries: List<MedicineAdherenceSummary>,
    val doseRecords: List<DoseRecord>,
    val hasFutureScheduledDoses: Boolean
)
