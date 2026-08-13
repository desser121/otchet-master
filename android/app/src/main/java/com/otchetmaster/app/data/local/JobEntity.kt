package com.otchetmaster.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "jobs", indices = [Index("createdAt")])
data class JobEntity(
    @PrimaryKey val id: String,
    val date: String,
    val address: String,
    val clientName: String,
    val clientPhone: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "LOCAL",
)
