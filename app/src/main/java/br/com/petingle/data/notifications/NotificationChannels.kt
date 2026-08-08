package br.com.petingle.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

object NotificationChannels {

    /**
     * Canal v2 — com som habilitado.
     * O sufixo _v2 força o Android a criar um novo canal,
     * pois canais já existentes não podem ser atualizados (som, vibração etc.)
     */
    const val REMINDERS_CHANNEL_ID   = "petingle_reminders_v2"
    const val REMINDERS_CHANNEL_NAME = "Lembretes de saúde"

    // Padrão de vibração característico: pausa / 3 pulsos rápidos + encerramento longo
    val VIBRATION_PATTERN = longArrayOf(0L, 300L, 120L, 300L, 120L, 600L)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Som padrão de notificação do dispositivo
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                REMINDERS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Lembretes de vacinas, consultas e cuidados do seu pet"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setShowBadge(true)
                setSound(soundUri, audioAttributes)
                enableLights(true)
                lightColor = 0xFFFF6B2C.toInt() // laranja PetIngle
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Remove canal antigo (sem som) se ainda existir
            manager.deleteNotificationChannel("petingle_reminders")

            manager.createNotificationChannel(channel)
        }
    }
}
