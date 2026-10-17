package com.example.meditrack.ui.caregiver

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.Caregiver
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class CaregiverInfoFragment : Fragment() {

    private lateinit var viewModel: CaregiverViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_caregiver_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as MediTrackApplication
        val factory = CaregiverViewModelFactory(app.caregiverRepository)
        viewModel = ViewModelProvider(this, factory)[CaregiverViewModel::class.java]

        setupViews(view)
        observeViewModel(view)
    }

    private fun setupViews(view: View) {
        val btnAdd = view.findViewById<MaterialButton>(R.id.btn_add_caregiver)
        btnAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isEdit", false)
            }
            findNavController().navigate(R.id.action_caregiverInfo_to_caregiverForm, bundle)
        }

        val btnEditTop = view.findViewById<ImageButton>(R.id.btn_edit_caregiver_top)
        btnEditTop.setOnClickListener {
            navigateToEdit()
        }

        val btnEditDetails = view.findViewById<MaterialButton>(R.id.btn_edit_caregiver_details)
        btnEditDetails.setOnClickListener {
            navigateToEdit()
        }

        val btnRemove = view.findViewById<MaterialButton>(R.id.btn_remove_caregiver)
        btnRemove.setOnClickListener {
            confirmRemoveCaregiver()
        }
    }

    private fun navigateToEdit() {
        val bundle = Bundle().apply {
            putBoolean("isEdit", true)
        }
        findNavController().navigate(R.id.action_caregiverInfo_to_caregiverForm, bundle)
    }

    private fun observeViewModel(view: View) {
        val layoutEmpty = view.findViewById<View>(R.id.layout_empty_caregiver)
        val layoutContent = view.findViewById<View>(R.id.layout_caregiver_content)
        val btnEditTop = view.findViewById<ImageButton>(R.id.btn_edit_caregiver_top)

        val tvAvatar = view.findViewById<TextView>(R.id.tv_caregiver_avatar)
        val tvName = view.findViewById<TextView>(R.id.tv_caregiver_name)
        val tvRelation = view.findViewById<TextView>(R.id.tv_caregiver_relation_badge)
        val tvPhone = view.findViewById<TextView>(R.id.tv_caregiver_phone)
        val tvEmail = view.findViewById<TextView>(R.id.tv_caregiver_email)
        val switchEmergency = view.findViewById<MaterialSwitch>(R.id.switch_emergency_contact)
        val btnCall = view.findViewById<MaterialButton>(R.id.btn_call_caregiver)
        val btnQuickDial = view.findViewById<ImageButton>(R.id.btn_quick_dial)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.caregiver.collect { caregiver ->
                    if (caregiver == null) {
                        layoutEmpty.visibility = View.VISIBLE
                        layoutContent.visibility = View.GONE
                        btnEditTop.visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        layoutContent.visibility = View.VISIBLE
                        btnEditTop.visibility = View.VISIBLE

                        bindCaregiverData(
                            caregiver,
                            tvAvatar,
                            tvName,
                            tvRelation,
                            tvPhone,
                            tvEmail,
                            switchEmergency,
                            btnCall,
                            btnQuickDial
                        )
                    }
                }
            }
        }
    }

    private fun bindCaregiverData(
        caregiver: Caregiver,
        tvAvatar: TextView,
        tvName: TextView,
        tvRelation: TextView,
        tvPhone: TextView,
        tvEmail: TextView,
        switchEmergency: MaterialSwitch,
        btnCall: MaterialButton,
        btnQuickDial: ImageButton
    ) {
        // Compute avatar initials
        val words = caregiver.fullName.trim().split("\\s+".toRegex())
        val initials = when {
            words.size >= 2 -> "${words[0].firstOrNull()?.uppercaseChar() ?: ""}${words[1].firstOrNull()?.uppercaseChar() ?: ""}"
            words.isNotEmpty() && words[0].isNotEmpty() -> words[0].take(2).uppercase()
            else -> "CG"
        }
        tvAvatar.text = initials
        tvName.text = caregiver.fullName
        tvRelation.text = caregiver.relationship
        tvPhone.text = caregiver.phone
        tvEmail.text = if (caregiver.email.isNotBlank()) caregiver.email else "Not provided"

        // Switch
        switchEmergency.setOnCheckedChangeListener(null)
        switchEmergency.isChecked = caregiver.isEmergencyContact
        switchEmergency.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEmergencyContact(isChecked)
        }

        // Call actions
        val callClickListener = View.OnClickListener {
            dialPhoneNumber(caregiver.phone)
        }
        btnCall.setOnClickListener(callClickListener)
        btnQuickDial.setOnClickListener(callClickListener)
    }

    private fun dialPhoneNumber(phoneNumber: String) {
        val trimmed = phoneNumber.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(requireContext(), "No phone number available.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(trimmed)}")
        }

        try {
            startActivity(dialIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No dialer application found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmRemoveCaregiver() {
        val currentCaregiver = viewModel.caregiver.value ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Caregiver")
            .setMessage("Are you sure you want to remove ${currentCaregiver.fullName}? Schedules, medicines, and dose history will not be affected.")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.deleteCaregiver {
                    Toast.makeText(requireContext(), "Caregiver removed successfully.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
