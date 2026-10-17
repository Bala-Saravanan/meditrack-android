package com.example.meditrack.ui.dose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DoseFragment : Fragment() {

    private lateinit var viewModel: DoseViewModel
    private lateinit var dateStripAdapter: DateStripAdapter
    private lateinit var doseAdapter: DoseAdapter

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dose, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = DoseViewModelFactory(app.doseRepository)
        viewModel = ViewModelProvider(this, factory)[DoseViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDosesForCurrentDate()
    }

    private fun setupViews(view: View) {
        val rvDateStrip = view.findViewById<RecyclerView>(R.id.rv_date_strip)
        rvDateStrip.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        dateStripAdapter = DateStripAdapter { selectedDate ->
            viewModel.setSelectedDate(selectedDate)
        }
        rvDateStrip.adapter = dateStripAdapter

        val rvDoses = view.findViewById<RecyclerView>(R.id.rv_doses)
        rvDoses.layoutManager = LinearLayoutManager(requireContext())
        doseAdapter = DoseAdapter(
            onItemClicked = { dose ->
                val bundle = bundleOf(
                    "scheduleId" to dose.scheduleId,
                    "doseId" to dose.id
                )
                findNavController().navigate(R.id.scheduleDoseHistoryFragment, bundle)
            },
            onTakenClicked = { dose ->
                viewModel.markDoseTaken(dose.id) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.toast_dose_taken, dose.medicineName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onSkippedClicked = { dose ->
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
        )
        rvDoses.adapter = doseAdapter

        // Calendar Picker Button
        val btnCalendar = view.findViewById<ImageButton>(R.id.btn_calendar_picker)
        btnCalendar.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(viewModel.selectedDateMillis.value)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selection?.let {
                    viewModel.setSelectedDate(it)
                }
            }
            picker.show(childFragmentManager, "DATE_PICKER")
        }

        // Jump to Today
        val btnToday = view.findViewById<MaterialButton>(R.id.btn_today_jump)
        btnToday.setOnClickListener {
            viewModel.setSelectedDate(System.currentTimeMillis())
        }

        // Status Filter Chips
        val cgFilters = view.findViewById<ChipGroup>(R.id.cg_dose_filters)
        cgFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chip_filter_all -> viewModel.setFilter(DoseFilter.ALL)
                R.id.chip_filter_upcoming -> viewModel.setFilter(DoseFilter.UPCOMING)
                R.id.chip_filter_taken -> viewModel.setFilter(DoseFilter.TAKEN)
                R.id.chip_filter_skipped -> viewModel.setFilter(DoseFilter.SKIPPED)
                R.id.chip_filter_unconfirmed -> viewModel.setFilter(DoseFilter.UNCONFIRMED)
            }
        }
    }

    private fun observeViewModel(view: View) {
        val tvMonthYear = view.findViewById<TextView>(R.id.tv_selected_month_year)
        val tvCountHeader = view.findViewById<TextView>(R.id.tv_dose_count_header)
        val rvDoses = view.findViewById<RecyclerView>(R.id.rv_doses)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_doses)

        val chipAll = view.findViewById<Chip>(R.id.chip_filter_all)
        val chipUpcoming = view.findViewById<Chip>(R.id.chip_filter_upcoming)
        val chipTaken = view.findViewById<Chip>(R.id.chip_filter_taken)
        val chipSkipped = view.findViewById<Chip>(R.id.chip_filter_skipped)
        val chipUnconfirmed = view.findViewById<Chip>(R.id.chip_filter_unconfirmed)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedDateMillis.collect { millis ->
                        tvMonthYear.text = monthYearFormat.format(Date(millis))
                    }
                }

                launch {
                    viewModel.dateStripItems.collect { items ->
                        dateStripAdapter.submitList(items)
                    }
                }

                launch {
                    viewModel.filterCounts.collect { counts ->
                        chipAll.text = "All (${counts.all})"
                        chipUpcoming.text = "Upcoming (${counts.upcoming})"
                        chipTaken.text = "Taken (${counts.taken})"
                        chipSkipped.text = "Skipped (${counts.skipped})"
                        chipUnconfirmed.text = "Unconfirmed (${counts.unconfirmed})"
                    }
                }

                launch {
                    viewModel.groupedDoseItems.collect { items ->
                        if (items.isEmpty()) {
                            rvDoses.visibility = View.GONE
                            layoutEmpty.visibility = View.VISIBLE
                            tvCountHeader.visibility = View.GONE
                        } else {
                            rvDoses.visibility = View.VISIBLE
                            layoutEmpty.visibility = View.GONE
                            tvCountHeader.visibility = View.VISIBLE
                            doseAdapter.submitList(items)
                        }
                    }
                }

                launch {
                    viewModel.totalDoseCount.collect { count ->
                        tvCountHeader.text = "$count ${if (count == 1) "Dose Scheduled" else "Doses Scheduled"}"
                    }
                }
            }
        }
    }
}
