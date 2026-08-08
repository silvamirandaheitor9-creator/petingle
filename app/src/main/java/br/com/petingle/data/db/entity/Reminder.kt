package br.com.petingle.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Stable
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE,
    )],
    // Índice em petId para filtros e joins com pets sem full-table-scan
    indices = [Index("petId")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val title: String,
    val category: String,    // vacina, consulta, banho, medicação, alimentação, vermífugo, personalizado
    val dateTimeMillis: Long,
    val recurrence: String = "none", // none, daily, weekly, monthly
    val isCompleted: Boolean = false,
    val notes: String = "",
    val notificationId: Int = 0,
)
