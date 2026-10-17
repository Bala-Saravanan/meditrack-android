package com.example.meditrack.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.example.meditrack.data.local.MediTrackDatabase
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.data.repository.UserRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var reminderAdapter: UpcomingReminderAdapter
    private lateinit var summaryAdapter: RecentDoseSummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = DashboardViewModelFactory(
            app.userRepository,
            app.doseRepository,
            app.database
        )
        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val tvTodayBadge = view.findViewById<TextView>(R.id.tv_today_badge)
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        tvTodayBadge.text = "Today, " + dateFormat.format(Date())

        val btnReset = view.findViewById<ImageButton>(R.id.btn_reset_data)
        btnReset.setOnClickListener {
            showResetConfirmationDialog()
        }

        val btnAvatar = view.findViewById<Button>(R.id.btn_avatar)
        btnAvatar.setOnClickListener {
            showResetConfirmationDialog()
        }

        // Setup Upcoming Reminders RecyclerView
        val rvReminders = view.findViewById<RecyclerView>(R.id.rv_upcoming_reminders)
        rvReminders.layoutManager = LinearLayoutManager(requireContext())
        reminderAdapter = UpcomingReminderAdapter(
            onTakenClicked = { record ->
                viewModel.markDoseTaken(record.id)
                Toast.makeText(requireContext(), "${record.medicineName} logged as Taken", Toast.LENGTH_SHORT).show()
            },
            onSkippedClicked = { record ->
                viewModel.markDoseSkipped(record.id)
                Toast.makeText(requireContext(), "${record.medicineName} marked as Skipped", Toast.LENGTH_SHORT).show()
            },
            onItemClicked = { record ->
                val bundle = bundleOf(
                    "scheduleId" to record.scheduleId,
                    "doseId" to record.id
                )
                findNavController().navigate(R.id.action_dashboard_to_scheduleDoseHistory, bundle)
            }
        )
        rvReminders.adapter = reminderAdapter

        // Setup Recent Summaries RecyclerView
        val rvSummaries = view.findViewById<RecyclerView>(R.id.rv_recent_summaries)
        rvSummaries.layoutManager = LinearLayoutManager(requireContext())
        summaryAdapter = RecentDoseSummaryAdapter(
            onDetailsClicked = { summary ->
                val bundle = bundleOf(
                    "scheduleId" to summary.scheduleId,
                    "doseId" to -1L
                )
                findNavController().navigate(R.id.action_dashboard_to_scheduleDoseHistory, bundle)
            }
        )
        rvSummaries.adapter = summaryAdapter
    }

    private fun observeViewModel(view: View) {
        val tvGreetingName = view.findViewById<TextView>(R.id.tv_greeting_name)
        val tvGreetingSubtitle = view.findViewById<TextView>(R.id.tv_greeting_subtitle)
        val btnAvatar = view.findViewById<Button>(R.id.btn_avatar)
        val layoutEmptyReminders = view.findViewById<View>(R.id.layout_empty_reminders)
        val rvReminders = view.findViewById<RecyclerView>(R.id.rv_upcoming_reminders)
        val layoutEmptySummaries = view.findViewById<View>(R.id.layout_empty_summaries)
        val rvSummaries = view.findViewById<RecyclerView>(R.id.rv_recent_summaries)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userProfile.collect { user ->
                        val name = user?.fullName ?: "Patient"
                        val greeting = getGreetingTimeOfDay()
                        tvGreetingName.text = "$greeting, $name 👋"
                        btnAvatar.text = name.firstOrNull()?.uppercase() ?: "P"
                    }
                }

                launch {
                    viewModel.pendingReminders.collect { reminders ->
                        if (reminders.isEmpty()) {
                            layoutEmptyReminders.visibility = View.VISIBLE
                            rvReminders.visibility = View.GONE
                            tvGreetingSubtitle.text = getString(R.string.no_upcoming_reminders)
                        } else {
                            layoutEmptyReminders.visibility = View.GONE
                            rvReminders.visibility = View.VISIBLE
                            val count = reminders.size
                            tvGreetingSubtitle.text = "You have $count ${if (count == 1) "dose" else "doses"} scheduled for today."
                            reminderAdapter.submitList(reminders)
                        }
                    }
                }

                launch {
                    viewModel.recentSummaries.collect { summaries ->
                        if (summaries.isEmpty()) {
                            layoutEmptySummaries.visibility = View.VISIBLE
                            rvSummaries.visibility = View.GONE
                        } else {
                            layoutEmptySummaries.visibility = View.GONE
                            rvSummaries.visibility = View.VISIBLE
                            summaryAdapter.submitList(summaries)
                        }
                    }
                }
            }
        }
    }

    private fun getGreetingTimeOfDay(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.btn_clear_data)
            .setMessage(R.string.clear_demo_data_confirm)
            .setPositiveButton(R.string.clear_data_action) { _, _ ->
                viewModel.clearAllData {
                    Toast.makeText(requireContext(), "Database cleared successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_dashboard_to_welcome)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
