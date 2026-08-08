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
 * Ação "Concluir" da notificação de lembrete.
 * Marca o lembrete como concluído no banco e cancela a notificação.
 */
class CompleteReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, -1L)
        val notifId    = intent.getIntExtra(ReminderBroadcastReceiver.EXTRA_NOTIF_ID, -1)

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
                    db.reminderDao().updateReminder(reminder.copy(isCompleted = true))
                }
                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
