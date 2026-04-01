package com.skinsense.ai.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.skinsense.ai.data.AnalysisResult
import com.skinsense.ai.ui.compose.SeverityLevel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

object PdfGenerator {

    fun generatePdf(context: Context, result: AnalysisResult): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (approx)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Margins and positioning
        var yPosition = 50f
        val xMargin = 50f
        val lineHeight = 25f

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SkinSense AI - Analysis Report", xMargin, yPosition, paint)
        yPosition += lineHeight * 2

        // Disease Name
        paint.textSize = 20f
        paint.color = Color.DKGRAY
        canvas.drawText("Diagnosis: ${result.diseaseName}", xMargin, yPosition, paint)
        yPosition += lineHeight * 1.5f

        // Severity Level & Confidence
        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT
        val severityColor = when (result.severityLevel) {
            SeverityLevel.MILD -> Color.parseColor("#4CAF50") // Green
            SeverityLevel.MODERATE -> Color.parseColor("#FF9800") // Orange
            SeverityLevel.SEVERE -> Color.RED
        }
        paint.color = severityColor
        canvas.drawText("Severity Level: ${result.severityLevel.name}", xMargin, yPosition, paint)
        yPosition += lineHeight

        paint.color = Color.parseColor("#F97316") // OrangeAccent
        canvas.drawText("Affected Area: ${String.format("%.1f", result.severityPercentage)}%", xMargin, yPosition, paint)
        yPosition += lineHeight

        paint.color = Color.BLACK
        canvas.drawText("Confidence: ${(result.confidence * 100).toInt()}%", xMargin, yPosition, paint)
        yPosition += lineHeight * 1.5f

        // Description
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Description:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        yPosition = drawMultiLineText(canvas, result.description, xMargin, yPosition, paint, 500f)
        yPosition += lineHeight

        // Symptoms
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Symptoms:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.symptoms.isNotEmpty()) {
            result.symptoms.forEach { symptom ->
                canvas.drawText("• $symptom", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
            canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
            yPosition += lineHeight
        }
        yPosition += lineHeight

        // Causes
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Possible Causes:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.causes.isNotEmpty()) {
            result.causes.forEach { cause ->
                canvas.drawText("• $cause", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
            canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
            yPosition += lineHeight
        }
        yPosition += lineHeight

        // Curing Recommendations (Renamed from Recommended Actions)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Curing Recommendations:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.recommendedActions.isNotEmpty()) {
            result.recommendedActions.forEach { action ->
                canvas.drawText("✓ $action", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
             canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
             yPosition += lineHeight
        }

        pdfDocument.finishPage(page)

        // Save PDF
        val fileName = "SkinSense_Report_${System.currentTimeMillis()}.pdf"
        var uri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val contentResolver = context.contentResolver
                uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val outputStream = contentResolver.openOutputStream(it)
                    outputStream?.use { stream ->
                        pdfDocument.writeTo(stream)
                    }
                }
            } else {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(directory, fileName)
                val outputStream = FileOutputStream(file)
                outputStream.use { stream ->
                    pdfDocument.writeTo(stream)
                }
                uri = Uri.fromFile(file)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            // Returning null to indicate failure
            return null
        } finally {
            pdfDocument.close()
        }

        return uri
    }

    private fun drawMultiLineText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, paint: Paint, maxWidth: Float): Float {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) < maxWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += paint.descent() - paint.ascent()
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
            currentY += paint.descent() - paint.ascent()
        }
        return currentY
    }

    fun generateShareablePdf(context: Context, result: AnalysisResult): Uri? {
        // Reuse the logic (or refactor to common method, but for now just duplicate drawing for safety/speed 
        // OR better: Create a common 'createPdfDocument' method and then save differently.
        // To minimize risk, I will act as a wrapper if possible, but PDFDocument needs to be written to stream.
        
        // Let's refactor slightly to avoid duplication if possible. 
        // Actually, to avoid big refactor risk, I will just call generatePdfToStream logic. 
        
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // --- DRAWING LOGIC (Duplicated for safety to ensure exact same visual) ---
        var yPosition = 50f
        val xMargin = 50f
        val lineHeight = 25f

        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SkinSense AI - Analysis Report", xMargin, yPosition, paint)
        yPosition += lineHeight * 2

        paint.textSize = 20f
        paint.color = Color.DKGRAY
        canvas.drawText("Diagnosis: ${result.diseaseName}", xMargin, yPosition, paint)
        yPosition += lineHeight * 1.5f

        paint.textSize = 16f
        paint.typeface = Typeface.DEFAULT
        val severityColor = when (result.severityLevel) {
            SeverityLevel.MILD -> Color.parseColor("#4CAF50")
            SeverityLevel.MODERATE -> Color.parseColor("#FF9800")
            SeverityLevel.SEVERE -> Color.RED
        }
        paint.color = severityColor
        canvas.drawText("Severity Level: ${result.severityLevel.name}", xMargin, yPosition, paint)
        yPosition += lineHeight

        paint.color = Color.parseColor("#F97316")
        canvas.drawText("Affected Area: ${String.format("%.1f", result.severityPercentage)}%", xMargin, yPosition, paint)
        yPosition += lineHeight

        paint.color = Color.BLACK
        canvas.drawText("Confidence: ${(result.confidence * 100).toInt()}%", xMargin, yPosition, paint)
        yPosition += lineHeight * 1.5f

        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Description:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        yPosition = drawMultiLineText(canvas, result.description, xMargin, yPosition, paint, 500f)
        yPosition += lineHeight

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Symptoms:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.symptoms.isNotEmpty()) {
            result.symptoms.forEach { symptom ->
                canvas.drawText("• $symptom", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
            canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
            yPosition += lineHeight
        }
        yPosition += lineHeight

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Possible Causes:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.causes.isNotEmpty()) {
            result.causes.forEach { cause ->
                canvas.drawText("• $cause", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
            canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
            yPosition += lineHeight
        }
        yPosition += lineHeight
        
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Curing Recommendations:", xMargin, yPosition, paint)
        yPosition += lineHeight
        paint.typeface = Typeface.DEFAULT
        if (result.recommendedActions.isNotEmpty()) {
            result.recommendedActions.forEach { action ->
                canvas.drawText("✓ $action", xMargin + 10, yPosition, paint)
                yPosition += lineHeight
            }
        } else {
             canvas.drawText("• Information not available", xMargin + 10, yPosition, paint)
             yPosition += lineHeight
        }

        pdfDocument.finishPage(page)
        // --- END DRAWING LOGIC ---

        try {
            val cachePath = File(context.cacheDir, "documents")
            cachePath.mkdirs()
            val fileName = "SkinSense_Report_${System.currentTimeMillis()}.pdf"
            val file = File(cachePath, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            
            // Get FileProvider URI
            return androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.skinsense.ai.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
