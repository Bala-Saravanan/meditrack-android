package com.example.meditrack.ui.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.repository.CaregiverRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaregiverViewModel(
    private val caregiverRepository: CaregiverRepository
) : ViewModel() {

    val caregiver: StateFlow<Caregiver?> = caregiverRepository.caregiver.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateEmergencyContact(isEmergency: Boolean) {
        viewModelScope.launch {
            caregiverRepository.updateEmergencyContactStatus(isEmergency)
        }
    }

    fun deleteCaregiver(onDeleted: () -> Unit) {
        viewModelScope.launch {
            caregiverRepository.deleteCaregiver()
            onDeleted()
        }
    }
}

class CaregiverViewModelFactory(
    private val caregiverRepository: CaregiverRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaregiverViewModel::class.java)) {
            return CaregiverViewModel(caregiverRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
