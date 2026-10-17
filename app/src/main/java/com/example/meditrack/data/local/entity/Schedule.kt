package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startDate: Long,
    val endDate: Long? = null,
    val status: String = STATUS_ACTIVE,
    val notificationsEnabled: Boolean = true,
    val physicianName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_COMPLETED = "Completed"
        const val STATUS_DISCONTINUED = "Discontinued"
    }
}
