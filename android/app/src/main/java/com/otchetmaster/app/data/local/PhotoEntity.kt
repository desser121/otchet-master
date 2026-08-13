package com.otchetmaster.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["jobId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("jobId")],
)
data class PhotoEntity(
    @androidx.room.PrimaryKey val id: String,
    val jobId: String,
    val localPath: String,
    val cloudUrl: String? = null,
    val position: Int,
    val createdAt: Long,
    val syncStatus: String = "LOCAL",
)
