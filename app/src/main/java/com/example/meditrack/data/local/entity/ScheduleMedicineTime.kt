package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_medicine_times",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleMedicine::class,
            parentColumns = ["id"],
            childColumns = ["scheduleMedicineId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("scheduleMedicineId")
    ]
)
data class ScheduleMedicineTime(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleMedicineId: Long,
    val period: String, // Morning, Afternoon, Evening, Night
    val minutesSinceMidnight: Int, // e.g., 480 = 08:00 AM, 1200 = 08:00 PM
    val isEnabled: Boolean = true
) {
    companion object {
        const val PERIOD_MORNING = "Morning"
        const val PERIOD_AFTERNOON = "Afternoon"
        const val PERIOD_EVENING = "Evening"
        const val PERIOD_NIGHT = "Night"

        fun formatTime(minutes: Int): String {
            val hours24 = minutes / 60
            val mins = minutes % 60
            val amPm = if (hours24 >= 12) "PM" else "AM"
            val hours12 = when (hours24 % 12) {
                0 -> 12
                else -> hours24 % 12
            }
            return String.format("%02d:%02d %s", hours12, mins, amPm)
        }
    }
}
