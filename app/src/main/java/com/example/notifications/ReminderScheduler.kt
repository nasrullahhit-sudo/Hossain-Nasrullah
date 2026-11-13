package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.ChallengeReminder
import java.util.Calendar

object ReminderScheduler {
  fun syncReminders(context: Context, reminders: List<ChallengeReminder>) {
    ReminderReceiver.ensureNotificationChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    reminders.forEach { reminder ->
      val requestCode = reminder.id.hashCode()
      val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
        putExtra(ReminderReceiver.EXTRA_BODY, "${reminder.targetText} • Discipline is Power!")
        putExtra(ReminderReceiver.EXTRA_NOTIF_ID, requestCode)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      if (reminder.enabled) {
        val triggerAt = Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, reminder.hour.toInt().coerceIn(0, 23))
          set(Calendar.MINUTE, reminder.minute.toInt().coerceIn(0, 59))
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
          if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_MONTH, 1)
          }
        }.timeInMillis

        alarmManager.setInexactRepeating(
          AlarmManager.RTC_WAKEUP,
          triggerAt,
          AlarmManager.INTERVAL_DAY,
          pendingIntent,
        )
      } else {
        alarmManager.cancel(pendingIntent)
      }
    }
  }
}
