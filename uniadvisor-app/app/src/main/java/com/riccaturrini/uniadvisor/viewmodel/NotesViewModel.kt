package com.riccaturrini.uniadvisor.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.riccaturrini.uniadvisor.data.Note
import com.riccaturrini.uniadvisor.data.NoteCreate
import com.riccaturrini.uniadvisor.data.NoteRating
import com.riccaturrini.uniadvisor.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val notes: List<Note>) : NotesUiState()
    object Empty : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}

sealed class UploadNoteState {
    object Idle : UploadNoteState()
    data class Uploading(val progress: Int) : UploadNoteState()
    object Success : UploadNoteState()
    data class Error(val message: String) : UploadNoteState()
}

sealed class DeleteNoteState {
    object Idle : DeleteNoteState()
    object Loading : DeleteNoteState()
    object Success : DeleteNoteState()
    data class Error(val message: String) : DeleteNoteState()
}

class NotesViewModel : ViewModel() {

    private val apiService = ApiClient.instance
    private val storage = FirebaseStorage.getInstance()

    private val _notesState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val notesState: StateFlow<NotesUiState> = _notesState

    private val _uploadState = MutableStateFlow<UploadNoteState>(UploadNoteState.Idle)
    val uploadState: StateFlow<UploadNoteState> = _uploadState

    private val _deleteState = MutableStateFlow<DeleteNoteState>(DeleteNoteState.Idle)
    val deleteState: StateFlow<DeleteNoteState> = _deleteState

    /**
     * Load user's notes
     */
    fun loadMyNotes() {
        viewModelScope.launch {
            _notesState.value = NotesUiState.Loading
            try {
                Log.d("NotesViewModel", "📚 Loading my notes...")
                val response = apiService.getMyNotes()

                when {
                    response.isSuccessful && response.body() != null -> {
                        val notes = response.body()!!
                        Log.d("NotesViewModel", "✅ Loaded ${notes.size} notes")

                        if (notes.isEmpty()) {
                            _notesState.value = NotesUiState.Empty
                        } else {
                            _notesState.value = NotesUiState.Success(notes)
                        }
                    }
                    response.code() == 404 -> {
                        Log.d("NotesViewModel", "ℹ️ No notes found (404)")
                        _notesState.value = NotesUiState.Empty
                    }
                    else -> {
                        val errorMsg = "Error loading notes: ${response.code()}"
                        Log.e("NotesViewModel", "❌ $errorMsg")
                        _notesState.value = NotesUiState.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("NotesViewModel", "💥 Exception loading notes: $errorMsg", e)
                _notesState.value = NotesUiState.Error(errorMsg)
            }
        }
    }

    /**
     * Upload a note file to Firebase Storage and create note in backend.
     *
     * VERSIONE FINALE: Non controlla Firebase.auth.currentUser direttamente,
     * ma lascia che l'AuthInterceptor gestisca l'autenticazione.
     *
     * L'AuthInterceptor si occuperà di ottenere il token e aggiungerlo alla richiesta.
     */
    fun uploadNote(
        fileUri: Uri,
        courseId: Int,
        description: String,
        fileName: String
    ) {
        viewModelScope.launch {
            try {
                Log.d("NotesViewModel", "📤 Starting upload for: $fileName")
                _uploadState.value = UploadNoteState.Uploading(0)

                // Create storage reference
                val timestamp = System.currentTimeMillis()
                val storagePath = "notes/$courseId/$timestamp-$fileName"
                val storageRef = storage.reference.child(storagePath)

                Log.d("NotesViewModel", "📁 Storage path: $storagePath")

                // Upload file to Firebase Storage
                val downloadUrl = withContext(Dispatchers.IO) {
                    val uploadTask = storageRef.putFile(fileUri)

                    // Track upload progress
                    uploadTask.addOnProgressListener { taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                        viewModelScope.launch {
                            _uploadState.value = UploadNoteState.Uploading(progress)
                            Log.d("NotesViewModel", "📊 Upload progress: $progress%")
                        }
                    }

                    // Wait for upload to complete
                    uploadTask.await()
                    Log.d("NotesViewModel", "✅ File uploaded to Firebase Storage")

                    // Get download URL
                    val url = storageRef.downloadUrl.await().toString()
                    Log.d("NotesViewModel", "🔗 Download URL: $url")
                    url
                }

                // IMPORTANTE: Piccolo delay per assicurare che Firebase Storage sia sincronizzato
                // Questo aiuta a stabilizzare l'SDK Firebase tra Storage e Auth
                kotlinx.coroutines.delay(1000)

                // Create note object for backend
                val noteCreate = NoteCreate(
                    course_id = courseId,
                    file_id = downloadUrl,
                    description = description
                )

                Log.d("NotesViewModel", "📤 Sending note to backend...")

                // Call backend API - AuthInterceptor will handle authentication
                val response = withContext(Dispatchers.IO) {
                    apiService.uploadNote(noteCreate)
                }

                // Handle response
                if (response.isSuccessful) {
                    Log.d("NotesViewModel", "✅ Note created in backend")
                    _uploadState.value = UploadNoteState.Success
                    // Reload notes list
                    loadMyNotes()
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Not authenticated. Please logout and login again."
                        403 -> "You don't have permission to upload notes for this course."
                        404 -> "Course not found."
                        else -> "Failed to create note: ${response.code()}"
                    }
                    Log.e("NotesViewModel", "❌ Backend error: $errorMsg (code: ${response.code()})")

                    // Delete uploaded file since backend failed
                    withContext(Dispatchers.IO) {
                        try {
                            storageRef.delete().await()
                            Log.d("NotesViewModel", "🗑️ Deleted uploaded file after backend failure")
                        } catch (e: Exception) {
                            Log.e("NotesViewModel", "Failed to delete file: ${e.message}")
                        }
                    }

                    _uploadState.value = UploadNoteState.Error(errorMsg)
                }

            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("User does not have permission") == true ->
                        "Storage permission denied. Please check your authentication."
                    e.message?.contains("Object does not exist") == true ->
                        "Upload failed. File not found."
                    else -> e.message ?: "Upload failed"
                }
                Log.e("NotesViewModel", "💥 Upload error: $errorMsg", e)
                _uploadState.value = UploadNoteState.Error(errorMsg)
            }
        }
    }

    /**
     * Delete a note
     */
    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            _deleteState.value = DeleteNoteState.Loading
            try {
                Log.d("NotesViewModel", "🗑️ Deleting note $noteId...")
                val response = apiService.deleteNote(noteId)

                if (response.isSuccessful) {
                    Log.d("NotesViewModel", "✅ Note deleted successfully")
                    _deleteState.value = DeleteNoteState.Success
                    // Reload notes
                    loadMyNotes()
                } else {
                    val errorMsg = "Delete failed: ${response.code()}"
                    Log.e("NotesViewModel", "❌ $errorMsg")
                    _deleteState.value = DeleteNoteState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Connection error"
                Log.e("NotesViewModel", "💥 Delete error: $errorMsg", e)
                _deleteState.value = DeleteNoteState.Error(errorMsg)
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadNoteState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteNoteState.Idle
    }
}