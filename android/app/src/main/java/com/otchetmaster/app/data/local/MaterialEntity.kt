package com.otchetmaster.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "materials",
    foreignKeys = [ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["jobId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("jobId")],
)
data class MaterialEntity(
    @androidx.room.PrimaryKey val id: String,
    val jobId: String,
    val name: String,
    val quantity: String? = null,
    val position: Int,
)
