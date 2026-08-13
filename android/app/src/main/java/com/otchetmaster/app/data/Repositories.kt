package com.otchetmaster.app.data

import com.otchetmaster.app.data.local.JobDao
import com.otchetmaster.app.data.local.JobEntity
import com.otchetmaster.app.data.local.JobStatus
import com.otchetmaster.app.data.local.MaterialDao
import com.otchetmaster.app.data.local.MaterialEntity
import com.otchetmaster.app.data.local.MasterProfileDao
import com.otchetmaster.app.data.local.MasterProfileEntity
import com.otchetmaster.app.data.local.PhotoDao
import com.otchetmaster.app.data.local.PhotoEntity
import com.otchetmaster.app.data.local.ReportDao
import com.otchetmaster.app.data.local.ReportEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProfileRepository(private val dao: MasterProfileDao) {
    val profile: Flow<MasterProfileEntity?> = dao.observe()

    suspend fun save(name: String, phone: String, city: String) {
        dao.upsert(MasterProfileEntity(name = name, phone = phone, city = city))
    }

    suspend fun get(): MasterProfileEntity? = dao.get()
}

class JobRepository(private val dao: JobDao) {
    val jobs: Flow<List<JobEntity>> = dao.observeAll()

    fun observeJob(id: String): Flow<JobEntity?> = dao.observeById(id)

    suspend fun statusCounts(): Map<String, Int> =
        dao.statusCounts().associate { it.status to it.cnt }

    suspend fun create(
        date: String,
        address: String,
        clientName: String,
        clientPhone: String,
        project: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            JobEntity(
                id = id,
                date = date,
                address = address,
                clientName = clientName,
                clientPhone = clientPhone,
                project = project?.ifBlank { null },
                createdAt = now,
                updatedAt = now,
            )
        )
        return id
    }

    suspend fun update(job: JobEntity) {
        dao.upsert(job.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getById(id: String): JobEntity? = dao.getById(id)

    suspend fun setStatus(id: String, status: String) {
        val job = dao.getById(id) ?: return
        dao.upsert(job.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    /** Создаёт копию работы (данные без фото/отчёта/материалов — их копирует вызывающий код). */
    suspend fun copy(id: String): String {
        val source = dao.getById(id) ?: throw IllegalStateException("Работа не найдена")
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            source.copy(
                id = newId,
                status = JobStatus.IN_PROGRESS.name,
                createdAt = now,
                updatedAt = now,
                syncStatus = "LOCAL",
            )
        )
        return newId
    }
}

class PhotoRepository(private val dao: PhotoDao) {
    fun observeByJob(jobId: String): Flow<List<PhotoEntity>> = dao.observeByJob(jobId)

    suspend fun add(jobId: String, localPath: String, position: Int) {
        val now = System.currentTimeMillis()
        dao.upsert(
            PhotoEntity(
                id = UUID.randomUUID().toString(),
                jobId = jobId,
                localPath = localPath,
                position = position,
                createdAt = now,
            )
        )
    }

    suspend fun setCaption(photoId: String, caption: String) {
        val photo = dao.getById(photoId) ?: return
        dao.upsert(photo.copy(caption = caption))
    }

    suspend fun remove(photo: PhotoEntity) {
        dao.delete(photo)
    }

    suspend fun getByJob(jobId: String): List<PhotoEntity> = dao.getByJob(jobId)
}

class ReportRepository(private val dao: ReportDao) {
    fun observeByJob(jobId: String): Flow<ReportEntity?> = dao.observeByJob(jobId)

    suspend fun getByJob(jobId: String): ReportEntity? = dao.getByJob(jobId)

    suspend fun upsert(report: ReportEntity) {
        dao.upsert(report)
    }
}

class MaterialRepository(private val dao: MaterialDao) {
    fun observeByJob(jobId: String): Flow<List<MaterialEntity>> = dao.observeByJob(jobId)

    suspend fun getByJob(jobId: String): List<MaterialEntity> = dao.getByJob(jobId)

    suspend fun replaceAll(jobId: String, materials: List<Triple<String, String, Double?>>) {
        dao.deleteByJob(jobId)
        dao.upsertAll(
            materials.mapIndexed { index, (name, quantity, price) ->
                MaterialEntity(
                    id = UUID.randomUUID().toString(),
                    jobId = jobId,
                    name = name,
                    quantity = quantity.ifBlank { null },
                    price = price,
                    position = index,
                )
            }
        )
    }
}
