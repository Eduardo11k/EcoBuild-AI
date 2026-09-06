package ecobuild_ai.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ecobuild_ai.app.model.User
import ecobuild_ai.app.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val repository = UserRepository()
    private val auth = Firebase.auth

    private val _userData = MutableStateFlow<User?>(null)
    val userData: StateFlow<User?> = _userData

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: run {
            _isLoaded.value = true
            return
        }
        _isLoaded.value = false
        viewModelScope.launch {
            try {
                _userData.value = repository.getUser(uid)
            } catch (e: Exception) {
                Log.e("LOAD_USER", "Error loading user data", e)
            } finally {
                _isLoaded.value = true
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun refreshUserData() {
        loadUserData()
    }

    fun updateProfile(fullName: String, email: String, profileImageUrl: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateProfile(uid, fullName, email, profileImageUrl)

                try {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = fullName.trim()
                        if (profileImageUrl.isNotBlank()) {
                            photoUri = android.net.Uri.parse(profileImageUrl)
                        }
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.e("UPDATE_AUTH", "Error syncing Auth displayName", e)
                }

                _userData.value = repository.getUser(uid)
                _saveSuccess.value = true
            } catch (e: Exception) {
                Log.e("UPDATE:", "Error updating profile", e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
