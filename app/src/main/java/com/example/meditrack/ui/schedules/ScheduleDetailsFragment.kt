package com.example.meditrack.ui.schedules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleDetailsFragment : Fragment() {

    private var scheduleId: Long = -1L
    private lateinit var viewModel: ScheduleDetailsViewModel
    private lateinit var adapter: ScheduleDetailsAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleId = arguments?.getLong("scheduleId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = ScheduleDetailsViewModelFactory(scheduleId, app.scheduleRepository)
        viewModel = ViewModelProvider(this, factory)[ScheduleDetailsViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_details)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val navigateToEdit = {
            findNavController().navigate(
                R.id.action_details_to_edit,
                bundleOf("scheduleId" to scheduleId)
            )
        }

        view.findViewById<MaterialButton>(R.id.btn_edit_schedule_top).setOnClickListener { navigateToEdit() }
        view.findViewById<MaterialButton>(R.id.btn_edit_schedule_bottom).setOnClickListener { navigateToEdit() }
        view.findViewById<MaterialButton>(R.id.btn_view_dose_history).setOnClickListener {
            findNavController().navigate(
                R.id.action_details_to_history,
                bundleOf("scheduleId" to scheduleId, "doseId" to -1L)
            )
        }

        // Status update buttons
        view.findViewById<MaterialButton>(R.id.btn_status_active).setOnClickListener {
            viewModel.updateStatus(Schedule.STATUS_ACTIVE)
        }
        view.findViewById<MaterialButton>(R.id.btn_status_completed).setOnClickListener {
            viewModel.updateStatus(Schedule.STATUS_COMPLETED)
        }
        view.findViewById<MaterialButton>(R.id.btn_status_discontinued).setOnClickListener {
            viewModel.updateStatus(Schedule.STATUS_DISCONTINUED)
        }

        // RecyclerView
        val rvMedicines = view.findViewById<RecyclerView>(R.id.rv_detail_medicines)
        rvMedicines.layoutManager = LinearLayoutManager(requireContext())
        adapter = ScheduleDetailsAdapter()
        rvMedicines.adapter = adapter
    }

    private fun observeViewModel(view: View) {
        val tvName = view.findViewById<TextView>(R.id.tv_detail_schedule_name)
        val tvStatus = view.findViewById<TextView>(R.id.tv_detail_status)
        val tvDates = view.findViewById<TextView>(R.id.tv_detail_date_range)
        val tvNotes = view.findViewById<TextView>(R.id.tv_detail_notes)
        val tvPhysician = view.findViewById<TextView>(R.id.tv_detail_physician)
        val tvNoMeds = view.findViewById<TextView>(R.id.tv_detail_no_medicines)
        val ivNotifications = view.findViewById<ImageView>(R.id.iv_detail_notifications_check)
        val tvNotificationsTitle = view.findViewById<TextView>(R.id.tv_detail_notifications_title)
        val tvNotificationsDesc = view.findViewById<TextView>(R.id.tv_detail_notifications_desc)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scheduleDetails.collect { details ->
                    if (details == null) return@collect
                    val schedule = details.schedule

                    tvName.text = schedule.name

                    // Status
                    tvStatus.text = schedule.status
                    when (schedule.status) {
                        Schedule.STATUS_ACTIVE -> {
                            tvStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.tertiary_fixed)
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.tertiary))
                        }
                        Schedule.STATUS_COMPLETED -> {
                            tvStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_fixed)
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                        }
                        else -> {
                            tvStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.surface_container_high)
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
                        }
                    }

                    // Dates
                    val startStr = dateFormat.format(Date(schedule.startDate))
                    val endStr = if (schedule.endDate != null && schedule.endDate > 0) {
                        dateFormat.format(Date(schedule.endDate))
                    } else {
                        getString(R.string.ongoing)
                    }
                    tvDates.text = "$startStr – $endStr"

                    // Notes
                    if (schedule.notes.isNotBlank()) {
                        tvNotes.text = schedule.notes
                        tvNotes.visibility = View.VISIBLE
                    } else {
                        tvNotes.visibility = View.GONE
                    }

                    // Physician
                    if (schedule.physicianName.isNotBlank()) {
                        tvPhysician.text = "Prescribed by: Dr. ${schedule.physicianName}"
                        tvPhysician.visibility = View.VISIBLE
                    } else {
                        tvPhysician.visibility = View.GONE
                    }

                    // Notifications
                    if (schedule.notificationsEnabled) {
                        tvNotificationsTitle.text = "Notification Reminders Active"
                        tvNotificationsDesc.text = getString(R.string.notifications_active_desc)
                        ivNotifications.setImageResource(R.drawable.ic_check_circle)
                        ivNotifications.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tertiary))
                    } else {
                        tvNotificationsTitle.text = "Notification Reminders Muted"
                        tvNotificationsDesc.text = "Alarms disabled for this schedule"
                        ivNotifications.setImageResource(R.drawable.ic_close)
                        ivNotifications.setColorFilter(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
                    }

                    // Medicines
                    val activeMedicines = details.medicinesWithTimes.filter { it.scheduleMedicine.isActive }
                    if (activeMedicines.isEmpty()) {
                        tvNoMeds.visibility = View.VISIBLE
                    } else {
                        tvNoMeds.visibility = View.GONE
                    }
                    adapter.submitList(activeMedicines)
                }
            }
        }
    }
}
