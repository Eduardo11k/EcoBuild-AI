package ecobuild_ai.app.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ecobuild_ai.app.screens.SelectedFile

class UploadViewModel : ViewModel() {
    var selectedFile by mutableStateOf<SelectedFile?>(null)
    var pdfThumbnail by mutableStateOf<Bitmap?>(null)

    fun clear() {
        selectedFile = null
        pdfThumbnail = null
    }
}
