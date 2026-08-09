package br.com.petingle.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.petingle.data.db.dao.DiaryDao
import br.com.petingle.data.db.dao.HealthRecordDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.dao.ReminderDao
import br.com.petingle.data.db.entity.DiaryEntry
import br.com.petingle.data.db.entity.HealthRecord
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.data.db.entity.Reminder

@Database(
    entities = [Pet::class, Reminder::class, DiaryEntry::class, HealthRecord::class],
    version = 2, // v2: adicionados índices em petId (health_records, diary_entries, reminders)
    exportSchema = true,
)
abstract class PetIngleDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun diaryDao(): DiaryDao
    abstract fun healthRecordDao(): HealthRecordDao
}
