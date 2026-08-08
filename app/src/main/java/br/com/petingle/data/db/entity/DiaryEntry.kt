package br.com.petingle.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Stable
@Entity(
    tableName = "diary_entries",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE,
    )],
    // Índice em petId para filtrar entradas por pet sem full-table-scan
    indices = [Index("petId")],
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val photoPath: String,
    val caption: String = "",   // max 140 chars
    val dateMillis: Long = System.currentTimeMillis(),
)
