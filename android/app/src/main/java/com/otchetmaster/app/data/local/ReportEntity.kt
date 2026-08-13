package com.otchetmaster.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "reports",
    foreignKeys = [ForeignKey(
        entity = JobEntity::class,
        parentColumns = ["id"],
        childColumns = ["jobId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("jobId")],
)
data class ReportEntity(
    @androidx.room.PrimaryKey val id: String,
    val jobId: String,
    val workPerformed: String,
    val materialsJson: String? = null,
    val notes: String? = null,
    val source: String = "ai",
    val pdfLocalPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "LOCAL",
)
