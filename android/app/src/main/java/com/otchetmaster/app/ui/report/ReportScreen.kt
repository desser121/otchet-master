package com.otchetmaster.app.ui.report

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.data.MaterialRepository
import com.otchetmaster.app.data.PhotoRepository
import com.otchetmaster.app.data.ProfileRepository
import com.otchetmaster.app.data.ReportRepository
import com.otchetmaster.app.data.local.ReportEntity
import com.otchetmaster.app.pdf.PdfGenerator
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    jobId: String,
    profileRepository: ProfileRepository,
    jobRepository: JobRepository,
    photoRepository: PhotoRepository,
    materialRepository: MaterialRepository,
    reportRepository: ReportRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile by profileRepository.profile.collectAsState(initial = null)
    val job by jobRepository.observeJob(jobId).collectAsState(initial = null)
    val photos by photoRepository.observeByJob(jobId).collectAsState(initial = emptyList())
    val materials by materialRepository.observeByJob(jobId).collectAsState(initial = emptyList())
    val report by reportRepository.observeByJob(jobId).collectAsState(initial = null)

    var description by remember(jobId) { mutableStateOf("") }
    var materialInput by remember(jobId) { mutableStateOf("") }
    var generating by remember(jobId) { mutableStateOf(false) }

    LaunchedEffect(report) {
        description = report?.workPerformed ?: ""
    }

    fun generateAndShare() {
        val profile = profile ?: return
        val job = job ?: return
        val report = report ?: return
        if (description.isBlank()) {
            Toast.makeText(context, "Добавьте описание работ", Toast.LENGTH_SHORT).show()
            return
        }
        generating = true
        scope.launch {
            val pdf = try {
                PdfGenerator.generate(context, profile, job, photos, materials, report.copy(workPerformed = description))
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
                generating = false
                return@launch
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdf
            )
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Отчёт о работах — ${job.clientName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(share, "Отправить отчёт"))
            generating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёт") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            job?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${it.date} — ${it.address.ifBlank { "без адреса" }}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Клиент: ${it.clientName.ifBlank { "—" }}${if (it.clientPhone.isNotBlank()) ", ${it.clientPhone}" else ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Фото: ${photos.size} шт.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Описание работ (редактируется)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 15
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Материалы", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            materials.forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (m.quantity != null) "• ${m.name} — ${m.quantity}" else "• ${m.name}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = materialInput,
                    onValueChange = { materialInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Название — количество") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(
                    onClick = {
                        if (materialInput.isNotBlank()) {
                            val name = materialInput.substringBefore("—").trim()
                            val quantity = materialInput.substringAfter("—", "").trim()
                            scope.launch {
                                val updated = materials.map {
                                    it.name to (it.quantity ?: "")
                                }.plus(name to quantity)
                                materialRepository.replaceAll(jobId, updated)
                            }
                            materialInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить материал")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { generateAndShare() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !generating
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (generating) "Создание PDF…" else "Создать PDF и отправить")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ручной режим: отчёт собирается на устройстве, без интернета.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
