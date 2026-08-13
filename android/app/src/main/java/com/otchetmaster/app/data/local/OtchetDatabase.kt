package com.otchetmaster.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MasterProfileEntity::class,
        JobEntity::class,
        PhotoEntity::class,
        MaterialEntity::class,
        ReportEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OtchetDatabase : RoomDatabase() {
    abstract fun masterProfileDao(): MasterProfileDao
    abstract fun jobDao(): JobDao
    abstract fun photoDao(): PhotoDao
    abstract fun materialDao(): MaterialDao
    abstract fun reportDao(): ReportDao
}
