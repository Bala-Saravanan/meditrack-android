package com.example.meditrack.data.local.model

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

    // Adherence percentage based strictly on self-reported taken vs scheduled (excluding future pending doses)
    val adherencePercentage: Int
        get() {
            val eligible = totalScheduled - pendingCount
            if (eligible <= 0) return 0
            return ((takenCount.toDouble() / eligible.toDouble()) * 100).toInt().coerceIn(0, 100)
        }
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
