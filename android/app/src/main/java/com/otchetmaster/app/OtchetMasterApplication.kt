package com.otchetmaster.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.data.MaterialRepository
import com.otchetmaster.app.data.PhotoRepository
import com.otchetmaster.app.data.ProfileRepository
import com.otchetmaster.app.data.ReportRepository
import com.otchetmaster.app.data.local.OtchetDatabase
import com.otchetmaster.app.updater.UpdateManager

class OtchetMasterApplication : Application() {

    val database: OtchetDatabase by lazy {
        Room.databaseBuilder(
            this,
            OtchetDatabase::class.java,
            "otchet-master.db"
        ).build()
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(database.masterProfileDao())
    }

    val jobRepository: JobRepository by lazy {
        JobRepository(database.jobDao())
    }

    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(database.photoDao())
    }

    val reportRepository: ReportRepository by lazy {
        ReportRepository(database.reportDao())
    }

    val materialRepository: MaterialRepository by lazy {
        MaterialRepository(database.materialDao())
    }

    val updateManager: UpdateManager by lazy {
        UpdateManager(this)
    }

    companion object {
        fun of(context: Context): OtchetMasterApplication =
            context.applicationContext as OtchetMasterApplication
    }
}
