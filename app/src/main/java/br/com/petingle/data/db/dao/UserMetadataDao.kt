package br.com.petingle.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.petingle.data.db.entity.UserMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMetadataDao {
    @Query("SELECT value FROM user_metadata WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM user_metadata WHERE `key` = :key")
    fun observeValue(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: UserMetadata)

    @Query("DELETE FROM user_metadata WHERE `key` = :key")
    suspend fun delete(key: String)
}
