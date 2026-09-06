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

    // true quando o Firestore respondeu (mesmo que sem documento)
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
                _isLoaded.value = true   // Firestore respondeu — pode aplicar fallback na UI
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun refreshUserData() {
        loadUserData()
    }

    fun updateProfile(fullName: String, email: String, username: String, profileImageUrl: String) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // 1. Sincroniza com o Firestore
                repository.updateProfile(uid, fullName, username, email, profileImageUrl)

                // 2. Sincroniza com o Firebase Auth displayName imediatamente
                try {
                    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                        displayName = fullName.trim()
                        if (profileImageUrl.isNotBlank()) {
                            photoUri = android.net.Uri.parse(profileImageUrl)
                        }
                    }
                    user.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    Log.e("UPDATE_AUTH", "Erro ao sincronizar Auth displayName", e)
                }

                // 3. Recarrega os dados
                _userData.value = repository.getUser(uid)
                _saveSuccess.value = true
            } catch (e: Exception) {
                Log.e("UPDATE:", "Erro ao atualizar perfil", e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
