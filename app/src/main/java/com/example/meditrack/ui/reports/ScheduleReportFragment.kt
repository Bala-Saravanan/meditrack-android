package com.example.meditrack.ui.reports

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.example.meditrack.data.local.model.DateRangeType
import com.example.meditrack.data.local.model.ScheduleDetailedReport
import com.example.meditrack.report.PdfReportGenerator
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleReportFragment : Fragment() {

    private var scheduleId: Long = -1L
    private lateinit var viewModel: ScheduleReportViewModel

    private lateinit var medicineAdapter: MedicineReportAdapter
    private lateinit var doseAdapter: ReportDoseAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            savePdfToUri(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleId = arguments?.getLong("scheduleId") ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = ScheduleReportViewModelFactory(
            scheduleId = scheduleId,
            doseRepository = app.doseRepository,
            caregiverRepository = app.caregiverRepository
        )
        viewModel = ViewModelProvider(this, factory)[ScheduleReportViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btn_report_back)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Medicine breakdown recycler
        val rvMeds = view.findViewById<RecyclerView>(R.id.rv_medicine_breakdown)
        rvMeds.layoutManager = LinearLayoutManager(requireContext())
        medicineAdapter = MedicineReportAdapter()
        rvMeds.adapter = medicineAdapter

        // Chronological doses recycler
        val rvDoses = view.findViewById<RecyclerView>(R.id.rv_report_doses)
        rvDoses.layoutManager = LinearLayoutManager(requireContext())
        doseAdapter = ReportDoseAdapter()
        rvDoses.adapter = doseAdapter

        // Date range chips
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_range)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_range_7_days -> viewModel.setRangeType(DateRangeType.LAST_7_DAYS)
                R.id.chip_range_30_days -> viewModel.setRangeType(DateRangeType.LAST_30_DAYS)
                R.id.chip_range_entire -> viewModel.setRangeType(DateRangeType.ENTIRE_PERIOD)
                R.id.chip_range_custom -> showCustomDateRangePicker()
            }
        }

        // Action buttons
        val btnDownload = view.findViewById<MaterialButton>(R.id.btn_download_pdf)
        btnDownload.setOnClickListener {
            launchDownloadFlow()
        }

        val btnShare = view.findViewById<MaterialButton>(R.id.btn_share_pdf)
        btnShare.setOnClickListener {
            launchShareFlow()
        }

        val btnQuickShare = view.findViewById<ImageButton>(R.id.btn_quick_share)
        btnQuickShare.setOnClickListener {
            launchShareFlow()
        }
    }

    private fun showCustomDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Custom Reporting Range")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            if (selection != null) {
                val start = selection.first
                val end = selection.second
                viewModel.setCustomRange(start, end)
            }
        }
        picker.show(childFragmentManager, "CUSTOM_DATE_RANGE")
    }

    private fun observeViewModel(view: View) {
        val tvTopScheduleName = view.findViewById<TextView>(R.id.tv_top_schedule_name)
        val tvRegimenBadge = view.findViewById<TextView>(R.id.tv_regimen_badge)
        val tvReportScheduleName = view.findViewById<TextView>(R.id.tv_report_schedule_name)
        val tvReportPeriod = view.findViewById<TextView>(R.id.tv_report_period)
        val tvReportGenTime = view.findViewById<TextView>(R.id.tv_report_generated_time)

        val tvOverallPct = view.findViewById<TextView>(R.id.tv_overall_compliance_percent)
        val barTaken = view.findViewById<View>(R.id.view_report_bar_taken)
        val barSkipped = view.findViewById<View>(R.id.view_report_bar_skipped)
        val barUnconf = view.findViewById<View>(R.id.view_report_bar_unconfirmed)

        val tvBarTakenPct = view.findViewById<TextView>(R.id.tv_bar_taken_pct)
        val tvBarSkippedPct = view.findViewById<TextView>(R.id.tv_bar_skipped_pct)
        val tvBarUnconfPct = view.findViewById<TextView>(R.id.tv_bar_unconf_pct)

        val tvTotalScheduled = view.findViewById<TextView>(R.id.tv_total_scheduled_count)
        val tvConfirmedTaken = view.findViewById<TextView>(R.id.tv_confirmed_taken_count)
        val tvUserSkipped = view.findViewById<TextView>(R.id.tv_user_skipped_count)
        val tvUnconfirmed = view.findViewById<TextView>(R.id.tv_unconfirmed_count)
        val tvLateDoses = view.findViewById<TextView>(R.id.tv_late_dose_count)

        val cardFutureNotice = view.findViewById<MaterialCardView>(R.id.card_future_doses_notice)
        val tvEmptyDoses = view.findViewById<TextView>(R.id.tv_empty_doses)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.report.collect { report ->
                    if (report == null) return@collect
                    bindReportData(
                        report,
                        tvTopScheduleName,
                        tvRegimenBadge,
                        tvReportScheduleName,
                        tvReportPeriod,
                        tvReportGenTime,
                        tvOverallPct,
                        barTaken,
                        barSkipped,
                        barUnconf,
                        tvBarTakenPct,
                        tvBarSkippedPct,
                        tvBarUnconfPct,
                        tvTotalScheduled,
                        tvConfirmedTaken,
                        tvUserSkipped,
                        tvUnconfirmed,
                        tvLateDoses,
                        cardFutureNotice,
                        tvEmptyDoses
                    )
                }
            }
        }
    }

    private fun bindReportData(
        report: ScheduleDetailedReport,
        tvTopScheduleName: TextView,
        tvRegimenBadge: TextView,
        tvReportScheduleName: TextView,
        tvReportPeriod: TextView,
        tvReportGenTime: TextView,
        tvOverallPct: TextView,
        barTaken: View,
        barSkipped: View,
        barUnconf: View,
        tvBarTakenPct: TextView,
        tvBarSkippedPct: TextView,
        tvBarUnconfPct: TextView,
        tvTotalScheduled: TextView,
        tvConfirmedTaken: TextView,
        tvUserSkipped: TextView,
        tvUnconfirmed: TextView,
        tvLateDoses: TextView,
        cardFutureNotice: MaterialCardView,
        tvEmptyDoses: TextView
    ) {
        tvTopScheduleName.text = report.scheduleName
        tvReportScheduleName.text = report.scheduleName
        tvRegimenBadge.text = "${report.scheduleStatus} Regimen"

        val startStr = dateFormat.format(Date(report.startTimestamp))
        val endStr = dateFormat.format(Date(report.endTimestamp))
        tvReportPeriod.text = "Report Period: $startStr – $endStr (${report.dateRangeType.label})"
        tvReportGenTime.text = "Generated on ${dateTimeFormat.format(Date())} (Device Local Time)"

        val summary = report.adherenceSummary
        tvOverallPct.text = "${summary.adherencePercentage}%"

        val eligible = summary.eligibleDoses
        if (eligible > 0) {
            val takenWeight = (summary.takenCount.toFloat() / eligible.toFloat()) * 100f
            val skippedWeight = (summary.skippedCount.toFloat() / eligible.toFloat()) * 100f
            val unconfWeight = (summary.unconfirmedCount.toFloat() / eligible.toFloat()) * 100f

            setBarWeight(barTaken, takenWeight)
            setBarWeight(barSkipped, skippedWeight)
            setBarWeight(barUnconf, unconfWeight)
        } else {
            setBarWeight(barTaken, 0f)
            setBarWeight(barSkipped, 0f)
            setBarWeight(barUnconf, 100f)
        }

        tvBarTakenPct.text = "${String.format(Locale.US, "%.1f", summary.takenPercentage)}% Taken"
        tvBarSkippedPct.text = "${String.format(Locale.US, "%.1f", summary.skippedPercentage)}% Skipped"
        tvBarUnconfPct.text = "${String.format(Locale.US, "%.1f", summary.unconfirmedPercentage)}% Unconf."

        tvTotalScheduled.text = "${summary.totalScheduled} Doses"
        tvConfirmedTaken.text = "${summary.takenCount} (${String.format(Locale.US, "%.1f", summary.takenPercentage)}%)"
        tvUserSkipped.text = "${summary.skippedCount} (${String.format(Locale.US, "%.1f", summary.skippedPercentage)}%)"
        tvUnconfirmed.text = "${summary.unconfirmedCount} (${String.format(Locale.US, "%.1f", summary.unconfirmedPercentage)}%)"
        tvLateDoses.text = "${summary.lateCount} Doses"

        cardFutureNotice.visibility = if (report.hasFutureScheduledDoses) View.VISIBLE else View.GONE

        // Submit lists
        medicineAdapter.submitList(report.medicineSummaries)

        if (report.doseRecords.isEmpty()) {
            tvEmptyDoses.visibility = View.VISIBLE
        } else {
            tvEmptyDoses.visibility = View.GONE
        }
        doseAdapter.submitList(report.doseRecords)
    }

    private fun setBarWeight(view: View, weight: Float) {
        val params = view.layoutParams as LinearLayout.LayoutParams
        params.weight = weight.coerceAtLeast(0f)
        view.layoutParams = params
        view.visibility = if (weight > 0f) View.VISIBLE else View.GONE
    }

    private fun launchDownloadFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentReport = viewModel.getReportOnce()
            if (currentReport == null) {
                Toast.makeText(requireContext(), "No report data available to export.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val sanitized = currentReport.scheduleName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val defaultFileName = "MediTrack_${sanitized}_Report.pdf"
            createDocumentLauncher.launch(defaultFileName)
        }
    }

    private fun savePdfToUri(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentReport = viewModel.getReportOnce()
            if (currentReport != null) {
                try {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        PdfReportGenerator.generatePdfReport(currentReport, outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "PDF report saved successfully.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun launchShareFlow() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentReport = viewModel.getReportOnce()
            if (currentReport == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No report data available to share.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val file = PdfReportGenerator.generatePdfReportToFile(requireContext(), currentReport)
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )

                val caregiver = viewModel.caregiver.value
                val shareSubject = "MediTrack Compliance Report: ${currentReport.scheduleName}"
                val shareBody = buildString {
                    append("MediTrack Compliance Report for ${currentReport.scheduleName}\n")
                    append("Reporting Period: ${dateFormat.format(Date(currentReport.startTimestamp))} – ${dateFormat.format(Date(currentReport.endTimestamp))}\n")
                    append("Self-Reported Adherence Rate: ${currentReport.adherenceSummary.adherencePercentage}%\n")
                    if (caregiver != null) {
                        append("Designated Caregiver: ${caregiver.fullName} (${caregiver.relationship})\n")
                    }
                    append("\nPlease find the attached official medication compliance report (PDF).")
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                    putExtra(Intent.EXTRA_TEXT, shareBody)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    startActivity(Intent.createChooser(shareIntent, "Share Compliance Report"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to prepare PDF for sharing: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
