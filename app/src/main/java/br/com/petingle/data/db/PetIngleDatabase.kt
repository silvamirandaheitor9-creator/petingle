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
import br.com.petingle.data.db.entity.UserMetadata
import br.com.petingle.data.db.dao.UserMetadataDao

@Database(
    entities = [Pet::class, Reminder::class, DiaryEntry::class, HealthRecord::class, UserMetadata::class],
    version = 3, // v3: adicionada tabela user_metadata para backup unificado
    exportSchema = true,
)
abstract class PetIngleDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun reminderDao(): ReminderDao
    abstract fun diaryDao(): DiaryDao
    abstract fun healthRecordDao(): HealthRecordDao
    abstract fun userMetadataDao(): UserMetadataDao
}
