package br.com.petingle.data.db.dao

import androidx.room.*
import br.com.petingle.data.db.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dateTimeMillis ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE petId = :petId ORDER BY dateTimeMillis ASC")
    fun getRemindersByPet(petId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dateTimeMillis ASC")
    fun getPendingReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE dateTimeMillis > :now AND isCompleted = 0 ORDER BY dateTimeMillis ASC LIMIT 1")
    fun getNextReminder(now: Long): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    suspend fun getAllPendingRemindersOnce(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
