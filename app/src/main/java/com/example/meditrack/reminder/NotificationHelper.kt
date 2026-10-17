package com.example.meditrack.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.meditrack.MainActivity
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.DoseRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID_REMINDERS = "meditrack_medication_reminders"
    private const val CHANNEL_NAME = "Medication Reminders"
    private const val CHANNEL_DESC = "Scheduled reminders to take or log your prescribed medications."

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(
        context: Context,
        dose: DoseRecord,
        scheduleName: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels(context)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(dose.scheduledAt))

        // Content intent: open MainActivity navigating to ScheduleDoseHistoryFragment
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to_history", true)
            putExtra("scheduleId", dose.scheduleId)
            putExtra("doseId", dose.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            ("content_${dose.id}").hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Taken
        val takenIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_TAKEN
            putExtra(ReminderActionReceiver.EXTRA_DOSE_ID, dose.id)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, dose.id.toInt())
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_NAME, dose.medicineName)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            ("taken_${dose.id}").hashCode(),
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Skipped
        val skippedIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SKIPPED
            putExtra(ReminderActionReceiver.EXTRA_DOSE_ID, dose.id)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, dose.id.toInt())
            putExtra(ReminderActionReceiver.EXTRA_MEDICINE_NAME, dose.medicineName)
        }
        val skippedPendingIntent = PendingIntent.getBroadcast(
            context,
            ("skipped_${dose.id}").hashCode(),
            skippedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Public Lockscreen Notification (Strict Privacy: No sensitive medicine name)
        val publicNotification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Scheduled Medication Reminder • Due at $formattedTime")
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()

        // Private / Unlocked Notification
        val foodInstructionText = when (dose.foodInstruction) {
            "BEFORE_FOOD" -> "Before Food"
            "WITH_FOOD" -> "With Food"
            "AFTER_FOOD" -> "After Food"
            "NO_RESTRICTION" -> "No Meal Restriction"
            else -> dose.foodInstruction
        }
        val instructionDetail = if (foodInstructionText.isNotEmpty()) " • $foodInstructionText" else ""

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_medication)
            .setContentTitle("Dose Reminder • $formattedTime")
            .setContentText("${dose.medicineName} ($scheduleName$instructionDetail)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to take ${dose.medicineName} for $scheduleName.$instructionDetail\nTap to view dose history or log your response below.")
            )
            .setColor(ContextCompat.getColor(context, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_check, "Taken", takenPendingIntent)
            .addAction(R.drawable.ic_cancel, "Skipped", skippedPendingIntent)
            .build()

        notificationManager.notify(dose.id.toInt(), notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
