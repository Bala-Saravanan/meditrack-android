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
import com.example.meditrack.reminder.ReminderScheduler
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
            val app = requireActivity().application as MediTrackApplication
            viewLifecycleOwner.lifecycleScope.launch {
                ReminderScheduler.scheduleAllActiveReminders(app, app.database.scheduleDao())
            }
        }
        view.findViewById<MaterialButton>(R.id.btn_status_completed).setOnClickListener {
            viewModel.updateStatus(Schedule.STATUS_COMPLETED)
            val app = requireActivity().application as MediTrackApplication
            viewLifecycleOwner.lifecycleScope.launch {
                ReminderScheduler.scheduleAllActiveReminders(app, app.database.scheduleDao())
            }
        }
        view.findViewById<MaterialButton>(R.id.btn_status_discontinued).setOnClickListener {
            viewModel.updateStatus(Schedule.STATUS_DISCONTINUED)
            val app = requireActivity().application as MediTrackApplication
            viewLifecycleOwner.lifecycleScope.launch {
                ReminderScheduler.scheduleAllActiveReminders(app, app.database.scheduleDao())
            }
        }

        // RecyclerView
        val rvMedicines = view.findViewById<RecyclerView>(R.id.rv_detail_medicines)
        rvMedicines.layoutManager = LinearLayoutManager(requireContext())
        adapter = ScheduleDetailsAdapter()
        rvMedicines.adapter = adapter
    }

    private fun observeViewModel(view: View) {
        val tvProtocolId = view.findViewById<TextView>(R.id.tv_detail_protocol_id)
        val tvName = view.findViewById<TextView>(R.id.tv_detail_schedule_name)
        val tvStatus = view.findViewById<TextView>(R.id.tv_detail_status)
        val tvDates = view.findViewById<TextView>(R.id.tv_detail_date_range)
        val tvNotes = view.findViewById<TextView>(R.id.tv_detail_notes)
        val layoutPhysician = view.findViewById<View>(R.id.layout_detail_physician)
        val tvPhysician = view.findViewById<TextView>(R.id.tv_detail_physician)
        val layoutAdherence = view.findViewById<View>(R.id.layout_detail_adherence)
        val tvAdherence = view.findViewById<TextView>(R.id.tv_detail_adherence)
        val tvPrescriptionsCount = view.findViewById<TextView>(R.id.tv_detail_prescriptions_count)
        val tvNoMeds = view.findViewById<TextView>(R.id.tv_detail_no_medicines)
        val ivNotifications = view.findViewById<ImageView>(R.id.iv_detail_notifications_check)
        val tvNotificationsTitle = view.findViewById<TextView>(R.id.tv_detail_notifications_title)
        val tvNotificationsDesc = view.findViewById<TextView>(R.id.tv_detail_notifications_desc)

        val app = requireActivity().application as MediTrackApplication

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scheduleDetails.collect { details ->
                    if (details == null) return@collect
                    val schedule = details.schedule

                    tvProtocolId.text = "CARE PROTOCOL #CV-${schedule.id}"
                    tvName.text = schedule.name

                    // Status
                    tvStatus.text = "✔ ${schedule.status}"
                    when (schedule.status) {
                        Schedule.STATUS_ACTIVE -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_pill_status_active)
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_tertiary_fixed))
                        }
                        Schedule.STATUS_COMPLETED -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_pill_status_completed)
                            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                        }
                        else -> {
                            tvStatus.setBackgroundResource(R.drawable.bg_pill_status_default)
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
                        tvPhysician.text = if (schedule.physicianName.startsWith("Dr.", ignoreCase = true)) {
                            schedule.physicianName
                        } else {
                            "Dr. ${schedule.physicianName}"
                        }
                        layoutPhysician.visibility = View.VISIBLE
                    } else {
                        layoutPhysician.visibility = View.GONE
                    }

                    // Calculate real adherence score for this schedule
                    try {
                        val doses = app.database.doseRecordDao().getDosesForScheduleOnce(schedule.id)
                        val total = doses.size
                        val taken = doses.count { it.status == com.example.meditrack.data.local.entity.DoseRecord.STATUS_TAKEN }
                        val adherencePct = if (total > 0) (taken * 100) / total else 100
                        tvAdherence.text = "$adherencePct% (30 Days)"
                        layoutAdherence.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        layoutAdherence.visibility = View.GONE
                    }

                    // Notifications
                    if (schedule.notificationsEnabled) {
                        tvNotificationsTitle.text = "Notification Status"
                        tvNotificationsDesc.text = "Alarms active (device local notifications enabled)"
                        ivNotifications.setImageResource(R.drawable.ic_check_circle)
                        ivNotifications.setColorFilter(ContextCompat.getColor(requireContext(), R.color.tertiary))
                    } else {
                        tvNotificationsTitle.text = "Notification Status"
                        tvNotificationsDesc.text = "Alarms disabled for this schedule"
                        ivNotifications.setImageResource(R.drawable.ic_close)
                        ivNotifications.setColorFilter(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
                    }

                    // Medicines
                    val activeMedicines = details.medicinesWithTimes.filter { it.scheduleMedicine.isActive }
                    tvPrescriptionsCount.text = "${activeMedicines.size} ${if (activeMedicines.size == 1) "Prescription" else "Prescriptions"}"

                    if (activeMedicines.isEmpty()) {
                        tvNoMeds.visibility = View.VISIBLE
                    } else {
                        tvNoMeds.visibility = View.GONE
                    }
                    adapter.submitList(activeMedicines) {
                        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll_schedule_details)?.scrollTo(0, 0)
                    }
                }
            }
        }
    }
}
