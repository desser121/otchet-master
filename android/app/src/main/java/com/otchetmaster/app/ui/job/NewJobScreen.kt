package com.otchetmaster.app.ui.job

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import com.otchetmaster.app.data.JobRepository
import com.otchetmaster.app.ui.job.utils.todayIso
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewJobScreen(
    jobRepository: JobRepository,
    onJobCreated: (String) -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(todayIso()) }
    var address by rememberSaveable { mutableStateOf("") }
    var clientName by rememberSaveable { mutableStateOf("") }
    var clientPhone by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var saving by rememberSaveable { mutableStateOf(false) }

    val canSave = address.isNotBlank() && clientName.isNotBlank() && !saving
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Новая работа") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Дата (ГГГГ-ММ-ДД)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Адрес объекта") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = clientName,
                onValueChange = { clientName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Имя клиента") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = clientPhone,
                onValueChange = { clientPhone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Телефон клиента") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = project,
                onValueChange = { project = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Проект / папка (необязательно)") },
                placeholder = { Text("Например: Квартира на Ленина 5") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        val id = jobRepository.create(date, address, clientName, clientPhone, project)
                        onJobCreated(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSave
            ) {
                Text(
                    text = if (saving) "Сохранение…" else "Далее — фото",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
