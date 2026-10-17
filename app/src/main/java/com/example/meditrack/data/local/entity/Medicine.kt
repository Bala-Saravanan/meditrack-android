package com.example.meditrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val instructions: String = "",
    val form: String = "Oral Tablet",
    val createdAt: Long = System.currentTimeMillis()
)
