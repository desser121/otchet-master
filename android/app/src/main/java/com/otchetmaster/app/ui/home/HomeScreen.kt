package com.otchetmaster.app.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.otchetmaster.app.updater.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNewJob: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory = HomeViewModelFactory(UpdateManager(LocalContext.current)),
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val updateState by viewModel.updateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ОтчётМастер",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.checkForUpdate() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Проверить обновления")
                }
            }
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
                        version = state.version,
                        downloading = false,
                        onUpdate = { viewModel.downloadAndInstall() },
                        onDismiss = { viewModel.dismissUpdate() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                is UpdateUiState.Downloading -> {
                    UpdateBanner(
                        version = "",
                        downloading = true,
                        onUpdate = {},
                        onDismiss = {}
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "История работ — будет здесь",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    version: String,
    downloading: Boolean,
    onUpdate: () -> Unit,
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
            if (downloading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (downloading) "Скачивание обновления…" else "Доступна версия $version",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!downloading) {
                    Text(
                        text = "Нажмите «Обновить», чтобы установить",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!downloading) {
                Button(onClick = onUpdate) {
                    Text("Обновить")
                }
            }
        }
    }
}
