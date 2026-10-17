package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_records",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ScheduleMedicine::class,
            parentColumns = ["id"],
            childColumns = ["scheduleMedicineId"],
            onDelete = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = ScheduleMedicineTime::class,
            parentColumns = ["id"],
            childColumns = ["scheduleMedicineTimeId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("scheduleId"),
        Index("scheduleMedicineId"),
        Index(value = ["scheduleMedicineTimeId", "scheduledAt"], unique = true)
    ]
)
data class DoseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduleMedicineId: Long,
    val scheduleMedicineTimeId: Long,
    val medicineName: String,
    val period: String,
    val scheduledAt: Long,
    val recordedAt: Long? = null,
    val status: String = STATUS_PENDING,
    val isLate: Boolean = false,
    val foodInstruction: String = ""
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_TAKEN = "TAKEN"
        const val STATUS_SKIPPED = "SKIPPED"
        const val STATUS_UNCONFIRMED = "UNCONFIRMED"

        // Threshold past scheduled time after which recorded response is marked late (60 minutes)
        const val LATE_THRESHOLD_MS = 60 * 60 * 1000L

        /**
         * Late-dose evaluation is null-safe and only applies when a dose has a recorded response.
         * UNCONFIRMED, PENDING, or null recordedAt are never considered late.
         */
        fun calculateIsLate(scheduledAt: Long?, recordedAt: Long?, status: String? = null): Boolean {
            if (scheduledAt == null || recordedAt == null) return false
            if (status != null && status != STATUS_TAKEN && status != STATUS_SKIPPED) {
                return false
            }
            return (recordedAt - scheduledAt) > LATE_THRESHOLD_MS
        }
    }
}
