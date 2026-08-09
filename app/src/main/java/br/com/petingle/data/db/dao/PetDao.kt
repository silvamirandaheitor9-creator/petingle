package br.com.petingle.data.db.dao

import androidx.room.*
import br.com.petingle.data.db.entity.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY createdAt DESC")
    fun getAllPets(): Flow<List<Pet>>

    @Query("SELECT COUNT(*) FROM pets")
    fun getPetCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pets")
    suspend fun getPetCountOnce(): Int

    @Query("SELECT * FROM pets WHERE id = :id")
    fun getPetById(id: Long): Flow<Pet?>

    @Query("SELECT * FROM pets WHERE id = :id")
    suspend fun getPetByIdOnce(id: Long): Pet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: Pet): Long

    @Update
    suspend fun updatePet(pet: Pet)

    @Delete
    suspend fun deletePet(pet: Pet)

    @Query("DELETE FROM pets")
    suspend fun deleteAllPets()
}
