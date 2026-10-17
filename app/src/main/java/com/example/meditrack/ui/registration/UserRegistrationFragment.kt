package com.example.meditrack.ui.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.example.meditrack.data.local.entity.UserProfile
import com.example.meditrack.data.repository.UserRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserRegistrationFragment : Fragment() {

    private val userRepository: UserRepository by lazy {
        (requireActivity().application as MediTrackApplication).userRepository
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back)
        val btnContinue = view.findViewById<Button>(R.id.btn_continue)
        val tilFullName = view.findViewById<TextInputLayout>(R.id.til_full_name)
        val etFullName = view.findViewById<TextInputEditText>(R.id.et_full_name)

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        etFullName.doAfterTextChanged {
            tilFullName.error = null
        }

        btnContinue.setOnClickListener {
            val name = etFullName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                tilFullName.error = getString(R.string.error_name_required)
                etFullName.requestFocus()
                return@setOnClickListener
            }

            btnContinue.isEnabled = false
            lifecycleScope.launch {
                userRepository.saveUserProfile(name)

                withContext(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_registration_to_dashboard)
                }
            }
        }
    }
}
