package com.otchetmaster.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MasterProfileEntity::class,
        JobEntity::class,
        PhotoEntity::class,
        MaterialEntity::class,
        ReportEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class OtchetDatabase : RoomDatabase() {
    abstract fun masterProfileDao(): MasterProfileDao
    abstract fun jobDao(): JobDao
    abstract fun photoDao(): PhotoDao
    abstract fun materialDao(): MaterialDao
    abstract fun reportDao(): ReportDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE jobs ADD COLUMN status TEXT NOT NULL DEFAULT 'IN_PROGRESS'")
                db.execSQL("ALTER TABLE photos ADD COLUMN caption TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
