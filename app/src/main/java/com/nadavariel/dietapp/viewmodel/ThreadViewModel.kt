package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Import your Thread model
import com.nadavariel.dietapp.model.Thread // <<<< CORRECT IMPORT
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query // Keep for ordering
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.Comment // Assuming Comment class path is correct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
// No need for com.google.firebase.Timestamp or java.util.Date here if Thread uses Long for timestamp
// No need for com.google.firebase.firestore.FieldValue for commentCount

class ThreadViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val _threads = MutableStateFlow<List<Thread>>(emptyList())
    val threads: StateFlow<List<Thread>> = _threads.asStateFlow()

    private val _selectedThread = MutableStateFlow<Thread?>(null)
    val selectedThread: StateFlow<Thread?> = _selectedThread.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    init {
        fetchAllThreads()
    }

    private fun fetchAllThreads() {
        viewModelScope.launch {
            try {
                firestore.collection("threads")
                    .orderBy("timestamp", Query.Direction.DESCENDING) // Order by your 'timestamp' field
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("ThreadViewModel", "Listen failed for all threads.", e)
                            _threads.value = emptyList()
                            return@addSnapshotListener
                        }
                        val threadList = snapshots?.mapNotNull { document ->
                            document.toObject<Thread>()?.copy(id = document.id)
                        } ?: emptyList()
                        _threads.value = threadList
                    }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching all threads", e)
                _threads.value = emptyList()
            }
        }
    }

    fun fetchThreadById(threadId: String) {
        viewModelScope.launch {
            _selectedThread.value = null
            _comments.value = emptyList()
            try {
                val docSnapshot = firestore.collection("threads").document(threadId).get().await()
                _selectedThread.value = docSnapshot.toObject<Thread>()?.copy(id = docSnapshot.id)
                if (_selectedThread.value != null) {
                    fetchCommentsForThread(threadId)
                } else {
                    Log.w("ThreadViewModel", "Thread with ID $threadId not found.")
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching thread by ID: $threadId", e)
            }
        }
    }

    private fun fetchCommentsForThread(threadId: String) {
        viewModelScope.launch {
            try {
                // Assuming Comment class has 'createdAt' as Firestore Timestamp or similar for ordering
                firestore.collection("threads").document(threadId).collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("ThreadViewModel", "Listen failed for comments on thread $threadId.", e)
                            _comments.value = emptyList()
                            return@addSnapshotListener
                        }
                        val commentList = snapshots?.mapNotNull { document ->
                            document.toObject<Comment>()?.copy(id = document.id)
                        } ?: emptyList()
                        _comments.value = commentList
                    }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching comments for thread: $threadId", e)
                _comments.value = emptyList()
            }
        }
    }

    fun addComment(threadId: String, commentText: String, authorName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("ThreadViewModel", "User not logged in. Cannot add comment.")
            return
        }
        if (commentText.isBlank()) {
            Log.e("ThreadViewModel", "Comment text cannot be empty.")
            return
        }

        val newCommentRef = firestore.collection("threads").document(threadId)
            .collection("comments").document()

        val comment = Comment( // Assuming Comment class is structured appropriately
            id = newCommentRef.id,
            threadId = threadId,
            authorId = userId,
            authorName = authorName,
            text = commentText
            // createdAt will be set by default in Comment data class (e.g. Timestamp(Date()))
        )

        viewModelScope.launch {
            try {
                newCommentRef.set(comment).await()
                Log.d("ThreadViewModel", "Comment added successfully to thread $threadId")
                // Comment count logic removed as it's not in your Thread model
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error adding comment to thread $threadId", e)
            }
        }
    }

    fun createThread(header: String, paragraph: String, topic: String, authorName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("ThreadViewModel", "User not logged in. Cannot create thread.")
            return
        }

        val newThreadRef = firestore.collection("threads").document()

        val thread = Thread( // Uses your model.Thread class
            id = newThreadRef.id,
            authorId = userId,
            authorName = authorName,
            header = header,
            paragraph = paragraph,
            topic = topic,
            timestamp = System.currentTimeMillis() // Uses Long for timestamp
        )

        viewModelScope.launch {
            try {
                newThreadRef.set(thread).await()
                Log.d("ThreadViewModel", "Thread created successfully with ID: ${newThreadRef.id}")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error creating thread", e)
            }
        }
    }

    fun clearSelectedThreadAndComments() {
        _selectedThread.value = null
        _comments.value = emptyList()
    }
}
