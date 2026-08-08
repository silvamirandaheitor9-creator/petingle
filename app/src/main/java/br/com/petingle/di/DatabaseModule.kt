package br.com.petingle.di

import android.content.Context
import androidx.room.Room
import br.com.petingle.data.db.PetIngleDatabase
import br.com.petingle.data.db.dao.DiaryDao
import br.com.petingle.data.db.dao.HealthRecordDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PetIngleDatabase =
        Room.databaseBuilder(context, PetIngleDatabase::class.java, "petingle.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePetDao(db: PetIngleDatabase): PetDao = db.petDao()
    @Provides fun provideReminderDao(db: PetIngleDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideDiaryDao(db: PetIngleDatabase): DiaryDao = db.diaryDao()
    @Provides fun provideHealthRecordDao(db: PetIngleDatabase): HealthRecordDao = db.healthRecordDao()
}
