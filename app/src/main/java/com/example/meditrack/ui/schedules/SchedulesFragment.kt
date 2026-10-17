package com.example.meditrack.ui.schedules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.google.android.material.chip.ChipGroup
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
        val factory = SchedulesViewModelFactory(app.scheduleRepository)
        viewModel = ViewModelProvider(this, factory)[SchedulesViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
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
        val tvTotalCount = view.findViewById<TextView>(R.id.tv_total_schedules_count)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_schedules)
        val rvSchedules = view.findViewById<RecyclerView>(R.id.rv_schedules)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activeCount.collect { count ->
                        tvActiveCount.text = count.toString()
                    }
                }

                launch {
                    viewModel.totalCount.collect { count ->
                        tvTotalCount.text = count.toString()
                    }
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
