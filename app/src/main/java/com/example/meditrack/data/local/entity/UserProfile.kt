package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val createdAt: Long = System.currentTimeMillis()
)
