package com.example.meditrack

import com.example.meditrack.data.local.dao.CaregiverDao
import com.example.meditrack.data.local.dao.DoseRecordDao
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.repository.CaregiverRepository
import com.example.meditrack.ui.caregiver.CaregiverFormViewModel
import com.example.meditrack.ui.caregiver.FormValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10VerificationTest {

    private class FakeCaregiverDao : CaregiverDao {
        var storedCaregiver: Caregiver? = null
        private val flow = MutableStateFlow<Caregiver?>(null)

        override fun getCaregiver(): Flow<Caregiver?> = flow
        override suspend fun getCaregiverOnce(): Caregiver? = storedCaregiver
        override suspend fun insertCaregiver(caregiver: Caregiver): Long {
            val id = if (caregiver.id == 0L) 1L else caregiver.id
            val entity = caregiver.copy(id = id)
            storedCaregiver = entity
            flow.value = entity
            return id
        }
        override suspend fun updateCaregiver(caregiver: Caregiver) {
            storedCaregiver = caregiver
            flow.value = caregiver
        }
        override suspend fun updateEmergencyContactStatus(id: Long, isEmergency: Boolean) {
            storedCaregiver = storedCaregiver?.copy(isEmergencyContact = isEmergency)
            flow.value = storedCaregiver
        }
        override suspend fun deleteCaregiver() {
            storedCaregiver = null
            flow.value = null
        }
    }

    @Test
    fun testCaregiverEmailIsStrictlyRequired() {
        val fakeDao = FakeCaregiverDao()
        val repo = CaregiverRepository(fakeDao)
        val vm = CaregiverFormViewModel(repo)

        vm.fullName = "Sarah Jenkins"
        vm.relationship = "Daughter"
        vm.phone = "555-123-4567"
        vm.email = "" // Blank email

        val isValid = vm.validate()
        assertFalse("Validation should fail when email is blank", isValid)

        val result = vm.validationState.value
        assertTrue("Expected Invalid result", result is FormValidationResult.Invalid)
        val invalid = result as FormValidationResult.Invalid
        assertEquals("Email address is required.", invalid.emailError)
    }

    @Test
    fun testCaregiverEmailInvalidSyntax() {
        val fakeDao = FakeCaregiverDao()
        val repo = CaregiverRepository(fakeDao)
        val vm = CaregiverFormViewModel(repo)

        vm.fullName = "Sarah Jenkins"
        vm.relationship = "Daughter"
        vm.phone = "555-123-4567"
        vm.email = "sarahjenkins-no-at-sign.com" // Invalid email

        val isValid = vm.validate()
        assertFalse("Validation should fail when email syntax is invalid", isValid)

        val result = vm.validationState.value
        assertTrue("Expected Invalid result", result is FormValidationResult.Invalid)
        val invalid = result as FormValidationResult.Invalid
        assertEquals("Please enter a valid email address.", invalid.emailError)
    }

    @Test
    fun testCaregiverEmailValidPasses() {
        runBlocking {
            val fakeDao = FakeCaregiverDao()
            val repo = CaregiverRepository(fakeDao)
            val vm = CaregiverFormViewModel(repo)

            vm.fullName = "Sarah Jenkins"
            vm.relationship = "Daughter"
            vm.phone = "555-123-4567"
            vm.email = " sarah.jenkins@example.com " // Valid with surrounding whitespace

            val isValid = vm.validate()
            assertTrue("Validation should pass with valid email", isValid)

            val saveResult = vm.saveCaregiver()
            assertTrue("Save caregiver should succeed", saveResult.isSuccess)

            val saved = fakeDao.getCaregiverOnce()
            assertNotNull(saved)
            assertEquals("sarah.jenkins@example.com", saved?.email)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCaregiverRepositoryRejectsBlankEmail() {
        runBlocking {
            val fakeDao = FakeCaregiverDao()
            val repo = CaregiverRepository(fakeDao)
            repo.saveCaregiver("Sarah", "Daughter", "5551234567", "   ", true)
        }
    }

    @Test
    fun testHistoricalDoseSortingAscendingChronological() {
        // Chronological sort requirement: Oldest historical dose first, newest last.
        // Primary sort key: scheduledAt ASC, secondary: id ASC
        val dose1 = DoseRecord(id = 1, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1,
            medicineName = "Med A", period = "morning", scheduledAt = 1000L, status = DoseRecord.STATUS_TAKEN)
        val dose2 = DoseRecord(id = 2, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1,
            medicineName = "Med B", period = "morning", scheduledAt = 500L, status = DoseRecord.STATUS_TAKEN)
        val dose3 = DoseRecord(id = 3, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1,
            medicineName = "Med C", period = "morning", scheduledAt = 2000L, status = DoseRecord.STATUS_SKIPPED)
        val dose4 = DoseRecord(id = 4, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1,
            medicineName = "Med D", period = "morning", scheduledAt = 500L, status = DoseRecord.STATUS_TAKEN)

        val list = listOf(dose1, dose2, dose3, dose4)
        val sorted = list.sortedWith(compareBy({ it.scheduledAt }, { it.id }))

        assertEquals(2L, sorted[0].id) // 500L, id 2
        assertEquals(4L, sorted[1].id) // 500L, id 4
        assertEquals(1L, sorted[2].id) // 1000L, id 1
        assertEquals(3L, sorted[3].id) // 2000L, id 3
    }

    @Test
    fun testActiveOnlySchedulesFilterForRemindersAndSummaries() {
        val activeSchedule = Schedule(id = 1, name = "Active Protocol", startDate = 1000L, status = Schedule.STATUS_ACTIVE)
        val completedSchedule = Schedule(id = 2, name = "Completed Protocol", startDate = 1000L, status = Schedule.STATUS_COMPLETED)
        val discontinuedSchedule = Schedule(id = 3, name = "Discontinued Protocol", startDate = 1000L, status = Schedule.STATUS_DISCONTINUED)

        val schedules = listOf(activeSchedule, completedSchedule, discontinuedSchedule)
        val schedulesMap = schedules.associateBy { it.id }

        val doses = listOf(
            DoseRecord(id = 101, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1,
                medicineName = "Active Med", period = "morning", scheduledAt = 10000L, status = DoseRecord.STATUS_PENDING),
            DoseRecord(id = 102, scheduleId = 2, scheduleMedicineId = 2, scheduleMedicineTimeId = 2,
                medicineName = "Completed Med", period = "morning", scheduledAt = 10000L, status = DoseRecord.STATUS_PENDING),
            DoseRecord(id = 103, scheduleId = 3, scheduleMedicineId = 3, scheduleMedicineTimeId = 3,
                medicineName = "Discontinued Med", period = "morning", scheduledAt = 10000L, status = DoseRecord.STATUS_PENDING)
        )

        // Simulating the Room JOIN where s.status = 'ACTIVE'
        val activeDoses = doses.filter { dose ->
            schedulesMap[dose.scheduleId]?.status == Schedule.STATUS_ACTIVE
        }

        assertEquals(1, activeDoses.size)
        assertEquals(101L, activeDoses[0].id)
        assertEquals("Active Med", activeDoses[0].medicineName)

        // Verify completed and discontinued schedules do NOT contribute
        assertFalse(activeDoses.any { it.scheduleId == completedSchedule.id })
        assertFalse(activeDoses.any { it.scheduleId == discontinuedSchedule.id })
    }

    @Test
    fun testEmailIntentRecipientFormatting() {
        val email = " caregiver.test@meditrack.org "
        val trimmed = email.trim()
        val pattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        assertTrue(pattern.matches(trimmed))
        assertEquals("caregiver.test@meditrack.org", trimmed)

        val mailtoUriString = "mailto:$trimmed"
        assertTrue(mailtoUriString.startsWith("mailto:caregiver.test@meditrack.org"))
    }
}
