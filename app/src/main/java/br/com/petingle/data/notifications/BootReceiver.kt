package br.com.petingle.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import br.com.petingle.data.db.PetIngleDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reagenda todos os lembretes pendentes após reinicialização do dispositivo.
 * O AlarmManager não persiste alarmes entre reboots — este receiver corrige isso.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        /** Lembretes perdidos até 2h antes do boot ainda disparam (quase imediatamente). */
        private const val GRACE_MS      = 2L * 60 * 60 * 1_000
        /** Atraso do disparo imediato após o boot — dá tempo ao sistema de estabilizar. */
        private const val FIRE_DELAY_MS = 5_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(context, PetIngleDatabase::class.java, "petingle.db")
                    .fallbackToDestructiveMigration()
                    .build()

                val now       = System.currentTimeMillis()
                val pending   = db.reminderDao().getAllPendingRemindersOnce()
                val scheduler = ReminderScheduler(context)

                for (reminder in pending) {
                    // Lembrete futuro → reagenda normalmente.
                    // Perdido durante o boot (até 2h atrás) → dispara em 5s.
                    // Mais antigo que 2h → descarta (sem sentido notificar depois).
                    val targetMs = when {
                        reminder.dateTimeMillis > now ->
                            reminder.dateTimeMillis
                        now - reminder.dateTimeMillis <= GRACE_MS ->
                            now + FIRE_DELAY_MS
                        else -> continue
                    }

                    val pet = db.petDao().getPetByIdOnce(reminder.petId)
                    scheduler.schedule(
                        reminder     = reminder.copy(dateTimeMillis = targetMs),
                        petName      = pet?.name ?: "",
                        petPhotoPath = pet?.photoPath ?: "",
                    )
                }
                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
