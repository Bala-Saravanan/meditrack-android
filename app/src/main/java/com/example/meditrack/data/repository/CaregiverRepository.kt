package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.CaregiverDao
import com.example.meditrack.data.local.entity.Caregiver
import kotlinx.coroutines.flow.Flow

class CaregiverRepository(private val caregiverDao: CaregiverDao) {

    val caregiver: Flow<Caregiver?> = caregiverDao.getCaregiver()

    suspend fun getCaregiverOnce(): Caregiver? {
        return caregiverDao.getCaregiverOnce()
    }

    suspend fun saveCaregiver(
        fullName: String,
        relationship: String,
        phone: String,
        email: String,
        isEmergencyContact: Boolean
    ): Long {
        val existing = caregiverDao.getCaregiverOnce()
        val caregiver = if (existing != null) {
            existing.copy(
                fullName = fullName.trim(),
                relationship = relationship.trim(),
                phone = phone.trim(),
                email = email.trim(),
                isEmergencyContact = isEmergencyContact,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Caregiver(
                fullName = fullName.trim(),
                relationship = relationship.trim(),
                phone = phone.trim(),
                email = email.trim(),
                isEmergencyContact = isEmergencyContact,
                updatedAt = System.currentTimeMillis()
            )
        }
        return caregiverDao.insertCaregiver(caregiver)
    }

    suspend fun updateEmergencyContactStatus(isEmergency: Boolean) {
        val existing = caregiverDao.getCaregiverOnce() ?: return
        caregiverDao.updateEmergencyContactStatus(existing.id, isEmergency)
    }

    suspend fun hasCaregiver(): Boolean {
        return caregiverDao.getCaregiverOnce() != null
    }
}
