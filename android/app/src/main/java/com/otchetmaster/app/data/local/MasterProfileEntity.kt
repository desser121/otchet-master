package com.otchetmaster.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_profile")
data class MasterProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val phone: String,
    val city: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
