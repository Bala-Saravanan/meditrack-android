package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.MedicineDao
import com.example.meditrack.data.local.entity.Medicine
import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {

    val allMedicines: Flow<List<Medicine>> = medicineDao.getAllMedicines()

    suspend fun getMedicineById(id: Long): Medicine? {
        return medicineDao.getMedicineById(id)
    }

    suspend fun addMedicine(name: String, instructions: String = "", form: String = "Oral Tablet"): Long {
        val medicine = Medicine(
            name = name.trim(),
            instructions = instructions.trim(),
            form = form.trim()
        )
        return medicineDao.insertMedicine(medicine)
    }

    suspend fun deleteMedicine(medicine: Medicine) {
        medicineDao.deleteMedicine(medicine)
    }
}
