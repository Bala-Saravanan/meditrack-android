package com.example.meditrack.ui.schedules

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateScheduleFragment : Fragment() {

    private var scheduleId: Long = -1L
    private lateinit var viewModel: CreateScheduleViewModel
    private lateinit var draftAdapter: DraftMedicineAdapter

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var availableMedicinesList: List<Medicine> = emptyList()
    private var selectedMedicine: Medicine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleId = arguments?.getLong("scheduleId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = CreateScheduleViewModelFactory(app.scheduleRepository, app.medicineRepository)
        viewModel = ViewModelProvider(this, factory)[CreateScheduleViewModel::class.java]

        setupViews(view)
        observeViewModel(view)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.initialize(scheduleId)
            populateInitialForm(view)
        }

        // Listen for new medicine created in AddMedicineFragment
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Long>("new_medicine_id")?.observe(viewLifecycleOwner) { newMedicineId ->
            if (newMedicineId != null && newMedicineId > 0) {
                savedStateHandle.remove<Long>("new_medicine_id")
                viewLifecycleOwner.lifecycleScope.launch {
                    val added = viewModel.addMedicineById(newMedicineId)
                    if (added) {
                        Toast.makeText(requireContext(), "New medicine added to schedule", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_create_title)
        if (scheduleId > 0) {
            tvTitle.text = getString(R.string.edit_schedule)
        } else {
            tvTitle.text = getString(R.string.new_schedule)
        }

        val navigateBack = {
            findNavController().popBackStack()
        }

        view.findViewById<ImageButton>(R.id.btn_back_create).setOnClickListener { navigateBack() }
        view.findViewById<MaterialButton>(R.id.btn_cancel_top).setOnClickListener { navigateBack() }
        view.findViewById<MaterialButton>(R.id.btn_cancel_schedule).setOnClickListener { navigateBack() }

        // Schedule details inputs
        val etName = view.findViewById<TextInputEditText>(R.id.et_schedule_name)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.et_end_date)
        val etPhysician = view.findViewById<TextInputEditText>(R.id.et_physician_name)
        val etNotes = view.findViewById<TextInputEditText>(R.id.et_schedule_notes)
        val switchNotifications = view.findViewById<MaterialSwitch>(R.id.switch_schedule_notifications)

        etName.doAfterTextChanged { viewModel.scheduleName = it?.toString().orEmpty() }
        etPhysician.doAfterTextChanged { viewModel.physicianName = it?.toString().orEmpty() }
        etNotes.doAfterTextChanged { viewModel.notes = it?.toString().orEmpty() }
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.notificationsEnabled = isChecked
        }

        // Date Pickers
        etStartDate.setOnClickListener {
            showDatePicker(viewModel.startDate) { selectedMillis ->
                viewModel.startDate = selectedMillis
                etStartDate.setText(dateFormat.format(Date(selectedMillis)))
            }
        }

        etEndDate.setOnClickListener {
            showDatePicker(viewModel.endDate ?: viewModel.startDate) { selectedMillis ->
                viewModel.endDate = selectedMillis
                etEndDate.setText(dateFormat.format(Date(selectedMillis)))
            }
        }

        view.findViewById<TextInputLayout>(R.id.til_end_date).setEndIconOnClickListener {
            viewModel.endDate = null
            etEndDate.setText("")
        }

        // Medicines Dropdown
        val actvSelectMed = view.findViewById<AutoCompleteTextView>(R.id.actv_select_medicine)
        actvSelectMed.setOnItemClickListener { _, _, position, _ ->
            if (position in availableMedicinesList.indices) {
                selectedMedicine = availableMedicinesList[position]
            }
        }

        // Add selected medicine to draft
        view.findViewById<MaterialButton>(R.id.btn_add_medicine_to_draft).setOnClickListener {
            val med = selectedMedicine
            if (med == null) {
                Toast.makeText(requireContext(), "Please select a medicine first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val added = viewModel.addMedicine(med)
            if (!added) {
                Toast.makeText(requireContext(), getString(R.string.error_duplicate_medicine), Toast.LENGTH_SHORT).show()
            } else {
                actvSelectMed.setText("", false)
                selectedMedicine = null
            }
        }

        // Open Screen 7: Add New Medicine
        view.findViewById<MaterialButton>(R.id.btn_open_add_medicine_form).setOnClickListener {
            findNavController().navigate(R.id.action_createSchedule_to_addMedicine)
        }

        // Draft Medicines RecyclerView
        val rvDrafts = view.findViewById<RecyclerView>(R.id.rv_draft_medicines)
        rvDrafts.layoutManager = LinearLayoutManager(requireContext())
        draftAdapter = DraftMedicineAdapter(
            onRemoveClicked = { medId ->
                viewModel.removeMedicine(medId)
            },
            onTimePickerRequested = { draft, period ->
                val currentMinutes = when (period) {
                    ScheduleMedicineTime.PERIOD_MORNING -> draft.morningMinutes
                    ScheduleMedicineTime.PERIOD_AFTERNOON -> draft.afternoonMinutes
                    ScheduleMedicineTime.PERIOD_EVENING -> draft.eveningMinutes
                    else -> draft.nightMinutes
                }
                showTimePicker(currentMinutes) { newMinutes ->
                    viewModel.updateSlotTime(draft.medicineId, period, newMinutes)
                }
            }
        )
        rvDrafts.adapter = draftAdapter

        // Save Schedule Button
        val tilName = view.findViewById<TextInputLayout>(R.id.til_schedule_name)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_schedule)
        btnSave.setOnClickListener {
            tilName.error = null
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.validateAndSave()
                result.onSuccess {
                    Toast.makeText(requireContext(), "Schedule saved successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }.onFailure { ex ->
                    val msg = ex.message ?: "Failed to save schedule"
                    if (msg.contains("name", ignoreCase = true)) {
                        tilName.error = msg
                        etName.requestFocus()
                    } else {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun populateInitialForm(view: View) {
        val etName = view.findViewById<TextInputEditText>(R.id.et_schedule_name)
        val etStartDate = view.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = view.findViewById<TextInputEditText>(R.id.et_end_date)
        val etPhysician = view.findViewById<TextInputEditText>(R.id.et_physician_name)
        val etNotes = view.findViewById<TextInputEditText>(R.id.et_schedule_notes)
        val switchNotifications = view.findViewById<MaterialSwitch>(R.id.switch_schedule_notifications)

        if (viewModel.scheduleName.isNotBlank() && etName.text.isNullOrBlank()) {
            etName.setText(viewModel.scheduleName)
        }
        etStartDate.setText(dateFormat.format(Date(viewModel.startDate)))
        if (viewModel.endDate != null && viewModel.endDate!! > 0) {
            etEndDate.setText(dateFormat.format(Date(viewModel.endDate!!)))
        }
        if (viewModel.physicianName.isNotBlank() && etPhysician.text.isNullOrBlank()) {
            etPhysician.setText(viewModel.physicianName)
        }
        if (viewModel.notes.isNotBlank() && etNotes.text.isNullOrBlank()) {
            etNotes.setText(viewModel.notes)
        }
        switchNotifications.isChecked = viewModel.notificationsEnabled
    }

    private fun observeViewModel(view: View) {
        val actvSelectMed = view.findViewById<AutoCompleteTextView>(R.id.actv_select_medicine)
        val tvNoAssigned = view.findViewById<TextView>(R.id.tv_no_assigned_meds_warning)
        val rvDrafts = view.findViewById<RecyclerView>(R.id.rv_draft_medicines)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allMedicines.collect { list ->
                        availableMedicinesList = list
                        val names = list.map { "${it.name} (${it.form})" }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        actvSelectMed.setAdapter(adapter)
                    }
                }

                launch {
                    viewModel.assignedMedicines.collect { list ->
                        if (list.isEmpty()) {
                            tvNoAssigned.visibility = View.VISIBLE
                            rvDrafts.visibility = View.GONE
                        } else {
                            tvNoAssigned.visibility = View.GONE
                            rvDrafts.visibility = View.VISIBLE
                        }
                        draftAdapter.submitList(list)
                    }
                }
            }
        }
    }

    private fun showDatePicker(initialMillis: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selected.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(initialMinutes: Int, onTimeSelected: (Int) -> Unit) {
        val initialHour = initialMinutes / 60
        val initialMinute = initialMinutes % 60
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val totalMinutes = hourOfDay * 60 + minute
                onTimeSelected(totalMinutes)
            },
            initialHour,
            initialMinute,
            false
        ).show()
    }
}
