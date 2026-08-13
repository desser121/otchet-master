package com.otchetmaster.app.ui.job

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.data.MaterialRepository
import com.otchetmaster.app.data.PhotoRepository
import com.otchetmaster.app.data.ReportRepository
import com.otchetmaster.app.data.local.JobStatus
import com.otchetmaster.app.data.local.PhotoEntity
import com.otchetmaster.app.data.local.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    jobRepository: JobRepository,
    photoRepository: PhotoRepository,
    materialRepository: MaterialRepository,
    reportRepository: ReportRepository,
    onOpenReport: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val job by jobRepository.observeJob(jobId).collectAsState(initial = null)
    val photos by photoRepository.observeByJob(jobId).collectAsState(initial = emptyList())
    val materials by materialRepository.observeByJob(jobId).collectAsState(initial = emptyList())
    val report by reportRepository.observeByJob(jobId).collectAsState(initial = null)

    var description by rememberSaveable(jobId) { mutableStateOf(report?.workPerformed ?: "") }
    var materialInput by rememberSaveable(jobId) { mutableStateOf("") }

    LaunchedEffect(report) {
        val r = report
        if (r != null && description != r.workPerformed) {
            description = r.workPerformed ?: ""
        }
    }

    val saveDescription: (String) -> Unit = { text ->
        val r = report
        if (r == null) {
            scope.launch {
                val now = System.currentTimeMillis()
                reportRepository.upsert(
                    ReportEntity(
                        id = UUID.randomUUID().toString(),
                        jobId = jobId,
                        workPerformed = text,
                        source = "manual",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        } else {
            scope.launch {
                reportRepository.upsert(r.copy(workPerformed = text))
            }
        }
    }

    var photoCounter by remember(jobId) { mutableStateOf(0) }
    var captionPhoto by remember(jobId) { mutableStateOf<PhotoEntity?>(null) }
    var captionText by rememberSaveable(jobId) { mutableStateOf("") }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val position = photos.size
            val path = savePhotoToStorage(context, uri, jobId)
            photoRepository.add(jobId, path, position)
        }
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            scope.launch {
                val position = photos.size
                val path = context.filesDir.resolve("jobs/$jobId/photo_${photoCounter}.jpg")
                photoCounter++
                photoRepository.add(jobId, path.absolutePath, position)
            }
        }
    }

    val cameraUri = remember(jobId) {
        mutableStateOf<Uri?>(null)
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val recognized = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!recognized.isNullOrBlank()) {
            val newText = if (description.isBlank()) recognized else description + "\n" + recognized
            description = newText
            saveDescription(newText)
        }
    }

    fun startDictation() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Говорите описание работ")
        }
        speechLauncher.launch(intent)
    }

    fun copyJob() {
        val current = job ?: return
        scope.launch {
            val newId = jobRepository.copy(current.id)
            val sourcePhotos = photoRepository.getByJob(current.id)
            sourcePhotos.forEachIndexed { index, photo ->
                photoRepository.add(newId, photo.localPath, index)
            }
            val sourceReport = reportRepository.getByJob(current.id)
            if (sourceReport != null) {
                reportRepository.upsert(
                    sourceReport.copy(
                        id = UUID.randomUUID().toString(),
                        jobId = newId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            val sourceMaterials = materialRepository.getByJob(current.id)
            materialRepository.replaceAll(
                newId,
                sourceMaterials.map { it.name to (it.quantity ?: "") }
            )
            Toast.makeText(context, "Работа скопирована", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Работа") },
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
            job?.let { currentJob ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Статус: ${JobStatus.fromName(currentJob.status).label}", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            JobStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = currentJob.status == status.name,
                                    onClick = {
                                        scope.launch { jobRepository.setStatus(currentJob.id, status.name) }
                                    },
                                    label = { Text(status.label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Дата: ${currentJob.date}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Адрес: ${currentJob.address.ifBlank { "—" }}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Клиент: ${currentJob.clientName.ifBlank { "—" }}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (currentJob.clientPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Телефон: ${currentJob.clientPhone}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { text ->
                    description = text
                    saveDescription(text)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание работ") },
                placeholder = { Text("Что было сделано: демонтаж, электрика, отделка…") },
                minLines = 5,
                maxLines = 12,
                trailingIcon = {
                    IconButton(onClick = ::startDictation) {
                        Icon(Icons.Filled.Mic, contentDescription = "Голосовой ввод")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Фото работ", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val gridItems: List<PhotoGridItem> = photos.map { PhotoGridItem.Photo(it) } + PhotoGridItem.Add
            gridItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            when (item) {
                                is PhotoGridItem.Photo -> PhotoTile(
                                    photo = item.photo,
                                    onClick = {
                                        captionText = item.photo.caption
                                        captionPhoto = item.photo
                                    },
                                    onRemove = { scope.launch { photoRepository.remove(item.photo) } }
                                )
                                PhotoGridItem.Add -> AddPhotoTile(
                                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    onTakePhoto = {
                                        val file = File(context.filesDir, "jobs/$jobId/photo_${photoCounter}.jpg")
                                        file.parentFile?.mkdirs()
                                        cameraUri.value = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        cameraUri.value?.let { uri ->
                                            takePhoto.launch(uri)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text("Материалы", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            materials.forEach { m ->
                Text(
                    text = if (m.quantity != null) "• ${m.name} — ${m.quantity}" else "• ${m.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
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
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (materialInput.isNotBlank()) {
                            val name = materialInput.substringBefore("—").trim()
                            val quantity = materialInput.substringAfter("—", "").trim()
                            scope.launch {
                                val updated = materials.map { it.name to (it.quantity ?: "") }
                                    .plus(name to quantity)
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
                onClick = onOpenReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Создать отчёт", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { copyJob() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Создать копию работы")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    captionPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { captionPhoto = null },
            title = { Text("Подпись к фото") },
            text = {
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Например: ванная, до работ") },
                    singleLine = false
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch { photoRepository.setCaption(photo.id, captionText) }
                    captionPhoto = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { captionPhoto = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private suspend fun savePhotoToStorage(context: Context, uri: Uri, jobId: String): String {
    return withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "jobs/$jobId")
        dir.mkdirs()
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Не удалось открыть файл")
        input.use { ins ->
            val bmp = BitmapFactory.decodeStream(ins)
                ?: throw IllegalStateException("Не удалось прочитать изображение")
            val max = 2048
            val scale = minOf(1f, max.toFloat() / maxOf(bmp.width, bmp.height))
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
            } else bmp
            FileOutputStream(file).use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bmp) scaled.recycle()
            bmp.recycle()
        }
        file.absolutePath
    }
}

@Composable
private fun PhotoTile(
    photo: PhotoEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        Box {
            AsyncImage(
                model = File(photo.localPath),
                contentDescription = "Фото работы",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onClick),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
            }
        }
        if (photo.caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = photo.caption,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddPhotoTile(onClick: () -> Unit, onTakePhoto: () -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Из галереи",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Галерея", style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onTakePhoto),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Снять камерой",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Камера", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private sealed interface PhotoGridItem {
    data class Photo(val photo: com.otchetmaster.app.data.local.PhotoEntity) : PhotoGridItem
    data object Add : PhotoGridItem
}
