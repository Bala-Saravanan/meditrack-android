package com.example.meditrack.ui.dose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleDoseHistoryFragment : Fragment() {

    private var scheduleId: Long = -1L
    private var doseId: Long = -1L

    private lateinit var viewModel: ScheduleDoseHistoryViewModel
    private lateinit var historyAdapter: DoseHistoryAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleId = arguments?.getLong("scheduleId") ?: -1L
        doseId = arguments?.getLong("doseId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_dose_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = ScheduleDoseHistoryViewModelFactory(
            scheduleId = scheduleId,
            targetDoseId = doseId,
            doseRepository = app.doseRepository
        )
        viewModel = ViewModelProvider(this, factory)[ScheduleDoseHistoryViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btn_history_back)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_schedule_doses)
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = DoseHistoryAdapter(
            highlightedDoseId = doseId,
            onTakenClicked = { record ->
                viewModel.markDoseTaken(record.id) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_dose_taken, record.medicineName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onSkippedClicked = { record ->
                viewModel.markDoseSkipped(record.id) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_dose_skipped, record.medicineName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
        rvHistory.adapter = historyAdapter
    }

    private fun observeViewModel(view: View) {
        val tvRegimenBadge = view.findViewById<TextView>(R.id.tv_schedule_regimen_badge)
        val tvTitle = view.findViewById<TextView>(R.id.tv_history_schedule_title)
        val tvMeta = view.findViewById<TextView>(R.id.tv_history_schedule_meta)
        val tvDates = view.findViewById<TextView>(R.id.tv_history_schedule_dates)

        val cardHighlight = view.findViewById<MaterialCardView>(R.id.card_highlighted_dose)
        val tvHighlightLabel = view.findViewById<TextView>(R.id.tv_highlight_label)
        val tvHighlightStatus = view.findViewById<TextView>(R.id.tv_highlight_status)
        val tvHighlightMedName = view.findViewById<TextView>(R.id.tv_highlight_med_name)
        val tvHighlightFood = view.findViewById<TextView>(R.id.tv_highlight_food_instruction)
        val layoutHighlightActions = view.findViewById<View>(R.id.layout_highlight_actions)
        val btnHighlightTake = view.findViewById<MaterialButton>(R.id.btn_highlight_take)
        val btnHighlightSkip = view.findViewById<MaterialButton>(R.id.btn_highlight_skip)

        val rvHistory = view.findViewById<RecyclerView>(R.id.rv_schedule_doses)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_history)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.schedule.collect { schedule ->
                        if (schedule != null) {
                            tvTitle.text = schedule.name
                            val physician = if (schedule.physicianName.isNotBlank()) "Dr. ${schedule.physicianName}" else "Self-Managed Routine"
                            val notes = if (schedule.notes.isNotBlank()) " • ${schedule.notes}" else ""
                            tvMeta.text = physician + notes

                            val startStr = dateFormat.format(Date(schedule.startDate))
                            val endStr = if (schedule.endDate != null) dateFormat.format(Date(schedule.endDate)) else getString(R.string.ongoing)
                            tvDates.text = "$startStr – $endStr"

                            when (schedule.status) {
                                Schedule.STATUS_ACTIVE -> {
                                    tvRegimenBadge.text = getString(R.string.active_regimen)
                                    tvRegimenBadge.setBackgroundResource(R.drawable.bg_status_pill_taken)
                                    tvRegimenBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_taken))
                                }
                                Schedule.STATUS_COMPLETED -> {
                                    tvRegimenBadge.text = getString(R.string.filter_completed)
                                    tvRegimenBadge.setBackgroundResource(R.drawable.bg_status_pill_pending)
                                    tvRegimenBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
                                }
                                else -> {
                                    tvRegimenBadge.text = getString(R.string.filter_discontinued)
                                    tvRegimenBadge.setBackgroundResource(R.drawable.bg_status_pill_skipped)
                                    tvRegimenBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_skipped))
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.highlightedDose.collect { dose ->
                        if (dose != null) {
                            cardHighlight.visibility = View.VISIBLE
                            val timeStr = timeFormat.format(Date(dose.scheduledAt))
                            tvHighlightLabel.text = "Target Dose • $timeStr"
                            tvHighlightMedName.text = dose.medicineName

                            val foodText = when (dose.foodInstruction) {
                                "BEFORE_FOOD" -> getString(R.string.food_before)
                                "WITH_FOOD" -> getString(R.string.food_with)
                                "AFTER_FOOD" -> getString(R.string.food_after)
                                else -> getString(R.string.food_no_restriction)
                            }
                            tvHighlightFood.text = "${dose.period.replaceFirstChar { it.titlecase(Locale.getDefault()) }} • $foodText"

                            tvHighlightStatus.text = dose.status
                            when (dose.status) {
                                DoseRecord.STATUS_TAKEN -> {
                                    tvHighlightStatus.setBackgroundResource(R.drawable.bg_status_pill_taken)
                                    tvHighlightStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_taken))
                                    layoutHighlightActions.visibility = View.GONE
                                }
                                DoseRecord.STATUS_SKIPPED -> {
                                    tvHighlightStatus.setBackgroundResource(R.drawable.bg_status_pill_skipped)
                                    tvHighlightStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_skipped))
                                    layoutHighlightActions.visibility = View.GONE
                                }
                                DoseRecord.STATUS_UNCONFIRMED -> {
                                    tvHighlightStatus.setBackgroundResource(R.drawable.bg_status_pill_unconfirmed)
                                    tvHighlightStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_unconfirmed))
                                    layoutHighlightActions.visibility = View.VISIBLE
                                }
                                else -> {
                                    tvHighlightStatus.setBackgroundResource(R.drawable.bg_status_pill_pending)
                                    tvHighlightStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_pending))
                                    layoutHighlightActions.visibility = View.VISIBLE
                                }
                            }

                            btnHighlightTake.setOnClickListener {
                                viewModel.markDoseTaken(dose.id) {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.toast_dose_taken, dose.medicineName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            btnHighlightSkip.setOnClickListener {
                                viewModel.markDoseSkipped(dose.id) {
                                    requireActivity().runOnUiThread {
                                        Toast.makeText(
                                            requireContext(),
                                            getString(R.string.toast_dose_skipped, dose.medicineName),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            cardHighlight.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.historyDoses.collect { doses ->
                        if (doses.isEmpty()) {
                            rvHistory.visibility = View.GONE
                            layoutEmpty.visibility = View.VISIBLE
                        } else {
                            rvHistory.visibility = View.VISIBLE
                            layoutEmpty.visibility = View.GONE
                            historyAdapter.submitList(doses)
                        }
                    }
                }
            }
        }
    }
}
