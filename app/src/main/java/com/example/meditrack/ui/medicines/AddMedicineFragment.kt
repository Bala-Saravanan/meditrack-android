package com.example.meditrack.ui.medicines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddMedicineFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_medicine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val medicineRepository = app.medicineRepository

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_add_medicine)
        val btnClose = view.findViewById<ImageButton>(R.id.btn_close_add_medicine)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel_add_medicine)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save_medicine)

        val tilName = view.findViewById<TextInputLayout>(R.id.til_medicine_name)
        val etName = view.findViewById<TextInputEditText>(R.id.et_medicine_name)
        val actvForm = view.findViewById<AutoCompleteTextView>(R.id.actv_medicine_form)
        val etInstructions = view.findViewById<TextInputEditText>(R.id.et_medicine_instructions)

        // Setup Forms Dropdown
        val forms = arrayOf("Oral Tablet", "Capsule", "Liquid / Syrup", "Injection", "Inhaler", "Topical Cream", "Drops", "Other")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, forms)
        actvForm.setAdapter(adapter)

        val navigateBack = {
            findNavController().popBackStack()
        }

        btnBack.setOnClickListener { navigateBack() }
        btnClose.setOnClickListener { navigateBack() }
        btnCancel.setOnClickListener { navigateBack() }

        btnSave.setOnClickListener {
            val name = etName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                tilName.error = getString(R.string.error_medicine_name_required)
                etName.requestFocus()
                return@setOnClickListener
            }
            tilName.error = null

            val form = actvForm.text?.toString()?.trim()?.ifEmpty { "Oral Tablet" } ?: "Oral Tablet"
            val instructions = etInstructions.text?.toString()?.trim().orEmpty()

            viewLifecycleOwner.lifecycleScope.launch {
                val newId = medicineRepository.addMedicine(name, instructions, form)
                Toast.makeText(requireContext(), "$name saved", Toast.LENGTH_SHORT).show()
                findNavController().previousBackStackEntry?.savedStateHandle?.set("new_medicine_id", newId)
                findNavController().popBackStack()
            }
        }
    }
}
