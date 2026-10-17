package com.example.meditrack.ui.caregiver

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
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class CaregiverFormFragment : Fragment() {

    private var isEdit: Boolean = false
    private var fromReportShare: Boolean = false
    private var reportScheduleId: Long = -1L

    private lateinit var viewModel: CaregiverFormViewModel

    private val relationshipOptions = listOf(
        "Spouse",
        "Daughter",
        "Son",
        "Parent",
        "Professional Caregiver",
        "Sibling",
        "Friend",
        "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isEdit = arguments?.getBoolean("isEdit", false) ?: false
        fromReportShare = arguments?.getBoolean("fromReportShare", false) ?: false
        reportScheduleId = arguments?.getLong("reportScheduleId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_caregiver_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = CaregiverFormViewModelFactory(app.caregiverRepository)
        viewModel = ViewModelProvider(this, factory)[CaregiverFormViewModel::class.java]

        setupViews(view)
        observeValidationState(view)

        if (isEdit) {
            viewModel.loadExistingCaregiver { existing ->
                populateFields(view)
            }
        }
    }

    private fun setupViews(view: View) {
        val tvTitle = view.findViewById<TextView>(R.id.tv_form_title)
        tvTitle.text = if (isEdit) "Edit Caregiver Details" else "Add Caregiver Details"

        val btnBack = view.findViewById<ImageButton>(R.id.btn_form_back)
        val btnCancelTop = view.findViewById<MaterialButton>(R.id.btn_form_cancel)
        val btnCancelBottom = view.findViewById<MaterialButton>(R.id.btn_cancel_bottom)

        val cancelListener = View.OnClickListener {
            findNavController().popBackStack()
        }
        btnBack.setOnClickListener(cancelListener)
        btnCancelTop.setOnClickListener(cancelListener)
        btnCancelBottom.setOnClickListener(cancelListener)

        val tilName = view.findViewById<TextInputLayout>(R.id.til_caregiver_name)
        val etName = view.findViewById<TextInputEditText>(R.id.et_caregiver_name)
        val tilRelationship = view.findViewById<TextInputLayout>(R.id.til_caregiver_relationship)
        val actvRelationship = view.findViewById<AutoCompleteTextView>(R.id.actv_relationship)
        val tilPhone = view.findViewById<TextInputLayout>(R.id.til_caregiver_phone)
        val etPhone = view.findViewById<TextInputEditText>(R.id.et_caregiver_phone)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.til_caregiver_email)
        val etEmail = view.findViewById<TextInputEditText>(R.id.et_caregiver_email)
        val switchEmergency = view.findViewById<MaterialSwitch>(R.id.switch_form_emergency)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_caregiver)

        // Setup relationship dropdown
        val relAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, relationshipOptions)
        actvRelationship.setAdapter(relAdapter)

        // Sync inputs with ViewModel
        etName.doAfterTextChanged {
            viewModel.fullName = it?.toString().orEmpty()
            tilName.error = null
        }
        actvRelationship.doAfterTextChanged {
            viewModel.relationship = it?.toString().orEmpty()
            tilRelationship.error = null
        }
        etPhone.doAfterTextChanged {
            viewModel.phone = it?.toString().orEmpty()
            tilPhone.error = null
        }
        etEmail.doAfterTextChanged {
            viewModel.email = it?.toString().orEmpty()
            tilEmail.error = null
        }
        switchEmergency.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isEmergencyContact = isChecked
        }

        btnSave.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = viewModel.saveCaregiver()
                if (result.isSuccess) {
                    Toast.makeText(requireContext(), "Caregiver details saved successfully.", Toast.LENGTH_SHORT).show()
                    if (fromReportShare) {
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("caregiver_saved_for_report", true)
                    }
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun populateFields(view: View) {
        val etName = view.findViewById<TextInputEditText>(R.id.et_caregiver_name)
        val actvRelationship = view.findViewById<AutoCompleteTextView>(R.id.actv_relationship)
        val etPhone = view.findViewById<TextInputEditText>(R.id.et_caregiver_phone)
        val etEmail = view.findViewById<TextInputEditText>(R.id.et_caregiver_email)
        val switchEmergency = view.findViewById<MaterialSwitch>(R.id.switch_form_emergency)

        etName.setText(viewModel.fullName)
        actvRelationship.setText(viewModel.relationship, false)
        etPhone.setText(viewModel.phone)
        etEmail.setText(viewModel.email)
        switchEmergency.isChecked = viewModel.isEmergencyContact
    }

    private fun observeValidationState(view: View) {
        val tilName = view.findViewById<TextInputLayout>(R.id.til_caregiver_name)
        val tilRelationship = view.findViewById<TextInputLayout>(R.id.til_caregiver_relationship)
        val tilPhone = view.findViewById<TextInputLayout>(R.id.til_caregiver_phone)
        val tilEmail = view.findViewById<TextInputLayout>(R.id.til_caregiver_email)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.validationState.collect { result ->
                    when (result) {
                        is FormValidationResult.Valid -> {
                            tilName.error = null
                            tilRelationship.error = null
                            tilPhone.error = null
                            tilEmail.error = null
                        }
                        is FormValidationResult.Invalid -> {
                            tilName.error = result.nameError
                            tilRelationship.error = result.relationshipError
                            tilPhone.error = result.phoneError
                            tilEmail.error = result.emailError
                        }
                    }
                }
            }
        }
    }
}
