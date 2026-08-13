package com.otchetmaster.app.ui.job

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.data.PhotoRepository
import com.otchetmaster.app.data.ReportRepository
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
    reportRepository: ReportRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val job by jobRepository.observeJob(jobId).collectAsState(initial = null)
    val photos by photoRepository.observeByJob(jobId).collectAsState(initial = emptyList())
    val report by reportRepository.observeByJob(jobId).collectAsState(initial = null)

    var description by rememberSaveable(jobId) { mutableStateOf(report?.workPerformed ?: "") }

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
            job?.let {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Дата: ${it.date}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Адрес: ${it.address.ifBlank { "—" }}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Клиент: ${it.clientName.ifBlank { "—" }}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (it.clientPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Телефон: ${it.clientPhone}", style = MaterialTheme.typography.bodyLarge)
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
                maxLines = 12
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Фото работ", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    PhotoTile(
                        photo = photo,
                        onRemove = {
                            scope.launch { photoRepository.remove(photo) }
                        }
                    )
                }
                item(key = "add") {
                    AddPhotoTile(
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
            Spacer(modifier = Modifier.height(24.dp))
        }
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
private fun PhotoTile(photo: PhotoEntity, onRemove: () -> Unit) {
    Box {
        AsyncImage(
            model = File(photo.localPath),
            contentDescription = "Фото работы",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
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
