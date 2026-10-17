package com.example.meditrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.meditrack.data.local.entity.Caregiver
import kotlinx.coroutines.flow.Flow

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregivers LIMIT 1")
    fun getCaregiver(): Flow<Caregiver?>

    @Query("SELECT * FROM caregivers LIMIT 1")
    suspend fun getCaregiverOnce(): Caregiver?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaregiver(caregiver: Caregiver): Long

    @Update
    suspend fun updateCaregiver(caregiver: Caregiver)

    @Query("UPDATE caregivers SET isEmergencyContact = :isEmergency WHERE id = :id")
    suspend fun updateEmergencyContactStatus(id: Long, isEmergency: Boolean)

    @Query("DELETE FROM caregivers")
    suspend fun deleteCaregiver()
}
