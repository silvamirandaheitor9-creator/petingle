package br.com.petingle.data.db.dao

import androidx.room.*
import br.com.petingle.data.db.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY dateMillis DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE petId = :petId ORDER BY dateMillis DESC")
    fun getEntriesByPet(petId: Long): Flow<List<DiaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Update
    suspend fun updateEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE id = :id LIMIT 1")
    fun getEntryById(id: Long): Flow<DiaryEntry?>
}
