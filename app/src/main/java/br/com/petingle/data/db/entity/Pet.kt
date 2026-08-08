package br.com.petingle.data.db.entity

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val species: String,          // cachorro, gato, pássaro, peixe, réptil, roedor, outro
    val breed: String = "",
    val sex: String = "",         // Macho / Fêmea
    val isCastrated: Boolean = false,
    val birthDate: String = "",   // ISO-8601 date string
    val approximateAge: String = "",
    val weightKg: Double = 0.0,
    val bloodType: String = "",
    val allergies: String = "",
    val chronicConditions: String = "",
    val microchip: String = "",
    val notes: String = "",
    val vetName: String = "",
    val vetPhone: String = "",
    val photoPath: String = "",   // internal storage path
    val createdAt: Long = System.currentTimeMillis(),
)
