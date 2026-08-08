package br.com.petingle.data.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import br.com.petingle.MainActivity
import br.com.petingle.R
import java.io.File

/**
 * Recebe alarmes agendados pelo [ReminderScheduler] e exibe a notificação rica:
 *  - Título e corpo contextuais e variados por categoria
 *  - Som padrão do dispositivo habilitado
 *  - Foto do pet como ícone grande (ou ícone de categoria como fallback)
 *  - Botões de ação "Concluir" e "Adiar 1h"
 *  - Vibração personalizada
 *  - Agrupamento nativo por GROUP_KEY
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId   = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val petName      = intent.getStringExtra(EXTRA_PET_NAME) ?: ""
        val petPhotoPath = intent.getStringExtra(EXTRA_PET_PHOTO_PATH) ?: ""
        val category     = intent.getStringExtra(EXTRA_CATEGORY) ?: "personalizado"
        val title        = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val notifId      = intent.getIntExtra(EXTRA_NOTIF_ID, reminderId.toInt().coerceAtLeast(1))

        val notifTitle = professionalTitle(category, petName, title)
        val notifBody  = professionalBody(category, petName)

        // ── Intent principal: abre o app ──────────────────────────────────────
        val mainPi = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Ação "Concluir" ───────────────────────────────────────────────────
        val completePi = PendingIntent.getBroadcast(
            context,
            notifId + 10_000,
            Intent(context, CompleteReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_NOTIF_ID, notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Ação "Adiar 1h" ───────────────────────────────────────────────────
        val snoozePi = PendingIntent.getBroadcast(
            context,
            notifId + 20_000,
            Intent(context, SnoozeReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID,    reminderId)
                putExtra(EXTRA_PET_NAME,       petName)
                putExtra(EXTRA_PET_PHOTO_PATH, petPhotoPath)
                putExtra(EXTRA_CATEGORY,       category)
                putExtra(EXTRA_TITLE,          title)
                putExtra(EXTRA_NOTIF_ID,       notifId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // ── Ícone grande: foto do pet → ícone de categoria → null ─────────────
        val largeBitmap: Bitmap? = loadLargeIcon(context, petPhotoPath, category)

        // Som padrão do dispositivo
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // ── Constrói a notificação ────────────────────────────────────────────
        val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setAutoCancel(true)
            .setContentIntent(mainPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(NotificationChannels.VIBRATION_PATTERN)
            .setSound(soundUri)
            .setGroup(GROUP_KEY)
            .addAction(R.drawable.ic_notification, "Concluir", completePi)
            .addAction(R.drawable.ic_notification, "Adiar 1h", snoozePi)

        if (largeBitmap != null) builder.setLargeIcon(largeBitmap)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, builder.build())

        // ── Notificação de agrupamento (summary) ──────────────────────────────
        val summaryBuilder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PetIngle")
            .setContentText("Lembretes pendentes dos seus pets")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText("PetIngle — Lembretes")
            )
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        manager.notify(SUMMARY_NOTIF_ID, summaryBuilder.build())
    }

    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val GROUP_KEY        = "br.com.petingle.REMINDERS_GROUP"
        const val SUMMARY_NOTIF_ID = 999_999

        const val EXTRA_REMINDER_ID    = "extra_reminder_id"
        const val EXTRA_PET_NAME       = "extra_pet_name"
        const val EXTRA_PET_PHOTO_PATH = "extra_pet_photo_path"
        const val EXTRA_CATEGORY       = "extra_category"
        const val EXTRA_TITLE          = "extra_title"
        const val EXTRA_NOTIF_ID       = "extra_notif_id"

        /**
         * Gera título profissional baseado na categoria e no nome do pet.
         * Se o usuário digitou um título personalizado (não vazio e diferente
         * do padrão da categoria), usa o título dele — apenas capitalizado.
         */
        fun professionalTitle(category: String, petName: String, userTitle: String = ""): String {
            val name = petName.trim().ifBlank { null }

            // Título padrão rico por categoria
            val categoryTitle = when (category) {
                "vacina"      -> if (name != null) "Vacina do $name" else "Hora da vacina"
                "consulta"    -> if (name != null) "Consulta do $name" else "Consulta veterinária"
                "banho"       -> if (name != null) "Banho e tosa do $name" else "Banho e tosa"
                "medicacao"   -> if (name != null) "Medicação do $name" else "Hora do remédio"
                "alimentacao" -> if (name != null) "$name está com fome!" else "Hora da refeição"
                "vermifugo"   -> if (name != null) "Vermífugo do $name" else "Hora do vermífugo"
                else          -> if (name != null) "Lembrete para $name" else "Lembrete PetIngle"
            }

            // Se o usuário digitou um título personalizado, usa o dele
            val trimmed = userTitle.trim()
            return if (trimmed.isNotBlank() && trimmed.lowercase() != category) {
                trimmed.replaceFirstChar { it.uppercaseChar() }
            } else {
                categoryTitle
            }
        }

        /**
         * Gera corpo de notificação profissional, cuidadoso e variado por categoria.
         * A variação é feita com base no hashCode do petName para ser determinística
         * por pet (o mesmo pet sempre recebe a mesma variante, mas pets diferentes
         * recebem mensagens diferentes — evita monotonia sem ser aleatório a cada push).
         */
        fun professionalBody(category: String, petName: String): String {
            val name = petName.trim().ifBlank { "seu pet" }
            val variant = (name.hashCode() and 0x7FFFFFFF) % 3

            return when (category) {
                "vacina" -> listOf(
                    "Manter a vacinação em dia é o maior presente que você pode dar a $name. Vamos lá?",
                    "Prevenção é saúde! Confira a vacina de $name e mantenha a proteção em dia.",
                    "Uma vacina hoje, muita saúde amanhã. $name conta com você!",
                )[variant]

                "consulta" -> listOf(
                    "O veterinário está esperando por $name. Cuide bem do seu companheiro!",
                    "Hora do check-up! Leve $name à consulta e garanta que está tudo bem.",
                    "Não esqueça: consulta marcada para $name. Saúde em dia, pet feliz!",
                )[variant]

                "banho" -> listOf(
                    "$name merece um banho quentinho e muita capricho. Hora de brilhar!",
                    "Banho e tosa fazem toda a diferença. $name vai adorar ficar cheiroso!",
                    "Deixa $name limpinho e feliz — é hora do banho e tosa!",
                )[variant]

                "medicacao" -> listOf(
                    "Não pule a dose de $name! Medicação em dia é saúde garantida.",
                    "Hora do remédio de $name. Capricho e cuidado em cada dose.",
                    "$name precisa da medicação agora. Você não esqueceu, né?",
                )[variant]

                "alimentacao" -> listOf(
                    "$name está de olho no potinho. Hora da refeição!",
                    "Barriga cheia, pet feliz! Não deixe $name esperando pela comida.",
                    "Hora de alimentar $name. Ele(a) já está contando os segundos!",
                )[variant]

                "vermifugo" -> listOf(
                    "Vermífugo previne parasitas e mantém $name saudável. Não adie!",
                    "Uma dose de vermífugo agora significa meses de proteção para $name.",
                    "Cuide de $name por dentro e por fora — hora do vermífugo!",
                )[variant]

                else -> listOf(
                    "Você criou um lembrete especial para $name. Hora de cumpri-lo!",
                    "$name conta com o seu cuidado. Não esqueça este lembrete!",
                    "Lembrete ativo para $name. Cada detalhe importa quando o assunto é cuidado!",
                )[variant]
            }
        }

        /**
         * Tenta carregar a foto do pet do sistema de arquivos; se não encontrar,
         * decodifica o PNG de categoria como fallback.
         */
        fun loadLargeIcon(context: Context, petPhotoPath: String, category: String): Bitmap? {
            if (petPhotoPath.isNotBlank()) {
                val file = File(petPhotoPath)
                if (file.exists()) {
                    return BitmapFactory.decodeFile(petPhotoPath)
                }
            }
            return try {
                BitmapFactory.decodeResource(context.resources, categoryIconRes(category))
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Mapeia categoria para o drawable de ícone correspondente.
         */
        fun categoryIconRes(category: String): Int = when (category) {
            "vacina"      -> R.drawable.icone_vacina
            "consulta"    -> R.drawable.icone_consulta
            "banho"       -> R.drawable.icone_banho
            "medicacao"   -> R.drawable.icone_medicacao
            "alimentacao" -> R.drawable.icone_alimentacao
            "vermifugo"   -> R.drawable.icone_vermifugo
            else          -> R.drawable.icone_personalizado
        }
    }
}
