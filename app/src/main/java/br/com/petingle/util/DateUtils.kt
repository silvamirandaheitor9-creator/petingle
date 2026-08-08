package br.com.petingle.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Utilitários de data para seletores Material3 DatePicker.
 *
 * ## Problema
 * O `DatePickerDialog` do Material3 **sempre** retorna millis referentes à
 * meia-noite em UTC (ex.: 2025-07-15 00:00:00 UTC).  Quando esses millis são
 * formatados diretamente com `SimpleDateFormat` sem especificar fuso, a JVM
 * usa o fuso local do dispositivo.  Em fusos negativos (ex.: Brasil UTC-3),
 * a meia-noite UTC corresponde a 21h do dia anterior, e o resultado exibido
 * fica um dia atrás do escolhido.
 *
 * ## Solução
 * - [utcMillisToDisplayDate]: lê Y/M/D **no fuso UTC** e formata diretamente,
 *   evitando qualquer conversão de timezone.
 * - [localMillisToUtcMidnight]: converte um millis em fuso local para a
 *   meia-noite UTC do mesmo dia, para inicializar o `DatePickerState`
 *   corretamente (o picker espera UTC midnight).
 * - [utcMillisToLocalPreservingTime]: aplica o dia escolhido no picker sobre
 *   um millis local existente, preservando hora e minuto (usado em Lembretes).
 *
 * Use estas funções em **todo** seletor de data do app — Vacinas,
 * Medicamentos, Consultas, Peso, Alimentação, Lembretes, Novo Pet, etc. —
 * para evitar que o bug de "dia anterior" reapareça em telas futuras.
 *
 * ### Fluxo para datas salvas como String ISO ("yyyy-MM-dd")
 * - Inicializar o picker:    `isoDateToUtcMidnightMillis(isoString)`
 * - Salvar ao confirmar:     `utcMillisToIsoDate(selectedDateMillis)`
 * - Exibir no campo:         `utcMillisToDisplayDate(isoDateToUtcMidnightMillis(iso) ?: 0L)`
 */
object DateUtils {

    /**
     * Converte [utcMillis] (retornados pelo `DatePicker`) para a string
     * "dd/MM/yyyy" lendo os campos de data **no fuso UTC**.
     *
     * Nunca passe esses millis para `SimpleDateFormat` sem timezone: a JVM
     * usaria o fuso local e mostraria o dia errado em fusos negativos.
     */
    fun utcMillisToDisplayDate(utcMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return String.format(
            Locale("pt", "BR"),
            "%02d/%02d/%04d",
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR),
        )
    }

    /**
     * Converte [localMillis] (fuso local do dispositivo) para a meia-noite
     * UTC do mesmo dia de calendário, para ser usado em
     * `rememberDatePickerState(initialSelectedDateMillis = ...)`.
     *
     * O `DatePicker` Material3 espera UTC midnight; se passarmos millis locais
     * diretamente, o picker pode exibir um dia diferente do atual.
     */
    fun localMillisToUtcMidnight(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR,         local.get(Calendar.YEAR))
            set(Calendar.MONTH,        local.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY,  0)
            set(Calendar.MINUTE,       0)
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
        }.timeInMillis
    }

    /**
     * Converte [utcMillis] (retornados pelo `DatePicker`) para a string ISO
     * "yyyy-MM-dd" lendo os campos de data **no fuso UTC**.
     *
     * Use no `confirmButton` do `DatePickerDialog` para salvar a data escolhida
     * sem distorção de fuso.  Nunca passe esses millis para `SimpleDateFormat`
     * sem timezone — o resultado seria o dia errado em fusos negativos.
     */
    fun utcMillisToIsoDate(utcMillis: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /**
     * Converte uma string ISO "yyyy-MM-dd" para os millis da meia-noite UTC
     * desse dia de calendário, para uso em
     * `rememberDatePickerState(initialSelectedDateMillis = ...)`.
     *
     * A conversão interpreta o dia/mês/ano da string **como datas de calendário
     * em UTC** (sem ajuste de fuso local), o que garante que o picker exiba
     * exatamente o dia armazenado.  Retorna `null` se a string for vazia ou
     * inválida.
     */
    fun isoDateToUtcMidnightMillis(isoDate: String): Long? {
        if (isoDate.isBlank()) return null
        val parts = isoDate.split("-")
        if (parts.size != 3) return null
        val year  = parts[0].toIntOrNull() ?: return null
        val month = (parts[1].toIntOrNull() ?: return null) - 1  // Calendar: 0-based
        val day   = parts[2].toIntOrNull() ?: return null
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR,         year)
            set(Calendar.MONTH,        month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY,  0)
            set(Calendar.MINUTE,       0)
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
        }.timeInMillis
    }

    /**
     * Aplica o dia/mês/ano de [selectedUtcMillis] (retornado pelo `DatePicker`)
     * sobre [existingLocalMillis], preservando hora e minuto originais.
     *
     * Use esta função no bloco `confirmButton` do `DatePickerDialog` da tela
     * de Lembretes (e qualquer outra tela que precise preservar o horário).
     */
    fun utcMillisToLocalPreservingTime(selectedUtcMillis: Long, existingLocalMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selectedUtcMillis
        }
        return Calendar.getInstance().apply {
            timeInMillis = existingLocalMillis
            set(Calendar.YEAR,         utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH,        utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
        }.timeInMillis
    }
}
