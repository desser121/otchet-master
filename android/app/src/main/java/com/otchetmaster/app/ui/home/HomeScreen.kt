package com.otchetmaster.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otchetmaster.app.data.local.JobEntity
import com.otchetmaster.app.data.local.JobStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewJob: () -> Unit,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onJobClick: (String) -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val updateState by viewModel.updateState.collectAsState()
    val jobs by viewModel.jobs.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedProject by rememberSaveable { mutableStateOf<String?>(null) }
    val projects = remember(jobs) {
        jobs.mapNotNull { it.project }
            .distinct()
            .sorted()
    }
    val visibleJobs = remember(jobs, selectedProject) {
        if (selectedProject == null) jobs
        else jobs.filter { it.project == selectedProject }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ОтчётМастер",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.checkForUpdate() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Проверить обновления")
                    }
                    IconButton(onClick = onBackup) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Бэкап данных")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (val state = updateState) {
                is UpdateUiState.UpdateAvailable -> {
                    UpdateBanner(
                        state = state,
                        onUpdate = { viewModel.downloadUpdate() },
                        onInstall = { viewModel.installUpdate() },
                        onDismiss = { viewModel.dismissUpdate() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is UpdateUiState.Downloading -> {
                    UpdateBanner(
                        state = UpdateUiState.Downloading,
                        onUpdate = {},
                        onInstall = {},
                        onDismiss = {}
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is UpdateUiState.Downloaded -> {
                    UpdateBanner(
                        state = state,
                        onUpdate = {},
                        onInstall = { viewModel.installUpdate() },
                        onDismiss = { viewModel.dismissUpdate() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is UpdateUiState.Error -> {
                    LaunchedEffect(state.message) {
                        snackbarHostState.showSnackbar(state.message)
                        viewModel.dismissUpdate()
                    }
                }
                else -> {}
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("История работ", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    StatsRow(stats = stats)
                    if (projects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedProject == null,
                                onClick = { selectedProject = null },
                                label = { Text("Все") }
                            )
                            projects.forEach { project ->
                                FilterChip(
                                    selected = selectedProject == project,
                                    onClick = {
                                        selectedProject = if (selectedProject == project) null else project
                                    },
                                    label = { Text(project) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (visibleJobs.isEmpty()) {
                        Text(
                            text = if (selectedProject == null)
                                "Пока нет работ. Нажмите «Новая работа»"
                            else
                                "В этом проекте пока нет работ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(360.dp)) {
                            items(visibleJobs, key = { it.id }) { job ->
                                JobListItem(job = job, onClick = { onJobClick(job.id) })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNewJob,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Новая работа", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state is UpdateUiState.Downloading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                when (state) {
                    is UpdateUiState.UpdateAvailable -> {
                        Text(
                            text = "Доступна версия ${state.version}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Скачайте обновление и установите вручную",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is UpdateUiState.Downloading -> {
                        Text(
                            text = "Скачивание обновления…",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    is UpdateUiState.Downloaded -> {
                        Text(
                            text = "Скачано ${state.version}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Нажмите «Установить», чтобы обновить",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
            when (state) {
                is UpdateUiState.UpdateAvailable -> {
                    Button(onClick = onUpdate) {
                        Text("Скачать")
                    }
                }
                is UpdateUiState.Downloaded -> {
                    Button(onClick = onInstall) {
                        Text("Установить")
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun JobListItem(job: JobEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.address.ifBlank { "Без адреса" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(
                        job.date,
                        job.clientName.ifBlank { job.clientPhone.ifBlank { "Клиент не указан" } },
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                job.project?.let { project ->
                    Text(
                        text = project,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsRow(stats: Map<String, Int>) {
    val items = listOf(
        JobStatus.IN_PROGRESS to "В работе",
        JobStatus.DONE to "Готово",
        JobStatus.SENT to "Отправлено",
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (key, label) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = (stats[key.name] ?: 0).toString(),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
