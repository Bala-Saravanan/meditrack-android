package com.example.meditrack.data.local

import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.entity.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object DatabaseInitializer {

    fun populateInitialData(database: MediTrackDatabase, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val userDao = database.userDao()
            if (userDao.getUserProfileOnce() != null) return@launch // Already seeded

            // 1. User Profile
            val user = UserProfile(id = 1, fullName = "Johnathan Davis")
            userDao.insertUser(user)

            // 2. Caregiver
            val caregiverDao = database.caregiverDao()
            val caregiver = Caregiver(
                id = 1,
                fullName = "Sarah Davis",
                relationship = "Daughter",
                phone = "+1 (555) 019-2834",
                email = "sarah.davis@example.com",
                isEmergencyContact = true
            )
            caregiverDao.insertCaregiver(caregiver)

            // 3. Medicines
            val medicineDao = database.medicineDao()
            val med1 = Medicine(id = 1, name = "Atorvastatin 20mg", instructions = "Take with water before breakfast", form = "Oral Tablet")
            val med2 = Medicine(id = 2, name = "Lisinopril 10mg", instructions = "Take with meal", form = "Oral Tablet")
            val med3 = Medicine(id = 3, name = "Baby Aspirin 81mg", instructions = "Bedtime with full glass of water", form = "Oral Tablet")
            val med4 = Medicine(id = 4, name = "Multivitamin Complex", instructions = "Take with morning snack or meal", form = "Softgel")
            val med5 = Medicine(id = 5, name = "Metformin 500mg", instructions = "Take with meals", form = "Oral Tablet")
            val med6 = Medicine(id = 6, name = "Amoxicillin 250mg", instructions = "Complete entire course", form = "Capsule")

            medicineDao.insertMedicines(listOf(med1, med2, med3, med4, med5, med6))

            // 4. Schedules
            val scheduleDao = database.scheduleDao()
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis

            cal.add(Calendar.DAY_OF_YEAR, -14)
            val schedule1Start = cal.timeInMillis

            val schedule1 = Schedule(
                id = 1,
                name = "Cardiovascular Care",
                startDate = schedule1Start,
                endDate = null,
                status = Schedule.STATUS_ACTIVE,
                notificationsEnabled = true,
                physicianName = "Dr. Ronald Vance",
                notes = "Twice daily routine prescribed for hypertension and cardiovascular maintenance."
            )

            cal.timeInMillis = todayStart
            cal.add(Calendar.DAY_OF_YEAR, -5)
            val schedule2Start = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 10)
            val schedule2End = cal.timeInMillis

            val schedule2 = Schedule(
                id = 2,
                name = "Post-Surgery Recovery",
                startDate = schedule2Start,
                endDate = schedule2End,
                status = Schedule.STATUS_ACTIVE,
                notificationsEnabled = true,
                physicianName = "Dr. Aris Thorne",
                notes = "Antibiotic and anti-inflammatory recovery course post-procedure."
            )

            scheduleDao.insertSchedule(schedule1)
            scheduleDao.insertSchedule(schedule2)

            // 5. Schedule Medicines
            val sm1 = ScheduleMedicine(id = 1, scheduleId = 1, medicineId = 1, foodInstruction = ScheduleMedicine.FOOD_BEFORE, doseQuantity = "1 Tablet")
            val sm2 = ScheduleMedicine(id = 2, scheduleId = 1, medicineId = 2, foodInstruction = ScheduleMedicine.FOOD_WITH, doseQuantity = "1 Tablet")
            val sm3 = ScheduleMedicine(id = 3, scheduleId = 1, medicineId = 3, foodInstruction = ScheduleMedicine.FOOD_NO_RESTRICTION, doseQuantity = "1 Tablet")
            val sm4 = ScheduleMedicine(id = 4, scheduleId = 2, medicineId = 6, foodInstruction = ScheduleMedicine.FOOD_WITH, doseQuantity = "1 Capsule")

            scheduleDao.insertScheduleMedicine(sm1)
            scheduleDao.insertScheduleMedicine(sm2)
            scheduleDao.insertScheduleMedicine(sm3)
            scheduleDao.insertScheduleMedicine(sm4)

            // 6. Schedule Medicine Times (Normalized table)
            // sm1 (Atorvastatin): Morning 08:00 AM (480 mins)
            val t1 = ScheduleMedicineTime(id = 1, scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_MORNING, minutesSinceMidnight = 480)
            // sm2 (Lisinopril): Morning 08:00 AM (480 mins), Evening 08:00 PM (1200 mins)
            val t2 = ScheduleMedicineTime(id = 2, scheduleMedicineId = 2, period = ScheduleMedicineTime.PERIOD_MORNING, minutesSinceMidnight = 480)
            val t3 = ScheduleMedicineTime(id = 3, scheduleMedicineId = 2, period = ScheduleMedicineTime.PERIOD_EVENING, minutesSinceMidnight = 1200)
            // sm3 (Aspirin): Night 10:00 PM (1320 mins)
            val t4 = ScheduleMedicineTime(id = 4, scheduleMedicineId = 3, period = ScheduleMedicineTime.PERIOD_NIGHT, minutesSinceMidnight = 1320)
            // sm4 (Amoxicillin): Morning 08:00 AM (480 mins), Afternoon 02:00 PM (840 mins), Evening 08:00 PM (1200 mins)
            val t5 = ScheduleMedicineTime(id = 5, scheduleMedicineId = 4, period = ScheduleMedicineTime.PERIOD_MORNING, minutesSinceMidnight = 480)
            val t6 = ScheduleMedicineTime(id = 6, scheduleMedicineId = 4, period = ScheduleMedicineTime.PERIOD_AFTERNOON, minutesSinceMidnight = 840)
            val t7 = ScheduleMedicineTime(id = 7, scheduleMedicineId = 4, period = ScheduleMedicineTime.PERIOD_EVENING, minutesSinceMidnight = 1200)

            scheduleDao.insertScheduleMedicineTimes(listOf(t1, t2, t3, t4, t5, t6, t7))

            // 7. Seed Dose Records for Past 4 Days and Today
            val doseDao = database.doseRecordDao()
            val doseList = mutableListOf<DoseRecord>()

            // Past 3 days + today:
            for (dayOffset in -3..0) {
                val dayCal = Calendar.getInstance()
                dayCal.timeInMillis = todayStart
                dayCal.add(Calendar.DAY_OF_YEAR, dayOffset)
                val dayBase = dayCal.timeInMillis

                val isToday = dayOffset == 0

                // Dose 1: Atorvastatin (08:00 AM)
                val timeAtor = dayBase + (8 * 3600 * 1000L)
                val statusAtor = when {
                    isToday -> DoseRecord.STATUS_PENDING
                    dayOffset == -1 -> DoseRecord.STATUS_SKIPPED
                    else -> DoseRecord.STATUS_TAKEN
                }
                val recAtor = if (statusAtor == DoseRecord.STATUS_TAKEN) timeAtor + (12 * 60 * 1000L) else if (statusAtor == DoseRecord.STATUS_SKIPPED) timeAtor + (30 * 60 * 1000L) else null
                doseList.add(
                    DoseRecord(
                        scheduleId = 1,
                        scheduleMedicineId = 1,
                        scheduleMedicineTimeId = 1,
                        medicineName = "Atorvastatin 20mg",
                        period = ScheduleMedicineTime.PERIOD_MORNING,
                        scheduledAt = timeAtor,
                        recordedAt = recAtor,
                        status = statusAtor,
                        isLate = false,
                        foodInstruction = ScheduleMedicine.FOOD_BEFORE
                    )
                )

                // Dose 2: Lisinopril Morning (08:00 AM)
                val timeLisM = dayBase + (8 * 3600 * 1000L)
                val statusLisM = when {
                    isToday -> DoseRecord.STATUS_PENDING
                    dayOffset == -2 -> DoseRecord.STATUS_UNCONFIRMED
                    else -> DoseRecord.STATUS_TAKEN
                }
                val recLisM = if (statusLisM == DoseRecord.STATUS_TAKEN) timeLisM + (5 * 60 * 1000L) else null
                doseList.add(
                    DoseRecord(
                        scheduleId = 1,
                        scheduleMedicineId = 2,
                        scheduleMedicineTimeId = 2,
                        medicineName = "Lisinopril 10mg",
                        period = ScheduleMedicineTime.PERIOD_MORNING,
                        scheduledAt = timeLisM,
                        recordedAt = recLisM,
                        status = statusLisM,
                        isLate = false,
                        foodInstruction = ScheduleMedicine.FOOD_WITH
                    )
                )

                // Dose 3: Lisinopril Evening (08:00 PM)
                val timeLisE = dayBase + (20 * 3600 * 1000L)
                val statusLisE = when {
                    isToday -> DoseRecord.STATUS_PENDING
                    else -> DoseRecord.STATUS_TAKEN
                }
                val recLisE = if (statusLisE == DoseRecord.STATUS_TAKEN) timeLisE + (10 * 60 * 1000L) else null
                doseList.add(
                    DoseRecord(
                        scheduleId = 1,
                        scheduleMedicineId = 2,
                        scheduleMedicineTimeId = 3,
                        medicineName = "Lisinopril 10mg",
                        period = ScheduleMedicineTime.PERIOD_EVENING,
                        scheduledAt = timeLisE,
                        recordedAt = recLisE,
                        status = statusLisE,
                        isLate = false,
                        foodInstruction = ScheduleMedicine.FOOD_WITH
                    )
                )

                // Dose 4: Post-surgery recovery (Amoxicillin Morning)
                val timeAmoxM = dayBase + (8 * 3600 * 1000L)
                val statusAmoxM = if (isToday) DoseRecord.STATUS_PENDING else DoseRecord.STATUS_TAKEN
                val recAmoxM = if (statusAmoxM == DoseRecord.STATUS_TAKEN) timeAmoxM + (3 * 60 * 1000L) else null
                doseList.add(
                    DoseRecord(
                        scheduleId = 2,
                        scheduleMedicineId = 4,
                        scheduleMedicineTimeId = 5,
                        medicineName = "Amoxicillin 250mg",
                        period = ScheduleMedicineTime.PERIOD_MORNING,
                        scheduledAt = timeAmoxM,
                        recordedAt = recAmoxM,
                        status = statusAmoxM,
                        isLate = false,
                        foodInstruction = ScheduleMedicine.FOOD_WITH
                    )
                )
            }

            doseDao.insertDoseRecords(doseList)
        }
    }

    suspend fun clearAllData(database: MediTrackDatabase) {
        database.clearAllTables()
    }
}
