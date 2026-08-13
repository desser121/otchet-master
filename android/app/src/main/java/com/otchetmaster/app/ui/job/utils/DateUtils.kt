package com.otchetmaster.app.ui.job.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun todayIso(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
