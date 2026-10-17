package com.example.meditrack.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime

data class ScheduleMedicineWithTimes(
    @Embedded val scheduleMedicine: ScheduleMedicine,
    @Relation(
        parentColumn = "medicineId",
        entityColumn = "id"
    )
    val medicine: Medicine,
    @Relation(
        parentColumn = "id",
        entityColumn = "scheduleMedicineId"
    )
    val times: List<ScheduleMedicineTime>
)

data class FullScheduleDetails(
    @Embedded val schedule: Schedule,
    @Relation(
        entity = ScheduleMedicine::class,
        parentColumn = "id",
        entityColumn = "scheduleId"
    )
    val medicinesWithTimes: List<ScheduleMedicineWithTimes>
)
