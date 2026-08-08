package br.com.petingle.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Shared entity for Vacinas, Consultas, Peso, Alimentação, Medicamentos
@Entity(
    tableName = "health_records",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE,
    )],
    // Índice em petId evita full-table-scan nas queries getRecordsByPetAndType
    indices = [Index("petId")],
)
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val type: String,         // vaccine | consultation | weight | feeding | medication
    // Vacina
    val vaccineName: String = "",
    val vaccineLot: String = "",
    val nextDoseDate: String = "",
    // Consulta
    val consultationReason: String = "",
    val diagnosis: String = "",
    val vetInstructions: String = "",
    // Peso
    val weightKg: Double = 0.0,
    // Alimentação
    val feedingType: String = "",
    val feedingAmountGrams: Double = 0.0,
    val feedingSchedule: String = "",
    // Medicamento
    val medicationName: String = "",
    val medicationDosage: String = "",
    val medicationFrequency: String = "",
    val medicationDurationDays: Int = 0,
    // Shared
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
)
