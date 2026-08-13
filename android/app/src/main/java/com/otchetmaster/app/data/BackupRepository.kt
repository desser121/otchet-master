package com.otchetmaster.app.data

import com.otchetmaster.app.data.local.JobDao
import com.otchetmaster.app.data.local.MaterialDao
import com.otchetmaster.app.data.local.PhotoDao
import com.otchetmaster.app.data.local.ReportDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class BackupData(
    @SerialName("version") val version: Int = 1,
    @SerialName("exported_at") val exportedAt: String = "",
    @SerialName("jobs") val jobs: List<BackupJob> = emptyList(),
    @SerialName("photos") val photos: List<BackupPhoto> = emptyList(),
    @SerialName("materials") val materials: List<BackupMaterial> = emptyList(),
    @SerialName("reports") val reports: List<BackupReport> = emptyList(),
)

@Serializable
data class BackupJob(
    @SerialName("id") val id: String,
    @SerialName("date") val date: String,
    @SerialName("address") val address: String,
    @SerialName("client_name") val clientName: String,
    @SerialName("client_phone") val clientPhone: String,
    @SerialName("status") val status: String,
    @SerialName("project") val project: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

@Serializable
data class BackupPhoto(
    @SerialName("id") val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("local_path") val localPath: String,
    @SerialName("position") val position: Int,
    @SerialName("caption") val caption: String,
    @SerialName("created_at") val createdAt: Long,
)

@Serializable
data class BackupMaterial(
    @SerialName("id") val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("name") val name: String,
    @SerialName("quantity") val quantity: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("position") val position: Int,
)

@Serializable
data class BackupReport(
    @SerialName("id") val id: String,
    @SerialName("job_id") val jobId: String,
    @SerialName("work_performed") val workPerformed: String,
    @SerialName("materials_json") val materialsJson: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("source") val source: String,
    @SerialName("work_price") val workPrice: Double? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
)

class BackupRepository(
    private val jobDao: JobDao,
    private val photoDao: PhotoDao,
    private val materialDao: MaterialDao,
    private val reportDao: ReportDao,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Экспортирует метаданные (без самих файлов фото) в JSON-файл. */
    suspend fun exportToFile(file: File) = withContext(Dispatchers.IO) {
        val data = BackupData(
            exportedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date()),
            jobs = jobDao.getAll().map {
                BackupJob(
                    id = it.id, date = it.date, address = it.address,
                    clientName = it.clientName, clientPhone = it.clientPhone,
                    status = it.status, project = it.project,
                    createdAt = it.createdAt, updatedAt = it.updatedAt,
                )
            },
            photos = photoDao.getAll().map {
                BackupPhoto(
                    id = it.id, jobId = it.jobId, localPath = it.localPath,
                    position = it.position, caption = it.caption, createdAt = it.createdAt,
                )
            },
            materials = materialDao.getAll().map {
                BackupMaterial(
                    id = it.id, jobId = it.jobId, name = it.name,
                    quantity = it.quantity, price = it.price, position = it.position,
                )
            },
            reports = reportDao.getAll().map {
                BackupReport(
                    id = it.id, jobId = it.jobId, workPerformed = it.workPerformed,
                    materialsJson = it.materialsJson, notes = it.notes, source = it.source,
                    workPrice = it.workPrice, createdAt = it.createdAt, updatedAt = it.updatedAt,
                )
            },
        )
        file.writeText(json.encodeToString(BackupData.serializer(), data))
    }

    /** Импортирует данные из JSON-файла (в режиме «добавить»). */
    suspend fun importFromFile(file: File): Int = withContext(Dispatchers.IO) {
        val text = file.readText()
        val data = json.decodeFromString(BackupData.serializer(), text)
        var count = 0
        data.jobs.forEach { j ->
            val existing = jobDao.getById(j.id)
            if (existing == null) {
                jobDao.upsert(
                    com.otchetmaster.app.data.local.JobEntity(
                        id = j.id, date = j.date, address = j.address,
                        clientName = j.clientName, clientPhone = j.clientPhone,
                        status = j.status, project = j.project,
                        createdAt = j.createdAt, updatedAt = j.updatedAt,
                    )
                )
                count++
            }
        }
        data.photos.forEach { p ->
            photoDao.upsert(
                com.otchetmaster.app.data.local.PhotoEntity(
                    id = p.id, jobId = p.jobId, localPath = p.localPath,
                    position = p.position, caption = p.caption, createdAt = p.createdAt,
                )
            )
        }
        data.materials.forEach { m ->
            materialDao.upsertAll(
                listOf(
                    com.otchetmaster.app.data.local.MaterialEntity(
                        id = m.id, jobId = m.jobId, name = m.name,
                        quantity = m.quantity, price = m.price, position = m.position,
                    )
                )
            )
        }
        data.reports.forEach { r ->
            reportDao.upsert(
                com.otchetmaster.app.data.local.ReportEntity(
                    id = r.id, jobId = r.jobId, workPerformed = r.workPerformed,
                    materialsJson = r.materialsJson, notes = r.notes, source = r.source,
                    workPrice = r.workPrice, createdAt = r.createdAt, updatedAt = r.updatedAt,
                )
            )
        }
        count
    }
}
