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
    val status: String = "IN_PROGRESS",
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "LOCAL",
)

enum class JobStatus(val label: String) {
    IN_PROGRESS("В работе"),
    DONE("Готово"),
    SENT("Отправлен клиенту");

    companion object {
        fun fromName(name: String): JobStatus =
            entries.firstOrNull { it.name == name } ?: IN_PROGRESS
    }
}
