package com.example.meditrack.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meditrack.MediTrackApplication
import com.example.meditrack.R
import com.example.meditrack.data.local.DatabaseInitializer
import com.example.meditrack.data.local.MediTrackDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGetStarted = view.findViewById<Button>(R.id.btn_get_started)
        val btnLoadDemo = view.findViewById<Button>(R.id.btn_load_demo)

        // Clean user registration for real personal data
        btnGetStarted.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_registration)
        }

        // Dedicated demo data loading option for presentation and testing
        btnLoadDemo.setOnClickListener {
            btnLoadDemo.isEnabled = false
            btnGetStarted.isEnabled = false
            val app = requireActivity().application as MediTrackApplication
            lifecycleScope.launch {
                DatabaseInitializer.populateInitialData(app.database, this)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Sample demo data loaded successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_welcome_to_dashboard)
                }
            }
        }
    }
}
