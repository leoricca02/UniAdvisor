package com.riccaturrini.uniadvisor.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.text.Text
import com.riccaturrini.uniadvisor.data.*
import com.riccaturrini.uniadvisor.utils.OcrUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class CameraOcrViewModel : ViewModel() {

    private val storage = FirebaseStorage.getInstance()

    private val _capturedImages = MutableStateFlow<List<CapturedImage>>(emptyList())
    val capturedImages: StateFlow<List<CapturedImage>> = _capturedImages

    private val _ocrState = MutableStateFlow<OcrProcessingState>(OcrProcessingState.Idle)
    val ocrState: StateFlow<OcrProcessingState> = _ocrState

    private val _pdfState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Idle)
    val pdfState: StateFlow<PdfGenerationState> = _pdfState

    private val _uploadState = MutableStateFlow<UploadNoteState>(UploadNoteState.Idle)
    val uploadState: StateFlow<UploadNoteState> = _uploadState

    fun addCapturedImage(uri: Uri) {
        val newImage = CapturedImage(uri = uri)
        _capturedImages.value = _capturedImages.value + newImage
    }

    fun removeCapturedImage(uri: Uri) {
        _capturedImages.value = _capturedImages.value.filter { it.uri != uri }
    }

    fun clearAllImages() {
        _capturedImages.value = emptyList()
        _ocrState.value = OcrProcessingState.Idle
        _pdfState.value = PdfGenerationState.Idle
    }

    fun processOcrOnImages(context: Context) {
        viewModelScope.launch {
            _ocrState.value = OcrProcessingState.Processing
            try {
                val updatedImages = mutableListOf<CapturedImage>()
                for (image in _capturedImages.value) {
                    // MODIFICA: extractTextFromImage ora ritorna 'Text' (non String)
                    val extractedTextResult: Text = withContext(Dispatchers.IO) {
                        OcrUtils.extractTextFromImage(context, image.uri)
                    }
                    updatedImages.add(image.copy(extractedText = extractedTextResult))
                }
                _capturedImages.value = updatedImages

                // MODIFICA: Per mostrare il testo nella UI usiamo .text dell'oggetto Text
                val allText = updatedImages.joinToString("\n\n") { it.extractedText?.text ?: "" }
                _ocrState.value = OcrProcessingState.Success(allText)
            } catch (e: Exception) {
                _ocrState.value = OcrProcessingState.Error(e.message ?: "OCR processing failed")
            }
        }
    }

    /**
     * Called specifically when user wants to preview the PDF before uploading
     */
    fun generatePdfForPreview(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _pdfState.value = PdfGenerationState.Processing(0)
                val imageUris = _capturedImages.value.map { it.uri }
                // MODIFICA: Passiamo la lista di oggetti Text?
                val extractedTexts = _capturedImages.value.map { it.extractedText }

                val pdfFile = OcrUtils.generateSearchablePdf(
                    context = context,
                    images = imageUris,
                    extractedTexts = extractedTexts
                )
                _pdfState.value = PdfGenerationState.Success(Uri.fromFile(pdfFile))
            } catch (e: Exception) {
                Log.e("CameraOcrViewModel", "PDF Generation error", e)
                _pdfState.value = PdfGenerationState.Error(e.message ?: "Failed to generate PDF")
            }
        }
    }

    /**
     * Internal helper to generate PDF (synchronous)
     */
    private fun generatePdfInternal(context: Context): File? {
        return try {
            val imageUris = _capturedImages.value.map { it.uri }
            // MODIFICA: Passiamo la lista di oggetti Text?
            val extractedTexts = _capturedImages.value.map { it.extractedText }
            OcrUtils.generateSearchablePdf(context, imageUris, extractedTexts)
        } catch (e: Exception) {
            null
        }
    }

    fun uploadPdfAsNote(context: Context, courseId: Int, description: String) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadNoteState.Uploading(0)

                // Generate fresh PDF for upload
                val pdfFile = withContext(Dispatchers.IO) {
                    generatePdfInternal(context)
                }

                if (pdfFile == null) {
                    _uploadState.value = UploadNoteState.Error("Failed to generate PDF")
                    return@launch
                }

                _uploadState.value = UploadNoteState.Uploading(20)

                // Upload logic
                val timestamp = System.currentTimeMillis()
                val storagePath = "notes/$courseId/$timestamp-scanned.pdf"
                val storageRef = storage.reference.child(storagePath)

                val uploadTask = withContext(Dispatchers.IO) {
                    storageRef.putFile(Uri.fromFile(pdfFile))
                }

                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    viewModelScope.launch {
                        _uploadState.value = UploadNoteState.Uploading(20 + (progress * 0.6).toInt())
                    }
                }

                uploadTask.await()
                val downloadUrl = withContext(Dispatchers.IO) {
                    storageRef.downloadUrl.await().toString()
                }

                val noteCreate = NoteCreate(
                    course_id = courseId,
                    file_id = downloadUrl,
                    description = description.ifBlank { "Scanned note (OCR)" }
                )

                val response = withContext(Dispatchers.IO) {
                    com.riccaturrini.uniadvisor.network.ApiClient.instance.uploadNote(noteCreate)
                }

                if (response.isSuccessful) {
                    _uploadState.value = UploadNoteState.Success
                    pdfFile.delete()
                    clearAllImages()
                } else {
                    _uploadState.value = UploadNoteState.Error("Failed to create note: ${response.code()}")
                    // Try to delete the uploaded file if backend failed
                    try { storageRef.delete() } catch (_: Exception) { }
                }

            } catch (e: Exception) {
                _uploadState.value = UploadNoteState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun resetUploadState() { _uploadState.value = UploadNoteState.Idle }
    fun resetPdfState() { _pdfState.value = PdfGenerationState.Idle } // Reset PDF state manually

    override fun onCleared() {
        super.onCleared()
        OcrUtils.cleanup()
    }
}