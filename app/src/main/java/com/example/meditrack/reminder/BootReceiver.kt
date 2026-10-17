package com.example.meditrack.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meditrack.MediTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            val app = context.applicationContext as MediTrackApplication

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ReminderScheduler.scheduleAllActiveReminders(context, app.database.scheduleDao())
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
