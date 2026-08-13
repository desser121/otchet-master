package com.otchetmaster.app.data

import com.otchetmaster.app.data.local.JobDao
import com.otchetmaster.app.data.local.JobEntity
import com.otchetmaster.app.data.local.MasterProfileDao
import com.otchetmaster.app.data.local.MasterProfileEntity
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

    suspend fun create(date: String, address: String, clientName: String, clientPhone: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            JobEntity(
                id = id,
                date = date,
                address = address,
                clientName = clientName,
                clientPhone = clientPhone,
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
}
