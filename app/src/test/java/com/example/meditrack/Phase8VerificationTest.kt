package com.example.meditrack

import com.example.meditrack.data.local.dao.CaregiverDao
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.DateRangeType
import com.example.meditrack.data.local.model.MedicineAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.repository.CaregiverRepository
import com.example.meditrack.reminder.ReminderScheduler
import com.example.meditrack.ui.caregiver.CaregiverFormViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Phase8VerificationTest {

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
    fun testEndToEnd_AdherenceCalculationsAndFuturePendingExclusion() {
        // Total 20 doses: 12 taken, 2 skipped, 2 unconfirmed, 4 future pending
        // Eligible doses = 20 - 4 = 16
        // Adherence percentage = (12 / 16) * 100 = 75%
        val summary = ScheduleAdherenceSummary(
            scheduleId = 10L,
            scheduleName = "Cardiovascular Therapy",
            totalScheduled = 20,
            takenCount = 12,
            skippedCount = 2,
            unconfirmedCount = 2,
            pendingCount = 4,
            lateCount = 1
        )

        assertEquals(16, summary.eligibleDoses)
        assertEquals(75, summary.adherencePercentage)
        assertEquals(14, summary.completedActionCount)
        assertEquals(75.0, summary.takenPercentage, 0.01)
        assertEquals(12.5, summary.skippedPercentage, 0.01)
        assertEquals(12.5, summary.unconfirmedPercentage, 0.01)
    }

    @Test
    fun testEndToEnd_MedicineBreakdownAdherence() {
        val medSummary = MedicineAdherenceSummary(
            medicineName = "Atorvastatin 20mg",
            totalScheduled = 10,
            takenCount = 8,
            skippedCount = 1,
            unconfirmedCount = 1,
            lateCount = 0
        )
        assertEquals(80, medSummary.adherencePercentage)
    }

    @Test
    fun testReminderScheduler_NextOccurrenceCalculationRules() {
        val now = 1729000000000L // Baseline timestamp
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        val activeSchedule = Schedule(
            id = 1L,
            name = "Daily Regimen",
            startDate = startOfDay - (2 * 24 * 60 * 60 * 1000L), // started 2 days ago
            endDate = startOfDay + (10 * 24 * 60 * 60 * 1000L), // ends in 10 days
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )

        // Morning slot: 08:00 AM = 480 minutes
        val morningSlot = ScheduleMedicineTime(
            id = 101L,
            scheduleMedicineId = 1L,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            minutesSinceMidnight = 480,
            isEnabled = true
        )

        // Scenario 1: Active schedule, enabled time -> returns valid occurrence timestamp
        val occurrence = ReminderScheduler.calculateNextOccurrence(activeSchedule, morningSlot, now)
        assertNotNull("Next occurrence must not be null for active schedule", occurrence)
        assertTrue(occurrence!! >= now)

        // Scenario 2: Notifications disabled on schedule -> returns null
        val noNotifSchedule = activeSchedule.copy(notificationsEnabled = false)
        assertNull(ReminderScheduler.calculateNextOccurrence(noNotifSchedule, morningSlot, now))

        // Scenario 3: Schedule completed -> returns null
        val completedSchedule = activeSchedule.copy(status = Schedule.STATUS_COMPLETED)
        assertNull(ReminderScheduler.calculateNextOccurrence(completedSchedule, morningSlot, now))

        // Scenario 4: Schedule discontinued -> returns null
        val discontinuedSchedule = activeSchedule.copy(status = Schedule.STATUS_DISCONTINUED)
        assertNull(ReminderScheduler.calculateNextOccurrence(discontinuedSchedule, morningSlot, now))

        // Scenario 5: Time slot disabled -> returns null
        val disabledSlot = morningSlot.copy(isEnabled = false)
        assertNull(ReminderScheduler.calculateNextOccurrence(activeSchedule, disabledSlot, now))

        // Scenario 6: Schedule already past end date -> returns null
        val expiredSchedule = activeSchedule.copy(endDate = startOfDay - (1 * 24 * 60 * 60 * 1000L))
        assertNull(ReminderScheduler.calculateNextOccurrence(expiredSchedule, morningSlot, now))
    }

    @Test
    fun testDoseAudit_LateThresholdAndStatusTransitions() {
        val scheduledAt = 1729000000000L

        // Taken on time (30 mins after scheduled time <= 60 mins)
        val onTime = scheduledAt + (30 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, onTime, DoseRecord.STATUS_TAKEN))

        // Taken late (61 mins after scheduled time > 60 mins)
        val late = scheduledAt + (61 * 60 * 1000L)
        assertTrue(DoseRecord.calculateIsLate(scheduledAt, late, DoseRecord.STATUS_TAKEN))

        // Skipped late (90 mins after scheduled time > 60 mins)
        val lateSkipped = scheduledAt + (90 * 60 * 1000L)
        assertTrue(DoseRecord.calculateIsLate(scheduledAt, lateSkipped, DoseRecord.STATUS_SKIPPED))

        // Unconfirmed or pending must NEVER be flagged as late
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, late, DoseRecord.STATUS_UNCONFIRMED))
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, late, DoseRecord.STATUS_PENDING))
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, null, DoseRecord.STATUS_TAKEN))
    }

    @Test
    fun testCaregiverIntegration_PersistenceAndIsolation() = runBlocking {
        val fakeDao = FakeCaregiverDao()
        val repo = CaregiverRepository(fakeDao)
        val viewModel = CaregiverFormViewModel(repo)

        // Test 1: Add caregiver
        viewModel.fullName = "Dr. Martha Jones"
        viewModel.relationship = "Nurse"
        viewModel.phone = "+1 (555) 765-4321"
        viewModel.email = "martha.jones@nhs.uk"
        viewModel.isEmergencyContact = true

        val saveRes = viewModel.saveCaregiver()
        assertTrue(saveRes.isSuccess)
        val saved = repo.getCaregiverOnce()
        assertNotNull(saved)
        assertEquals("Dr. Martha Jones", saved?.fullName)
        assertEquals("Nurse", saved?.relationship)

        // Test 2: Edit caregiver retains single record
        repo.saveCaregiver(
            fullName = "Dr. Martha Jones, MD",
            relationship = "Chief Medical Officer",
            phone = "555-765-9999",
            email = "martha@clinic.org",
            isEmergencyContact = false
        )
        val updated = repo.getCaregiverOnce()
        assertEquals(saved!!.id, updated!!.id)
        assertEquals("Dr. Martha Jones, MD", updated.fullName)
        assertFalse(updated.isEmergencyContact)

        // Test 3: Deletion isolates caregiver table without cascade
        repo.deleteCaregiver()
        assertNull(repo.getCaregiverOnce())
        assertFalse(repo.hasCaregiver())
    }

    @Test
    fun testDateRangeTypeLabels() {
        assertEquals("Last 7 Days", DateRangeType.LAST_7_DAYS.label)
        assertEquals("Last 30 Days", DateRangeType.LAST_30_DAYS.label)
        assertEquals("Entire Schedule Period", DateRangeType.ENTIRE_PERIOD.label)
        assertEquals("Custom", DateRangeType.CUSTOM.label)
    }
}
