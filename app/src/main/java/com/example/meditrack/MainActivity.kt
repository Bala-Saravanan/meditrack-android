package com.example.meditrack

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
    private val userRepository: UserRepository by lazy {
        (application as MediTrackApplication).userRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_navigation)

        bottomNav.setupWithNavController(navController)

        // Show/hide bottom navigation based on active destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.welcomeFragment,
                R.id.registrationFragment,
                R.id.scheduleDetailsFragment,
                R.id.createScheduleFragment,
                R.id.addMedicineFragment,
                R.id.scheduleDoseHistoryFragment -> {
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }

        // On cold start, check if user profile exists
        lifecycleScope.launch {
            if (userRepository.hasUserProfile() && navController.currentDestination?.id == R.id.welcomeFragment) {
                navController.navigate(R.id.action_welcome_to_dashboard)
            }
        }
    }
}