package com.example.meditrack.ui.schedules

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
import com.example.meditrack.data.local.entity.Schedule
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SchedulesFragment : Fragment() {

    private lateinit var viewModel: SchedulesViewModel
    private lateinit var adapter: SchedulesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedules, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = SchedulesViewModelFactory(app.scheduleRepository, app.doseRepository)
        viewModel = ViewModelProvider(this, factory)[SchedulesViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btn_schedules_back)
        btnBack?.setOnClickListener {
            findNavController().navigateUp()
        }

        val btnSearch = view.findViewById<ImageButton>(R.id.btn_schedules_search)
        btnSearch?.setOnClickListener {
            Toast.makeText(requireContext(), "Search filters active", Toast.LENGTH_SHORT).show()
        }

        val btnLayout = view.findViewById<ImageButton>(R.id.btn_schedules_layout)
        btnLayout?.setOnClickListener {
            findNavController().navigate(R.id.dashboardFragment)
        }

        val btnCreate = view.findViewById<MaterialButton>(R.id.btn_create_schedule)
        btnCreate.setOnClickListener {
            findNavController().navigate(
                R.id.action_schedules_to_create,
                bundleOf("scheduleId" to -1L)
            )
        }

        val btnEmptyCreate = view.findViewById<MaterialButton>(R.id.btn_empty_create_schedule)
        btnEmptyCreate.setOnClickListener {
            findNavController().navigate(
                R.id.action_schedules_to_create,
                bundleOf("scheduleId" to -1L)
            )
        }

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filters)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_filter_active -> viewModel.setFilter(Schedule.STATUS_ACTIVE)
                R.id.chip_filter_completed -> viewModel.setFilter(Schedule.STATUS_COMPLETED)
                R.id.chip_filter_discontinued -> viewModel.setFilter(Schedule.STATUS_DISCONTINUED)
                else -> viewModel.setFilter("ALL")
            }
        }

        val rvSchedules = view.findViewById<RecyclerView>(R.id.rv_schedules)
        rvSchedules.layoutManager = LinearLayoutManager(requireContext())
        adapter = SchedulesAdapter { scheduleId ->
            findNavController().navigate(
                R.id.action_schedules_to_details,
                bundleOf("scheduleId" to scheduleId)
            )
        }
        rvSchedules.adapter = adapter
    }

    private fun observeViewModel(view: View) {
        val tvActiveCount = view.findViewById<TextView>(R.id.tv_active_schedules_count)
        val tvDailyAdherence = view.findViewById<TextView>(R.id.tv_daily_adherence_percent)
        val btnAvatar = view.findViewById<MaterialButton>(R.id.btn_schedules_avatar)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_schedules)
        val rvSchedules = view.findViewById<RecyclerView>(R.id.rv_schedules)

        val chipAll = view.findViewById<Chip>(R.id.chip_filter_all)
        val chipActive = view.findViewById<Chip>(R.id.chip_filter_active)
        val chipCompleted = view.findViewById<Chip>(R.id.chip_filter_completed)
        val chipDiscontinued = view.findViewById<Chip>(R.id.chip_filter_discontinued)

        val app = requireActivity().application as MediTrackApplication

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    app.userRepository.userProfile.collect { user ->
                        val initial = user?.fullName?.firstOrNull()?.uppercase() ?: "J"
                        btnAvatar?.text = initial
                    }
                }

                launch {
                    viewModel.activeCount.collect { count ->
                        tvActiveCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.dailyAdherencePercent.collect { percent ->
                        tvDailyAdherence.text = "$percent%"
                    }
                }

                launch {
                    combine(
                        viewModel.totalCount,
                        viewModel.activeCount,
                        viewModel.completedCount,
                        viewModel.discontinuedCount
                    ) { total, active, completed, discontinued ->
                        chipAll.text = "All ($total)"
                        chipActive.text = "Active ($active)"
                        chipCompleted.text = "Completed ($completed)"
                        chipDiscontinued.text = "Discontinued ($discontinued)"
                    }.collect {}
                }

                launch {
                    viewModel.filteredSchedules.collect { schedules ->
                        if (schedules.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                            rvSchedules.visibility = View.GONE
                        } else {
                            layoutEmpty.visibility = View.GONE
                            rvSchedules.visibility = View.VISIBLE
                            adapter.submitList(schedules)
                        }
                    }
                }
            }
        }
    }
}
