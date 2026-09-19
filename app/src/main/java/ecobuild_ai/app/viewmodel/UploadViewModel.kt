package ecobuild_ai.app.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ecobuild_ai.app.screens.SelectedFile

import ecobuild_ai.app.data.remote.NetworkModule
import ecobuild_ai.app.data.remote.OrganizationCreateRequest
import ecobuild_ai.app.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UploadViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val apiService = NetworkModule.apiService
    private val auth = FirebaseAuth.getInstance()

    var selectedFile by mutableStateOf<SelectedFile?>(null)
    var pdfThumbnail by mutableStateOf<Bitmap?>(null)
    var organizationId by mutableStateOf<Int?>(null)
    var isCheckingOrg by mutableStateOf(false)
    
    // Novo: ID do plano após upload bem sucedido
    var lastUploadedPlanId by mutableStateOf<Int?>(null)

    init {
        refreshOrganization()
    }

    fun refreshOrganization() {
        val currentUser = auth.currentUser ?: return
        if (organizationId != null) return
        
        isCheckingOrg = true
        viewModelScope.launch {
            try {
                android.util.Log.d("UploadViewModel", "Checking organization for user: ${currentUser.uid}")
                // 1. Tenta buscar do Firestore
                val user = userRepository.getUser(currentUser.uid)
                if (user?.organizationId != null) {
                    android.util.Log.d("UploadViewModel", "Found organizationId in Firestore: ${user.organizationId}")
                    organizationId = user.organizationId
                } else {
                    android.util.Log.d("UploadViewModel", "No organizationId found. Creating new one via API...")
                    // 2. Se não existir, cria na API
                    val orgName = "${user?.fullName?.ifEmpty { null } ?: currentUser.displayName?.ifEmpty { null } ?: "User"}'s Projects"
                    
                    // Validação rigorosa conforme o OpenAPI JSON:
                    // Name: 4-50 chars. Location: 8-50 chars.
                    val safeOrgName = if (orgName.length < 4) orgName.padEnd(4, '_') else orgName.take(50)
                    val safeLocation = "Street, City, Luanda" // Garantindo mais de 8 caracteres

                    // TENTA LISTAR ANTES DE CRIAR (Caso o servidor já tenha mas o Firestore não)
                    try {
                        val existingOrgs = apiService.getOrganizations()
                        val match = existingOrgs.find { it.name == safeOrgName }
                        if (match != null) {
                            android.util.Log.d("UploadViewModel", "Found existing org on API with name $safeOrgName: ${match.id}")
                            organizationId = match.id
                        } else {
                            val response = apiService.createOrganization(
                                OrganizationCreateRequest(name = safeOrgName, location = safeLocation)
                            )
                            android.util.Log.d("UploadViewModel", "New organization created on API: ${response.id}")
                            organizationId = response.id
                        }
                    } catch (apiEx: Exception) {
                        android.util.Log.e("UploadViewModel", "API Error while checking/creating org: ${apiEx.message}")
                        throw apiEx
                    }

                    // 3. Guarda o ID no Firestore para a próxima vez
                    organizationId?.let { id ->
                        userRepository.updateOrganization(currentUser.uid, id)
                        android.util.Log.d("UploadViewModel", "OrganizationId $id saved to Firestore.")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UploadViewModel", "Critical Error managing organization: ${e.message}", e)
            } finally {
                isCheckingOrg = false
            }
        }
    }

    fun clear() {
        selectedFile = null
        pdfThumbnail = null
    }
}
