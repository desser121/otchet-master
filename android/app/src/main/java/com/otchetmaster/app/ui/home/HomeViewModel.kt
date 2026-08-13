package com.otchetmaster.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.data.local.JobEntity
import com.otchetmaster.app.updater.UpdateInfo
import com.otchetmaster.app.updater.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object Downloading : UpdateUiState
    data class UpdateAvailable(val version: String) : UpdateUiState
    data class Downloaded(val version: String) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class HomeViewModel(
    private val updateManager: UpdateManager,
    jobRepository: JobRepository,
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobEntity>>(emptyList())
    val jobs: StateFlow<List<JobEntity>> = _jobs.asStateFlow()

    private val _stats = MutableStateFlow<Map<String, Int>>(emptyMap())
    val stats: StateFlow<Map<String, Int>> = _stats.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    init {
        viewModelScope.launch {
            jobRepository.jobs.collect { _jobs.value = it }
        }
        viewModelScope.launch {
            jobRepository.jobs.collect { _stats.value = jobRepository.statusCounts() }
        }
    }

    fun checkForUpdate() {
        _updateState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val info: UpdateInfo = try {
                updateManager.checkForUpdate()
            } catch (e: Exception) {
                _updateState.value = UpdateUiState.Error("Не удалось проверить обновления")
                return@launch
            }
            if (info.isNewer) {
                _updateState.value = UpdateUiState.UpdateAvailable(info.latestVersion)
            } else {
                _updateState.value = UpdateUiState.Idle
            }
        }
    }

    fun downloadUpdate() {
        val state = _updateState.value as? UpdateUiState.UpdateAvailable ?: return
        _updateState.value = UpdateUiState.Downloading
        viewModelScope.launch {
            try {
                val info = updateManager.checkForUpdate()
                val apkUrl = info.apkUrl ?: run {
                    _updateState.value = UpdateUiState.Error("APK не найден в релизе")
                    return@launch
                }
                val file = updateManager.downloadApk(apkUrl)
                _updateState.value = UpdateUiState.Downloaded(state.version)
            } catch (e: Exception) {
                _updateState.value = UpdateUiState.Error("Ошибка при скачивании обновления")
            }
        }
    }

    fun installUpdate() {
        val state = _updateState.value as? UpdateUiState.Downloaded ?: return
        val file = updateManager.downloadedApkFile()
        if (file == null || !file.exists()) {
            _updateState.value = UpdateUiState.Error("Файл обновления не найден")
            return
        }
        val started = updateManager.installApk(file)
        if (started) {
            _updateState.value = UpdateUiState.Idle
        } else {
            _updateState.value = UpdateUiState.Error("Разрешите установку из этого источника в настройках")
        }
    }

    fun dismissUpdate() {
        _updateState.value = UpdateUiState.Idle
    }
}
