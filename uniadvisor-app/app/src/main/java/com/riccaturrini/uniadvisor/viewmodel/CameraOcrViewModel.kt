package com.riccaturrini.uniadvisor.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
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

    /**
     * Add a captured image
     */
    fun addCapturedImage(uri: Uri) {
        val newImage = CapturedImage(uri = uri)
        _capturedImages.value = _capturedImages.value + newImage
        Log.d("CameraOcrViewModel", "📷 Image added. Total: ${_capturedImages.value.size}")
    }

    /**
     * Remove a captured image
     */
    fun removeCapturedImage(uri: Uri) {
        _capturedImages.value = _capturedImages.value.filter { it.uri != uri }
        Log.d("CameraOcrViewModel", "🗑️ Image removed. Total: ${_capturedImages.value.size}")
    }

    /**
     * Clear all captured images
     */
    fun clearAllImages() {
        _capturedImages.value = emptyList()
        _ocrState.value = OcrProcessingState.Idle
        _pdfState.value = PdfGenerationState.Idle
        Log.d("CameraOcrViewModel", "🧹 All images cleared")
    }

    /**
     * Process OCR on all captured images
     */
    fun processOcrOnImages(context: Context) {
        viewModelScope.launch {
            _ocrState.value = OcrProcessingState.Processing

            try {
                val updatedImages = mutableListOf<CapturedImage>()

                for (image in _capturedImages.value) {
                    val extractedText = withContext(Dispatchers.IO) {
                        OcrUtils.extractTextFromImage(context, image.uri)
                    }

                    updatedImages.add(image.copy(extractedText = extractedText))
                    Log.d("CameraOcrViewModel", "✅ OCR processed for image: ${image.uri}")
                }

                _capturedImages.value = updatedImages

                // Combine all extracted text
                val allText = updatedImages.joinToString("\n\n") { it.extractedText ?: "" }

                _ocrState.value = OcrProcessingState.Success(allText)
                Log.d("CameraOcrViewModel", "🎉 OCR completed. Total text: ${allText.length} chars")

            } catch (e: Exception) {
                Log.e("CameraOcrViewModel", "❌ OCR failed", e)
                _ocrState.value = OcrProcessingState.Error(e.message ?: "OCR processing failed")
            }
        }
    }

    /**
     * Generate PDF from captured images and extracted text
     */
    fun generatePdf(context: Context): File? {
        return try {
            _pdfState.value = PdfGenerationState.Processing(0)

            val imageUris = _capturedImages.value.map { it.uri }
            val extractedTexts = _capturedImages.value.map { it.extractedText ?: "" }

            _pdfState.value = PdfGenerationState.Processing(50)

            val pdfFile = OcrUtils.generateSearchablePdf(
                context = context,
                images = imageUris,
                extractedTexts = extractedTexts
            )

            _pdfState.value = PdfGenerationState.Processing(100)
            _pdfState.value = PdfGenerationState.Success(Uri.fromFile(pdfFile))

            Log.d("CameraOcrViewModel", "✅ PDF generated: ${pdfFile.absolutePath}")
            pdfFile

        } catch (e: Exception) {
            Log.e("CameraOcrViewModel", "❌ PDF generation failed", e)
            _pdfState.value = PdfGenerationState.Error(e.message ?: "PDF generation failed")
            null
        }
    }

    /**
     * Upload PDF to Firebase Storage and create note
     */
    fun uploadPdfAsNote(
        context: Context,
        courseId: Int,
        description: String
    ) {
        viewModelScope.launch {
            try {
                _uploadState.value = UploadNoteState.Uploading(0)

                // Generate PDF first
                val pdfFile = generatePdf(context)
                if (pdfFile == null) {
                    _uploadState.value = UploadNoteState.Error("Failed to generate PDF")
                    return@launch
                }

                _uploadState.value = UploadNoteState.Uploading(25)

                // Upload to Firebase Storage
                val timestamp = System.currentTimeMillis()
                val storagePath = "notes/$courseId/$timestamp-scanned.pdf"
                val storageRef = storage.reference.child(storagePath)

                val uploadTask = withContext(Dispatchers.IO) {
                    storageRef.putFile(Uri.fromFile(pdfFile))
                }

                // Track upload progress
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    viewModelScope.launch {
                        _uploadState.value = UploadNoteState.Uploading(25 + (progress * 0.5).toInt())
                    }
                }

                uploadTask.await()

                _uploadState.value = UploadNoteState.Uploading(75)

                // Get download URL
                val downloadUrl = withContext(Dispatchers.IO) {
                    storageRef.downloadUrl.await().toString()
                }

                _uploadState.value = UploadNoteState.Uploading(90)

                // Create note in backend
                val noteCreate = NoteCreate(
                    course_id = courseId,
                    file_id = downloadUrl,
                    description = description.ifBlank { "Scanned note (OCR)" }
                )

                val response = withContext(Dispatchers.IO) {
                    com.riccaturrini.uniadvisor.network.ApiClient.instance.uploadNote(noteCreate)
                }

                if (response.isSuccessful) {
                    Log.d("CameraOcrViewModel", "✅ Note uploaded successfully")
                    _uploadState.value = UploadNoteState.Success

                    // Cleanup
                    pdfFile.delete()
                    clearAllImages()
                } else {
                    Log.e("CameraOcrViewModel", "❌ Backend error: ${response.code()}")
                    _uploadState.value = UploadNoteState.Error("Failed to create note: ${response.code()}")

                    // Delete uploaded file
                    withContext(Dispatchers.IO) {
                        try {
                            storageRef.delete().await()
                        } catch (e: Exception) {
                            Log.e("CameraOcrViewModel", "Failed to delete file", e)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("CameraOcrViewModel", "💥 Upload error", e)
                _uploadState.value = UploadNoteState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadNoteState.Idle
    }

    fun resetOcrState() {
        _ocrState.value = OcrProcessingState.Idle
    }

    fun resetPdfState() {
        _pdfState.value = PdfGenerationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        OcrUtils.cleanup()
    }
}