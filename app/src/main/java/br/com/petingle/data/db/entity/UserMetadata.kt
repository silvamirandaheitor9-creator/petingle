package br.com.petingle.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_metadata")
data class UserMetadata(
    @PrimaryKey
    val key: String,
    val value: String
)
