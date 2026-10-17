package com.example.meditrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.meditrack.data.repository.UserRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private var currentBottomInset: Int = 0
    private val userRepository: UserRepository by lazy {
        (application as MediTrackApplication).userRepository
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission denied. Scheduled reminders may not appear.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestNotificationPermission(force: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                val prefs = getSharedPreferences("meditrack_prefs", Context.MODE_PRIVATE)
                val alreadyPrompted = prefs.getBoolean("notification_permission_prompted", false)
                if (!alreadyPrompted || force) {
                    prefs.edit().putBoolean("notification_permission_prompted", true).apply()
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainContainer = findViewById<View>(R.id.main_container)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_navigation)

        // Handle System Insets (Status bar & Navigation bar) to prevent header and action overlap
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainContainer.updatePadding(
                left = insets.left,
                top = insets.top,
                right = insets.right
            )
            currentBottomInset = insets.bottom
            updateBottomInsets(bottomNav.visibility == View.VISIBLE)
            windowInsets
        }

        bottomNav.setupWithNavController(navController)

        // Show/hide bottom navigation based on active destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isBottomNavHidden = when (destination.id) {
                R.id.welcomeFragment,
                R.id.registrationFragment,
                R.id.scheduleDetailsFragment,
                R.id.createScheduleFragment,
                R.id.addMedicineFragment,
                R.id.scheduleDoseHistoryFragment,
                R.id.scheduleReportFragment,
                R.id.caregiverFormFragment -> true
                else -> false
            }
            bottomNav.visibility = if (isBottomNavHidden) View.GONE else View.VISIBLE
            updateBottomInsets(!isBottomNavHidden)
        }

        // On cold start, check if user profile exists
        lifecycleScope.launch {
            val hasProfile = userRepository.hasUserProfile()
            if (hasProfile && navController.currentDestination?.id == R.id.welcomeFragment) {
                navController.navigate(R.id.action_welcome_to_dashboard)
            }
            if (hasProfile) {
                requestNotificationPermission(force = false)
            }
            handleNotificationIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val navigateToHistory = intent.getBooleanExtra("navigate_to_history", false)
        val scheduleId = intent.getLongExtra("scheduleId", -1L)
        val doseId = intent.getLongExtra("doseId", -1L)

        if (navigateToHistory && scheduleId > 0) {
            intent.removeExtra("navigate_to_history")
            intent.removeExtra("scheduleId")
            intent.removeExtra("doseId")
            val bundle = Bundle().apply {
                putLong("scheduleId", scheduleId)
                putLong("doseId", doseId)
            }
            try {
                navController.navigate(R.id.scheduleDoseHistoryFragment, bundle)
            } catch (e: Exception) {
                // Ignore if navigation destination is unreachable in current state
            }
        }
    }

    private fun updateBottomInsets(isBottomNavVisible: Boolean) {
        val navHostView = findViewById<View>(R.id.nav_host_fragment) ?: return
        if (isBottomNavVisible) {
            bottomNav.updatePadding(bottom = currentBottomInset)
            navHostView.updatePadding(bottom = 0)
        } else {
            bottomNav.updatePadding(bottom = 0)
            navHostView.updatePadding(bottom = currentBottomInset)
        }
    }
}