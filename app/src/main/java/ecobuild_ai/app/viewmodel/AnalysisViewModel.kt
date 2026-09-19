package ecobuild_ai.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import ecobuild_ai.app.data.remote.AnalysisResponse
import ecobuild_ai.app.data.remote.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    object Loading : AnalysisUiState()
    data class Ready(val analysis: AnalysisResponse) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

class AnalysisViewModel : ViewModel() {
    var uiState by mutableStateOf<AnalysisUiState>(AnalysisUiState.Idle)
        private set

    private val _isCheckedNotify = MutableStateFlow(false)
    val isCheckedNotify: StateFlow<Boolean> = _isCheckedNotify

    fun setNotifyEnabled(enabled: Boolean) {
        _isCheckedNotify.value = enabled
    }
    fun pollAnalysisStatus(planId: Int) {
        uiState = AnalysisUiState.Loading
        viewModelScope.launch {
            try {
                var isReady = false
                while (!isReady) {
                    val analyses = NetworkModule.apiService.getAnalyses(planId)
                    android.util.Log.d("AnalysisVM", "API Response: $analyses")
                    
                    if (analyses.isNotEmpty()) {
                        // Busca o mais recente pelo tempo ou ID
                        val latest = analyses.maxByOrNull { it.created_at }
                        
                        if (latest != null) {
                            android.util.Log.d("AnalysisVM", "Status of plan $planId: ${latest.status}")
                            when (latest.status) {
                                "ready" -> {
                                    uiState = AnalysisUiState.Ready(latest)
                                    isReady = true
                                }
                                "failed" -> {
                                    uiState = AnalysisUiState.Error("Server IA failed to process this plan.")
                                    isReady = true
                                }
                                else -> {
                                    // pending or processing, wait and poll again
                                    delay(4000)
                                }
                            }
                        }
                    } else {
                        // Lista vazia - a análise ainda nem foi criada no DB da API
                        android.util.Log.d("AnalysisVM", "No analysis record found for plan $planId yet. Waiting...")
                        delay(4000)
                    }
                }
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                android.util.Log.e("AnalysisVM", "HTTP Error ${e.code()}: $errorBody")
                uiState = AnalysisUiState.Error("API Error ${e.code()}: ${e.message()}")
            } catch (e: Exception) {
                android.util.Log.e("AnalysisVM", "Unknown Error: ${e.message}", e)
                uiState = AnalysisUiState.Error("Connection Error: ${e.localizedMessage}")
            }
        }
    }
}
