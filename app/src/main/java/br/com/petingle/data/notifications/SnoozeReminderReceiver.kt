package br.com.petingle.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import br.com.petingle.data.db.PetIngleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ação "Adiar 1h" da notificação de lembrete.
 * Cancela a notificação atual, atualiza o horário no banco (+1h)
 * e agenda um novo alarme.
 */
class SnoozeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId   = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, -1L)
        val petName      = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_PET_NAME) ?: ""
        val petPhotoPath = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_PET_PHOTO_PATH) ?: ""
        val notifId      = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_NOTIF_ID, -1)

        // Cancela a notificação imediatamente
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId >= 0) manager.cancel(notifId)

        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(context, PetIngleDatabase::class.java, "petingle.db")
                    .fallbackToDestructiveMigration()
                    .build()

                val reminder = db.reminderDao().getReminderById(reminderId)
                if (reminder != null) {
                    val snoozedTime = System.currentTimeMillis() + 60L * 60L * 1_000L
                    val snoozed = reminder.copy(dateTimeMillis = snoozedTime)
                    db.reminderDao().updateReminder(snoozed)

                    // Agenda o novo alarme diretamente (sem Hilt — receiver fora do graph)
                    ReminderScheduler(context).schedule(snoozed, petName, petPhotoPath)
                }
                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
