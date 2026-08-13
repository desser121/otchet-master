package com.otchetmaster.app.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.otchetmaster.app.data.local.JobEntity
import com.otchetmaster.app.data.local.MaterialEntity
import com.otchetmaster.app.data.local.MasterProfileEntity
import com.otchetmaster.app.data.local.PhotoEntity
import com.otchetmaster.app.data.local.ReportEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val CONTENT_W = PAGE_W - MARGIN * 2
    private const val CONTENT_H = PAGE_H - MARGIN * 2
    private const val PHOTO_H = 320f

    suspend fun generate(
        context: Context,
        profile: MasterProfileEntity,
        job: JobEntity,
        photos: List<PhotoEntity>,
        materials: List<MaterialEntity>,
        report: ReportEntity,
    ): File = withContext(Dispatchers.IO) {
        buildPdf(context, profile, job, photos, materials, report)
    }

    private fun buildPdf(
        context: Context,
        profile: MasterProfileEntity,
        job: JobEntity,
        photos: List<PhotoEntity>,
        materials: List<MaterialEntity>,
        report: ReportEntity,
    ): File {
        val doc = PdfDocument()

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 20f
            color = 0xFF444444.toInt()
        }
        val subPaint = Paint().apply {
            textSize = 12f
            color = 0xFF444444.toInt()
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
            color = 0xFF000000.toInt()
        }
        val headerLabel = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = 0xFF000000.toInt()
        }
        val grayLine = Paint().apply {
            color = 0xFFDDDDDD.toInt()
            strokeWidth = 1f
        }

        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var canvas = page.canvas
        var y = MARGIN

        fun newPageIfNeeded(extra: Float) {
            if (y + extra > MARGIN + CONTENT_H) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        // Header
        canvas.drawText("ОТЧЁТ О ВЫПОЛНЕННЫХ РАБОТАХ", MARGIN, y, titlePaint)
        y += 24
        canvas.drawText("Мастер: ${profile.name}", MARGIN, y, subPaint)
        y += 16
        canvas.drawText("Телефон: ${profile.phone}  |  Город: ${profile.city}", MARGIN, y, subPaint)
        y += 20
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, grayLine)
        y += 18

        // Job info
        fun infoRow(label: String, value: String) {
            canvas.drawText(label, MARGIN, y, headerLabel)
            canvas.drawText(value, MARGIN + 110f, y, bodyPaint)
            y += 16
        }
        infoRow("Дата:", job.date)
        infoRow("Адрес:", job.address)
        infoRow("Клиент:", job.clientName + if (job.clientPhone.isNotBlank()) " (${job.clientPhone})" else "")
        y += 10

        // Work performed
        newPageIfNeeded(60f)
        canvas.drawText("Выполненные работы", MARGIN, y, headerLabel)
        y += 16
        report.workPerformed.split("\n").forEach { line ->
            newPageIfNeeded(16f)
            canvas.drawText(line.ifBlank { " " }, MARGIN + 10f, y, bodyPaint)
            y += 15
        }
        y += 12

        // Materials
        if (materials.isNotEmpty()) {
            newPageIfNeeded(50f)
            canvas.drawText("Использованные материалы", MARGIN, y, headerLabel)
            y += 16
            materials.forEach { m ->
                newPageIfNeeded(16f)
                val text = if (m.quantity != null) "${m.name} — ${m.quantity}" else m.name
                canvas.drawText("• $text", MARGIN + 10f, y, bodyPaint)
                y += 15
            }
            y += 12
        }

        // Photos
        if (photos.isNotEmpty()) {
            canvas.drawText("Фото работ", MARGIN, y, headerLabel)
            y += 14
            photos.forEach { photo ->
                newPageIfNeeded(PHOTO_H + 40f)
                try {
                    val bmp = BitmapFactory.decodeFile(photo.localPath)
                    if (bmp != null) {
                        val scale = CONTENT_W / bmp.width
                        val drawH = bmp.height * scale
                        canvas.drawBitmap(
                            bmp,
                            null,
                            android.graphics.RectF(MARGIN, y, MARGIN + CONTENT_W, y + drawH),
                            null
                        )
                        bmp.recycle()
                        y += drawH + 14
                    }
                } catch (_: Exception) {
                    canvas.drawText("(не удалось загрузить фото)", MARGIN, y, subPaint)
                    y += 14
                }
            }
        }

        doc.finishPage(page)

        val safeName = job.clientName.ifBlank { "client" }.replace(Regex("[^\\wа-яА-ЯёЁ]+"), "_")
        val outFile = File(context.cacheDir, "otchet_${job.date}_$safeName.pdf")
        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()
        return outFile
    }
}
