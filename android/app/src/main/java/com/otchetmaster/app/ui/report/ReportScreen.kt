package com.otchetmaster.app.ui.report

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
import java.io.File
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
    var workPriceInput by remember(jobId) { mutableStateOf("") }
    var materialInput by remember(jobId) { mutableStateOf("") }
    var generating by remember(jobId) { mutableStateOf(false) }

    fun parsePrice(raw: String): Double? =
        raw.replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0 }

    fun totalMaterials(): Double = materials.sumOf { it.price ?: 0.0 }

    fun totalAmount(): Double = (workPriceInput.replace(",", ".").toDoubleOrNull() ?: 0.0) + totalMaterials()

    fun formatPrice(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)

    fun isValidPdf(file: File): Boolean {
        return try {
            val header = file.inputStream().use { it.readBytes().take(5) }
            String(header.toByteArray()).startsWith("%PDF")
        } catch (e: Exception) {
            false
        }
    }

    fun generateAndSave(uri: android.net.Uri) {
        val profile = profile ?: return
        val job = job ?: return
        val report = report ?: ReportEntity(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            workPerformed = description,
            source = "manual",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        generating = true
        scope.launch {
            try {
                val pdf = PdfGenerator.generate(context, profile, job, photos, materials, report.copy(workPerformed = description, workPrice = parsePrice(workPriceInput)))
                if (!isValidPdf(pdf)) {
                    Toast.makeText(context, "PDF сформирован некорректно: ${pdf.length()} байт", Toast.LENGTH_LONG).show()
                    generating = false
                    return@launch
                }
                val out = context.contentResolver.openOutputStream(uri)
                if (out == null) {
                    Toast.makeText(context, "Не удалось открыть файл для записи", Toast.LENGTH_LONG).show()
                    generating = false
                    return@launch
                }
                out.use { out2 ->
                    pdf.inputStream().use { it.copyTo(out2) }
                }
                Toast.makeText(context, "PDF сохранён", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при сохранении PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
            generating = false
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) generateAndSave(uri)
    }

    LaunchedEffect(report) {
        description = report?.workPerformed ?: ""
        workPriceInput = report?.workPrice?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: ""
    }

    fun saveWorkPrice(raw: String) {
        val r = report
        val price = parsePrice(raw)
        scope.launch {
            if (r == null) {
                val now = System.currentTimeMillis()
                reportRepository.upsert(
                    ReportEntity(
                        id = UUID.randomUUID().toString(),
                        jobId = jobId,
                        workPerformed = description,
                        workPrice = price,
                        source = "manual",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            } else {
                reportRepository.upsert(r.copy(workPrice = price))
            }
        }
    }

    lateinit var storagePermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    fun saveToDownloads(pdf: File): Uri? {
        val fileName = pdf.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            val written = context.contentResolver.openOutputStream(uri)?.use { out ->
                pdf.inputStream().use { it.copyTo(out) }
            }
            if (written == null || written == 0L) {
                context.contentResolver.delete(uri, null, null)
                return null
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return uri
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val target = File(dir, fileName)
            pdf.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        }
    }

    fun generateAndPreview() {
        val profile = profile ?: return
        val job = job ?: return
        val report = report ?: ReportEntity(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            workPerformed = description,
            source = "manual",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        generating = true
        scope.launch {
            try {
                val pdf = PdfGenerator.generate(context, profile, job, photos, materials, report.copy(workPerformed = description, workPrice = parsePrice(workPriceInput)))
                if (!isValidPdf(pdf)) {
                    Toast.makeText(context, "PDF сформирован некорректно: ${pdf.length()} байт", Toast.LENGTH_LONG).show()
                    generating = false
                    return@launch
                }
                saveToDownloads(pdf)
                val viewUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdf
                )
                val view = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(viewUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(view)
                } catch (e: Exception) {
                    Toast.makeText(context, "PDF сохранён в Загрузки", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка при создании PDF", Toast.LENGTH_SHORT).show()
            }
            generating = false
        }
    }

    fun generateAndShare() {
        val profile = profile ?: return
        val job = job ?: return
        val report = report ?: ReportEntity(
            id = UUID.randomUUID().toString(),
            jobId = jobId,
            workPerformed = description,
            source = "manual",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        generating = true
        scope.launch {
            val pdf = try {
                PdfGenerator.generate(context, profile, job, photos, materials, report.copy(workPerformed = description, workPrice = parsePrice(workPriceInput)))
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

    storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            generateAndPreview()
        } else {
            Toast.makeText(context, "Нет разрешения на запись", Toast.LENGTH_SHORT).show()
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
                    m.price?.let { price ->
                        Text(
                            text = "${formatPrice(price)} ₽",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    label = { Text("Название — количество — цена") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(
                    onClick = {
                        if (materialInput.isNotBlank()) {
                            val parts = materialInput.split("—").map { it.trim() }
                            val name = parts.getOrNull(0).orEmpty()
                            val quantity = parts.getOrNull(1).orEmpty()
                            val price = parts.getOrNull(2)?.let { parsePrice(it) }
                            scope.launch {
                                val updated = materials.map {
                                    Triple(it.name, it.quantity ?: "", it.price)
                                }.plus(Triple(name, quantity, price))
                                materialRepository.replaceAll(jobId, updated)
                            }
                            materialInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить материал")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text("Стоимость работы, ₽", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = workPriceInput,
                onValueChange = {
                    workPriceInput = it
                    saveWorkPrice(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Например: 15000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Материалы", style = MaterialTheme.typography.bodyLarge)
                        Text("${formatPrice(totalMaterials())} ₽", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Работа", style = MaterialTheme.typography.bodyLarge)
                        Text("${formatPrice(parsePrice(workPriceInput) ?: 0.0)} ₽", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Итого", style = MaterialTheme.typography.titleMedium)
                        Text("${formatPrice(totalAmount())} ₽", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { generateAndPreview() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !generating
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(if (generating) "Создание PDF…" else "Создать PDF")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { generateAndShare() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !generating
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Отправить")
                }
                OutlinedButton(
                    onClick = {
                        saveLauncher.launch(
                            "Отчёт_${(job?.clientName ?: "").ifBlank { "работы" }}.pdf"
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !generating
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("В файл")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PDF автоматически сохраняется в «Загрузки» и открывается для предпросмотра.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
