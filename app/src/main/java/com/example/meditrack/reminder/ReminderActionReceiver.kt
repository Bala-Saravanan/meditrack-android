package com.example.meditrack.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meditrack.MediTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.example.meditrack.action.DOSE_TAKEN"
        const val ACTION_SKIPPED = "com.example.meditrack.action.DOSE_SKIPPED"
        const val EXTRA_DOSE_ID = "extra_dose_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_MEDICINE_NAME = "extra_medicine_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        if (doseId <= 0) return

        // Immediately dismiss the notification
        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as MediTrackApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TAKEN -> {
                        app.doseRepository.recordDoseTaken(doseId)
                    }
                    ACTION_SKIPPED -> {
                        app.doseRepository.recordDoseSkipped(doseId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
