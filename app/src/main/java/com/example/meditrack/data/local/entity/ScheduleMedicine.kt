package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedule_medicines",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("scheduleId"),
        Index("medicineId")
    ]
)
data class ScheduleMedicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val medicineId: Long,
    val foodInstruction: String = FOOD_BEFORE,
    val doseQuantity: String = "1 Tablet",
    val isActive: Boolean = true
) {
    companion object {
        const val FOOD_BEFORE = "BEFORE_FOOD"
        const val FOOD_WITH = "WITH_FOOD"
        const val FOOD_NO_RESTRICTION = "NO_RESTRICTION"
    }
}
