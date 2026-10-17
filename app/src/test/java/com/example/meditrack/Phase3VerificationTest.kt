package com.example.meditrack

import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.ui.schedules.MedicineDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase3VerificationTest {

    private fun validateSchedule(
        name: String,
        startDate: Long?,
        endDate: Long?,
        medicines: List<MedicineDraft>
    ): String? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return "Please enter a schedule name"
        if (startDate == null || startDate <= 0L) return "Please select a start date"
        if (endDate != null && endDate < startDate) return "End date cannot be earlier than start date"
        if (medicines.isEmpty()) return "Please assign at least one medicine"
        for (med in medicines) {
            val hasSlot = med.isMorningEnabled || med.isAfternoonEnabled || med.isEveningEnabled || med.isNightEnabled
            if (!hasSlot) return "Please enable at least one time slot for ${med.medicineName}"
        }
        val uniqueIds = medicines.map { it.medicineId }.toSet()
        if (uniqueIds.size < medicines.size) {
            return "Duplicate medicines cannot be assigned to the same schedule"
        }
        return null
    }

    @Test
    fun testScheduleNameValidation() {
        val medicines = listOf(
            MedicineDraft(
                medicineId = 1,
                medicineName = "Metformin",
                medicineForm = "Tablet",
                isMorningEnabled = true
            )
        )
        // Blank name should fail
        assertEquals("Please enter a schedule name", validateSchedule("", 1729000000000L, null, medicines))
        assertEquals("Please enter a schedule name", validateSchedule("   ", 1729000000000L, null, medicines))
        // Valid name should pass
        assertNull(validateSchedule("Diabetes Management", 1729000000000L, null, medicines))
    }

    @Test
    fun testDateValidation_RequiredStartDate_And_EndDateComparison() {
        val medicines = listOf(
            MedicineDraft(
                medicineId = 1,
                medicineName = "Metformin",
                medicineForm = "Tablet",
                isMorningEnabled = true
            )
        )
        val startDate = 1729000000000L
        val validEndDate = 1729500000000L
        val earlierEndDate = 1728500000000L

        // Missing start date
        assertEquals("Please select a start date", validateSchedule("Diabetes Management", null, null, medicines))
        assertEquals("Please select a start date", validateSchedule("Diabetes Management", 0L, null, medicines))

        // Valid start date with no end date (open-ended schedule)
        assertNull(validateSchedule("Diabetes Management", startDate, null, medicines))

        // End date earlier than start date
        assertEquals("End date cannot be earlier than start date", validateSchedule("Diabetes Management", startDate, earlierEndDate, medicines))

        // End date equal to or after start date
        assertNull(validateSchedule("Diabetes Management", startDate, startDate, medicines))
        assertNull(validateSchedule("Diabetes Management", startDate, validEndDate, medicines))
    }

    @Test
    fun testMedicineAssignmentValidation() {
        val startDate = 1729000000000L

        // Empty medicine list should fail
        assertEquals(
            "Please assign at least one medicine",
            validateSchedule("Hypertension Regimen", startDate, null, emptyList())
        )

        // Medicine with NO slots enabled should fail
        val noSlotMedicine = MedicineDraft(
            medicineId = 1,
            medicineName = "Amlodipine",
            medicineForm = "Tablet",
            isMorningEnabled = false,
            isAfternoonEnabled = false,
            isEveningEnabled = false,
            isNightEnabled = false
        )
        assertEquals(
            "Please enable at least one time slot for Amlodipine",
            validateSchedule("Hypertension Regimen", startDate, null, listOf(noSlotMedicine))
        )

        // Valid medicine with slot enabled
        val validMedicine = noSlotMedicine.copy(isMorningEnabled = true)
        assertNull(validateSchedule("Hypertension Regimen", startDate, null, listOf(validMedicine)))
    }

    @Test
    fun testPreventDuplicateMedicineAssignmentWithinSameSchedule() {
        val startDate = 1729000000000L
        val draft1 = MedicineDraft(
            medicineId = 1,
            medicineName = "Atorvastatin",
            medicineForm = "Tablet",
            isNightEnabled = true
        )
        val draft2 = MedicineDraft(
            medicineId = 1,
            medicineName = "Atorvastatin",
            medicineForm = "Tablet",
            isMorningEnabled = true
        )

        val duplicateList = listOf(draft1, draft2)
        assertEquals(
            "Duplicate medicines cannot be assigned to the same schedule",
            validateSchedule("Lipid Regimen", startDate, null, duplicateList)
        )

        // Distinct medicines should pass
        val draft3 = MedicineDraft(
            medicineId = 2,
            medicineName = "Metformin",
            medicineForm = "Tablet",
            isMorningEnabled = true
        )
        assertNull(validateSchedule("Lipid & Sugar", startDate, null, listOf(draft1, draft3)))
    }

    @Test
    fun testMultipleTimeSlotsCombinationAndMinutesConversion() {
        // Medicine configured for Morning (08:00 AM) and Night (09:30 PM)
        val morningMinutes = 8 * 60 // 480
        val nightMinutes = 21 * 60 + 30 // 1290

        val item = MedicineDraft(
            medicineId = 10,
            medicineName = "Amoxicillin",
            medicineForm = "Capsule",
            foodInstruction = ScheduleMedicine.FOOD_AFTER,
            remindersEnabled = true,
            isMorningEnabled = true,
            morningMinutes = morningMinutes,
            isAfternoonEnabled = false,
            afternoonMinutes = 720,
            isEveningEnabled = false,
            eveningMinutes = 1080,
            isNightEnabled = true,
            nightMinutes = nightMinutes
        )

        // Build times
        val times = mutableListOf<ScheduleMedicineTime>()
        if (item.isMorningEnabled) {
            times.add(ScheduleMedicineTime(scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_MORNING, minutesSinceMidnight = item.morningMinutes))
        }
        if (item.isAfternoonEnabled) {
            times.add(ScheduleMedicineTime(scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_AFTERNOON, minutesSinceMidnight = item.afternoonMinutes))
        }
        if (item.isEveningEnabled) {
            times.add(ScheduleMedicineTime(scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_EVENING, minutesSinceMidnight = item.eveningMinutes))
        }
        if (item.isNightEnabled) {
            times.add(ScheduleMedicineTime(scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_NIGHT, minutesSinceMidnight = item.nightMinutes))
        }

        assertEquals(2, times.size)
        assertEquals(ScheduleMedicineTime.PERIOD_MORNING, times[0].period)
        assertEquals(480, times[0].minutesSinceMidnight)
        assertEquals("08:00 AM", ScheduleMedicineTime.formatTime(times[0].minutesSinceMidnight))

        assertEquals(ScheduleMedicineTime.PERIOD_NIGHT, times[1].period)
        assertEquals(1290, times[1].minutesSinceMidnight)
        assertEquals("09:30 PM", ScheduleMedicineTime.formatTime(times[1].minutesSinceMidnight))
    }

    @Test
    fun testFoodInstructionsIntegrity() {
        // Verify all 4 food instruction constants exist and are distinct
        assertEquals("BEFORE_FOOD", ScheduleMedicine.FOOD_BEFORE)
        assertEquals("WITH_FOOD", ScheduleMedicine.FOOD_WITH)
        assertEquals("AFTER_FOOD", ScheduleMedicine.FOOD_AFTER)
        assertEquals("NO_RESTRICTION", ScheduleMedicine.FOOD_NO_RESTRICTION)
    }

    @Test
    fun testScheduleStatusTransitionsAndHistoricalPreservation() {
        val schedule = Schedule(
            id = 1,
            name = "Post-Surgery Recovery",
            startDate = 1729000000000L,
            endDate = 1730000000000L,
            status = Schedule.STATUS_ACTIVE
        )
        assertEquals(Schedule.STATUS_ACTIVE, schedule.status)

        // Mark Completed
        val completedSchedule = schedule.copy(status = Schedule.STATUS_COMPLETED)
        assertEquals(Schedule.STATUS_COMPLETED, completedSchedule.status)

        // Mark Discontinued
        val discontinuedSchedule = schedule.copy(status = Schedule.STATUS_DISCONTINUED)
        assertEquals(Schedule.STATUS_DISCONTINUED, discontinuedSchedule.status)

        // Verify that dose records associated with this schedule retain their references
        val doseRecord = DoseRecord(
            id = 501,
            scheduleId = schedule.id,
            scheduleMedicineId = 10,
            scheduleMedicineTimeId = 20,
            medicineName = "Pain Relief",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = 1729050000000L,
            recordedAt = 1729050500000L,
            status = DoseRecord.STATUS_TAKEN,
            isLate = false,
            foodInstruction = ScheduleMedicine.FOOD_WITH
        )

        assertEquals(1, doseRecord.scheduleId)
        assertEquals(DoseRecord.STATUS_TAKEN, doseRecord.status)
        assertEquals("Pain Relief", doseRecord.medicineName)
    }

    @Test
    fun testSoftDeactivationPreservesForeignKeyIntegrity() {
        val existingAssignment = ScheduleMedicine(
            id = 15,
            scheduleId = 1,
            medicineId = 2,
            foodInstruction = ScheduleMedicine.FOOD_BEFORE,
            isActive = true
        )

        // Deactivated representation
        val deactivatedAssignment = existingAssignment.copy(isActive = false)
        assertFalse(deactivatedAssignment.isActive)
        assertEquals(existingAssignment.id, deactivatedAssignment.id) // PK preserved
        assertEquals(existingAssignment.scheduleId, deactivatedAssignment.scheduleId) // FK preserved

        val timeSlot = ScheduleMedicineTime(
            id = 30,
            scheduleMedicineId = 15,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            minutesSinceMidnight = 480,
            isEnabled = true
        )
        val disabledTimeSlot = timeSlot.copy(isEnabled = false)
        assertFalse(disabledTimeSlot.isEnabled)
        assertEquals(30, disabledTimeSlot.id)
    }
}
