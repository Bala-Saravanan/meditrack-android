package com.example.meditrack.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
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
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ReportsFragment : Fragment() {

    private lateinit var viewModel: ReportsViewModel
    private lateinit var adapter: ScheduleReportCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = ReportsViewModelFactory(
            doseRepository = app.doseRepository,
            userRepository = app.userRepository
        )
        viewModel = ViewModelProvider(this, factory)[ReportsViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val rvCards = view.findViewById<RecyclerView>(R.id.rv_report_cards)
        rvCards.layoutManager = LinearLayoutManager(requireContext())
        adapter = ScheduleReportCardAdapter { scheduleId ->
            val bundle = Bundle().apply {
                putLong("scheduleId", scheduleId)
            }
            findNavController().navigate(R.id.action_reports_to_scheduleReport, bundle)
        }
        rvCards.adapter = adapter

        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_status)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_active -> viewModel.setStatusFilter("Active")
                R.id.chip_completed -> viewModel.setStatusFilter("Completed")
                R.id.chip_discontinued -> viewModel.setStatusFilter("Discontinued")
                else -> viewModel.setStatusFilter("ALL")
            }
        }
    }

    private fun observeViewModel(view: View) {
        val tvAvatar = view.findViewById<TextView>(R.id.tv_user_avatar)
        val pbLoading = view.findViewById<ProgressBar>(R.id.pb_reports_loading)
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_reports)
        val rvCards = view.findViewById<RecyclerView>(R.id.rv_report_cards)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    tvAvatar.text = state.userInitial
                    pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (!state.isLoading) {
                        if (state.cards.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                            rvCards.visibility = View.GONE
                        } else {
                            layoutEmpty.visibility = View.GONE
                            rvCards.visibility = View.VISIBLE
                            adapter.submitList(state.cards)
                        }
                    }
                }
            }
        }
    }
}
