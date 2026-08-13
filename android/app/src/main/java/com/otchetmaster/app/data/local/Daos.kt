package com.otchetmaster.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: MasterProfileEntity)

    @Query("SELECT * FROM master_profile WHERE id = 1")
    fun observe(): Flow<MasterProfileEntity?>

    @Query("SELECT * FROM master_profile WHERE id = 1")
    suspend fun get(): MasterProfileEntity?
}

@Dao
interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: JobEntity)

    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE id = :id")
    fun observeById(id: String): Flow<JobEntity?>

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE jobId = :jobId ORDER BY position ASC")
    fun observeByJob(jobId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE jobId = :jobId ORDER BY position ASC")
    suspend fun getByJob(jobId: String): List<PhotoEntity>

    @Delete
    suspend fun delete(photo: PhotoEntity)
}

@Dao
interface MaterialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(materials: List<MaterialEntity>)

    @Query("SELECT * FROM materials WHERE jobId = :jobId ORDER BY position ASC")
    fun observeByJob(jobId: String): Flow<List<MaterialEntity>>

    @Query("DELETE FROM materials WHERE jobId = :jobId")
    suspend fun deleteByJob(jobId: String)
}

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE jobId = :jobId LIMIT 1")
    fun observeByJob(jobId: String): Flow<ReportEntity?>

    @Query("SELECT * FROM reports WHERE jobId = :jobId LIMIT 1")
    suspend fun getByJob(jobId: String): ReportEntity?
}
