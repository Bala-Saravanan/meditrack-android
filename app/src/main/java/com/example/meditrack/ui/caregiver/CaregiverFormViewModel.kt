package com.example.meditrack.ui.caregiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.repository.CaregiverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class FormValidationResult {
    object Valid : FormValidationResult()
    data class Invalid(
        val nameError: String? = null,
        val relationshipError: String? = null,
        val phoneError: String? = null,
        val emailError: String? = null
    ) : FormValidationResult()
}

class CaregiverFormViewModel(
    private val caregiverRepository: CaregiverRepository
) : ViewModel() {

    var fullName: String = ""
    var relationship: String = ""
    var phone: String = ""
    var email: String = ""
    var isEmergencyContact: Boolean = true

    private var isInitialized = false

    private val _validationState = MutableStateFlow<FormValidationResult>(FormValidationResult.Valid)
    val validationState: StateFlow<FormValidationResult> = _validationState

    fun loadExistingCaregiver(onLoaded: (Caregiver) -> Unit) {
        if (isInitialized) return
        viewModelScope.launch {
            val existing = caregiverRepository.getCaregiverOnce()
            if (existing != null) {
                fullName = existing.fullName
                relationship = existing.relationship
                phone = existing.phone
                email = existing.email
                isEmergencyContact = existing.isEmergencyContact
                isInitialized = true
                onLoaded(existing)
            }
        }
    }

    fun validate(): Boolean {
        var nameErr: String? = null
        var relErr: String? = null
        var phoneErr: String? = null
        var emailErr: String? = null

        val trimmedName = fullName.trim()
        val trimmedRel = relationship.trim()
        val trimmedPhone = phone.trim()
        val trimmedEmail = email.trim()

        if (trimmedName.isBlank()) {
            nameErr = "Caregiver full name is required."
        }

        if (trimmedRel.isBlank()) {
            relErr = "Please specify relationship."
        }

        if (trimmedPhone.isBlank()) {
            phoneErr = "Phone number is required."
        } else {
            // Strip formatting characters to verify actual digit count
            val digits = trimmedPhone.filter { it.isDigit() }
            if (digits.length < 7) {
                phoneErr = "Please enter a valid phone number (at least 7 digits)."
            }
        }

        if (trimmedEmail.isNotBlank()) {
            val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
            if (!emailPattern.matches(trimmedEmail)) {
                emailErr = "Please enter a valid email address."
            }
        }

        val hasError = nameErr != null || relErr != null || phoneErr != null || emailErr != null
        _validationState.value = if (hasError) {
            FormValidationResult.Invalid(nameErr, relErr, phoneErr, emailErr)
        } else {
            FormValidationResult.Valid
        }

        return !hasError
    }

    suspend fun saveCaregiver(): Result<Unit> {
        if (!validate()) {
            return Result.failure(IllegalArgumentException("Validation failed"))
        }

        return try {
            caregiverRepository.saveCaregiver(
                fullName = fullName.trim(),
                relationship = relationship.trim(),
                phone = phone.trim(),
                email = email.trim(),
                isEmergencyContact = isEmergencyContact
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CaregiverFormViewModelFactory(
    private val caregiverRepository: CaregiverRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CaregiverFormViewModel::class.java)) {
            return CaregiverFormViewModel(caregiverRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
