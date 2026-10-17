package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caregivers")
data class Caregiver(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val relationship: String,
    val phone: String,
    val email: String = "",
    val isEmergencyContact: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
