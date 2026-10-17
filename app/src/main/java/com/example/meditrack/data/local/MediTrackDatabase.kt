package com.example.meditrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.meditrack.data.local.dao.CaregiverDao
import com.example.meditrack.data.local.dao.DoseRecordDao
import com.example.meditrack.data.local.dao.MedicineDao
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.dao.UserDao
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.entity.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        UserProfile::class,
        Medicine::class,
        Schedule::class,
        ScheduleMedicine::class,
        ScheduleMedicineTime::class,
        DoseRecord::class,
        Caregiver::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MediTrackDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseRecordDao(): DoseRecordDao
    abstract fun caregiverDao(): CaregiverDao

    companion object {
        @Volatile
        private var INSTANCE: MediTrackDatabase? = null

        fun getDatabase(context: Context): MediTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediTrackDatabase::class.java,
                    "meditrack_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
