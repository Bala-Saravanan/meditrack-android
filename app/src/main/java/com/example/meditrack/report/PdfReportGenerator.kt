package com.example.meditrack.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.model.ScheduleDetailedReport
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 width in points at 72 dpi
    private const val PAGE_HEIGHT = 842 // A4 height in points at 72 dpi
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = MARGIN_RIGHT - MARGIN_LEFT

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun generatePdfReportToFile(context: Context, report: ScheduleDetailedReport): File {
        val reportsDir = File(context.cacheDir, "reports").apply {
            if (!exists()) mkdirs()
        }
        // Clean up files older than 24h
        reportsDir.listFiles()?.forEach { file ->
            if (System.currentTimeMillis() - file.lastModified() > 24 * 60 * 60 * 1000L) {
                file.delete()
            }
        }
        val sanitizedName = report.scheduleName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "MediTrack_${sanitizedName}_$timestamp.pdf")
        FileOutputStream(file).use { out ->
            generatePdfReport(report, out)
        }
        return file
    }

    fun generatePdfReport(report: ScheduleDetailedReport, outputStream: OutputStream) {
        val document = PdfDocument()

        try {
            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            // Paints
            val primaryPaint = Paint().apply {
                color = Color.rgb(0, 104, 95) // Primary Teal #00685F
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.rgb(11, 28, 48) // Text dark
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 14f
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(109, 122, 119) // Text secondary
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9f
                isAntiAlias = true
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(0, 104, 95)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 11f
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(11, 28, 48)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9f
                isAntiAlias = true
            }

            val boldBodyPaint = Paint().apply {
                color = Color.rgb(11, 28, 48)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9f
                isAntiAlias = true
            }

            val footerPaint = Paint().apply {
                color = Color.rgb(140, 150, 148)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8f
                isAntiAlias = true
            }

            val cardBgPaint = Paint().apply {
                color = Color.rgb(243, 247, 255)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val cardBorderPaint = Paint().apply {
                color = Color.rgb(211, 228, 254)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            val tableHeaderPaint = Paint().apply {
                color = Color.rgb(0, 104, 95)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val tableHeaderTextPaint = Paint().apply {
                color = Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5f
                isAntiAlias = true
            }

            val tableRowAltPaint = Paint().apply {
                color = Color.rgb(248, 250, 255)
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.rgb(220, 226, 235)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

            var currentY = 40f

            // Function to check pagination and advance page
            fun checkAndAdvancePage(requiredHeight: Float, onNewPageHeader: () -> Unit = {}) {
                if (currentY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    drawFooter(canvas, currentPageNumber, footerPaint)
                    document.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 40f

                    // Running header on continuation pages
                    canvas.drawText("MediTrack Compliance Report • ${report.scheduleName}", MARGIN_LEFT, currentY, subtitlePaint)
                    canvas.drawLine(MARGIN_LEFT, currentY + 5f, MARGIN_RIGHT, currentY + 5f, linePaint)
                    currentY += 20f

                    onNewPageHeader()
                }
            }

            // --- PAGE 1: HEADER ---

            // Logo & Title
            canvas.drawText("MediTrack", MARGIN_LEFT, currentY, primaryPaint)
            currentY += 16f
            canvas.drawText("Official Regimen Compliance & Intake Ledger", MARGIN_LEFT, currentY, subtitlePaint)
            currentY += 12f

            // Top metadata line
            val genDateStr = "Generated on ${dateTimeFormat.format(Date())} (Device Local Time)"
            canvas.drawText(genDateStr, MARGIN_LEFT, currentY, subtitlePaint)
            currentY += 16f
            canvas.drawLine(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY, linePaint)
            currentY += 16f

            // Schedule Overview Card
            val cardRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 65f)
            canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(cardRect, 6f, 6f, cardBorderPaint)

            var cardTextY = currentY + 18f
            canvas.drawText("Regimen: ${report.scheduleName}", MARGIN_LEFT + 12f, cardTextY, titlePaint)

            // Status badge text
            val statusColor = when (report.scheduleStatus) {
                "Active" -> Color.rgb(22, 163, 74)
                "Completed" -> Color.rgb(37, 99, 235)
                else -> Color.rgb(107, 114, 128)
            }
            val statusPaint = Paint().apply {
                color = statusColor
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 10f
                isAntiAlias = true
            }
            canvas.drawText("Status: ${report.scheduleStatus}", MARGIN_RIGHT - 100f, cardTextY, statusPaint)

            cardTextY += 16f
            val dateRangeStr = "Reporting Window: ${dateFormat.format(Date(report.startTimestamp))} – ${dateFormat.format(Date(report.endTimestamp))} (${report.dateRangeType.label})"
            canvas.drawText(dateRangeStr, MARGIN_LEFT + 12f, cardTextY, bodyPaint)

            cardTextY += 14f
            val physicianStr = if (report.physicianName.isNotBlank()) "Supervising Physician: ${report.physicianName}" else "Self-Managed Regimen"
            canvas.drawText(physicianStr, MARGIN_LEFT + 12f, cardTextY, subtitlePaint)

            currentY += 75f

            // --- EXECUTIVE SUMMARY / METRICS GRID ---
            canvas.drawText("COMPLIANCE SUMMARY", MARGIN_LEFT, currentY, sectionHeaderPaint)
            currentY += 12f

            val summary = report.adherenceSummary
            val boxWidth = (CONTENT_WIDTH - 24f) / 4f
            val boxHeight = 44f

            val metricLabels = listOf(
                "Total Scheduled" to "${summary.totalScheduled} Doses",
                "Confirmed Taken" to "${summary.takenCount} (${String.format(Locale.US, "%.1f", summary.takenPercentage)}%)",
                "User Skipped" to "${summary.skippedCount} (${String.format(Locale.US, "%.1f", summary.skippedPercentage)}%)",
                "Unconfirmed" to "${summary.unconfirmedCount} (${String.format(Locale.US, "%.1f", summary.unconfirmedPercentage)}%)"
            )

            metricLabels.forEachIndexed { index, (label, value) ->
                val boxX = MARGIN_LEFT + (index * (boxWidth + 8f))
                val rect = RectF(boxX, currentY, boxX + boxWidth, currentY + boxHeight)
                canvas.drawRoundRect(rect, 4f, 4f, cardBgPaint)
                canvas.drawRoundRect(rect, 4f, 4f, cardBorderPaint)
                canvas.drawText(label, boxX + 8f, currentY + 16f, subtitlePaint)
                canvas.drawText(value, boxX + 8f, currentY + 34f, boldBodyPaint)
            }
            currentY += boxHeight + 10f

            // Overall Intake Rate & Late Responses Callout
            val subMetricRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 36f)
            canvas.drawRoundRect(subMetricRect, 4f, 4f, cardBgPaint)
            canvas.drawRoundRect(subMetricRect, 4f, 4f, cardBorderPaint)

            canvas.drawText(
                "User-Reported Adherence Rate: ${summary.adherencePercentage}%",
                MARGIN_LEFT + 12f,
                currentY + 18f,
                primaryPaint.apply { textSize = 11f }
            )
            canvas.drawText(
                "Denominator: ${summary.eligibleDoses} eligible doses (Taken ÷ (Taken + Skipped + Unconfirmed))",
                MARGIN_LEFT + 12f,
                currentY + 29f,
                subtitlePaint
            )

            val lateText = "Late Submissions: ${summary.lateCount} Doses (>60 mins past schedule)"
            canvas.drawText(lateText, MARGIN_RIGHT - 240f, currentY + 22f, bodyPaint)

            currentY += 46f

            // Notice / Disclaimer Box
            val disclaimerRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 28f)
            val disclaimerBgPaint = Paint().apply {
                color = Color.rgb(254, 243, 199) // Soft amber
                style = Paint.Style.FILL
            }
            val disclaimerTextPaint = Paint().apply {
                color = Color.rgb(146, 64, 14)
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawRoundRect(disclaimerRect, 4f, 4f, disclaimerBgPaint)
            canvas.drawText(
                "Notice: Figures represent self-reported device logs and do not constitute physical verification of intake.",
                MARGIN_LEFT + 8f,
                currentY + 12f,
                disclaimerTextPaint
            )
            canvas.drawText(
                "Unanswered doses are categorized strictly as Unconfirmed. Do not alter prescriptions without consulting a doctor.",
                MARGIN_LEFT + 8f,
                currentY + 22f,
                disclaimerTextPaint
            )
            currentY += 38f

            // --- MEDICINE REGIMEN BREAKDOWN TABLE ---
            if (report.medicineSummaries.isNotEmpty()) {
                checkAndAdvancePage(60f)
                canvas.drawText("MEDICATION REGIMEN BREAKDOWN", MARGIN_LEFT, currentY, sectionHeaderPaint)
                currentY += 10f

                // Table Header
                val medHeaderRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 18f)
                canvas.drawRect(medHeaderRect, tableHeaderPaint)
                canvas.drawText("Medicine Name", MARGIN_LEFT + 8f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Scheduled", MARGIN_LEFT + 220f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Taken", MARGIN_LEFT + 290f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Skipped", MARGIN_LEFT + 350f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Unconf.", MARGIN_LEFT + 410f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Late", MARGIN_LEFT + 470f, currentY + 12f, tableHeaderTextPaint)
                currentY += 18f

                report.medicineSummaries.forEachIndexed { idx, med ->
                    checkAndAdvancePage(18f)
                    if (idx % 2 == 1) {
                        canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 16f, tableRowAltPaint)
                    }
                    canvas.drawText(med.medicineName, MARGIN_LEFT + 8f, currentY + 11f, boldBodyPaint)
                    canvas.drawText("${med.totalScheduled}", MARGIN_LEFT + 220f, currentY + 11f, bodyPaint)
                    canvas.drawText("${med.takenCount} (${med.adherencePercentage}%)", MARGIN_LEFT + 290f, currentY + 11f, bodyPaint)
                    canvas.drawText("${med.skippedCount}", MARGIN_LEFT + 350f, currentY + 11f, bodyPaint)
                    canvas.drawText("${med.unconfirmedCount}", MARGIN_LEFT + 410f, currentY + 11f, bodyPaint)
                    canvas.drawText("${med.lateCount}", MARGIN_LEFT + 470f, currentY + 11f, bodyPaint)
                    canvas.drawLine(MARGIN_LEFT, currentY + 16f, MARGIN_RIGHT, currentY + 16f, linePaint)
                    currentY += 16f
                }
                currentY += 15f
            }

            // --- CHRONOLOGICAL DOSE LOG TABLE ---
            checkAndAdvancePage(60f)
            canvas.drawText("CHRONOLOGICAL DOSE LOG", MARGIN_LEFT, currentY, sectionHeaderPaint)
            currentY += 10f

            fun drawDoseTableHeader() {
                val doseHeaderRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 18f)
                canvas.drawRect(doseHeaderRect, tableHeaderPaint)
                canvas.drawText("Scheduled Time", MARGIN_LEFT + 8f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Medication", MARGIN_LEFT + 130f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Status", MARGIN_LEFT + 280f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Logged Time", MARGIN_LEFT + 360f, currentY + 12f, tableHeaderTextPaint)
                canvas.drawText("Notes / Flags", MARGIN_LEFT + 460f, currentY + 12f, tableHeaderTextPaint)
                currentY += 18f
            }

            drawDoseTableHeader()

            if (report.doseRecords.isEmpty()) {
                canvas.drawText("No dose records found in the selected date range.", MARGIN_LEFT + 8f, currentY + 14f, subtitlePaint)
                currentY += 20f
            } else {
                report.doseRecords.forEachIndexed { index, dose ->
                    checkAndAdvancePage(18f) {
                        drawDoseTableHeader()
                    }

                    if (index % 2 == 1) {
                        canvas.drawRect(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 16f, tableRowAltPaint)
                    }

                    // Scheduled Time
                    val scheduledStr = dateTimeFormat.format(Date(dose.scheduledAt))
                    canvas.drawText(scheduledStr, MARGIN_LEFT + 8f, currentY + 11f, bodyPaint)

                    // Medication name (truncate if too long)
                    val medDisplay = if (dose.medicineName.length > 24) dose.medicineName.take(22) + "..." else dose.medicineName
                    canvas.drawText(medDisplay, MARGIN_LEFT + 130f, currentY + 11f, boldBodyPaint)

                    // Status
                    val statusTextPaint = Paint().apply {
                        color = when (dose.status) {
                            DoseRecord.STATUS_TAKEN -> Color.rgb(22, 163, 74)
                            DoseRecord.STATUS_SKIPPED -> Color.rgb(220, 38, 38)
                            DoseRecord.STATUS_UNCONFIRMED -> Color.rgb(107, 114, 128)
                            else -> Color.rgb(217, 119, 6)
                        }
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 8.5f
                        isAntiAlias = true
                    }
                    canvas.drawText(dose.status, MARGIN_LEFT + 280f, currentY + 11f, statusTextPaint)

                    // Logged Time
                    val loggedStr = if (dose.recordedAt != null && dose.recordedAt > 0) {
                        timeFormat.format(Date(dose.recordedAt))
                    } else {
                        "–"
                    }
                    canvas.drawText(loggedStr, MARGIN_LEFT + 360f, currentY + 11f, bodyPaint)

                    // Flags
                    val flag = when {
                        dose.isLate -> "Late Entry (>60m)"
                        dose.scheduledAt > System.currentTimeMillis() -> "Future Scheduled"
                        else -> "On Time"
                    }
                    val flagPaint = Paint().apply {
                        color = if (dose.isLate) Color.rgb(185, 28, 28) else Color.rgb(109, 122, 119)
                        textSize = 8f
                        isAntiAlias = true
                    }
                    canvas.drawText(flag, MARGIN_LEFT + 460f, currentY + 11f, flagPaint)

                    canvas.drawLine(MARGIN_LEFT, currentY + 16f, MARGIN_RIGHT, currentY + 16f, linePaint)
                    currentY += 16f
                }
            }

            // Draw final page footer
            drawFooter(canvas, currentPageNumber, footerPaint)
            document.finishPage(page)

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int, paint: Paint) {
        val y = PAGE_HEIGHT - 24f
        val footerText = "MediTrack Compliance Report • Page $pageNumber"
        canvas.drawText(footerText, MARGIN_LEFT, y, paint)
        val disclaimerText = "Strictly Confidential Medical Ledger • Self-Reported Patient Data"
        canvas.drawText(disclaimerText, MARGIN_RIGHT - 260f, y, paint)
    }
}
