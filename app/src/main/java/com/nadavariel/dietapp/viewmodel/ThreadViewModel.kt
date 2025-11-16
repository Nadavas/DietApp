package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth // <-- IMPORT ADDED
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration // <-- IMPORT ADDED
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.Comment
import com.nadavariel.dietapp.model.Like
import com.nadavariel.dietapp.model.Thread
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ThreadViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth // <-- 1. ADD AUTH INSTANCE

    // --- LISTENER REGISTRATIONS FOR CLEANUP ---
    private var threadsListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null

    // --- EXISTING STATE FLOWS ---
    private val _threads = MutableStateFlow<List<Thread>>(emptyList())
    val threads: StateFlow<List<Thread>> = _threads.asStateFlow()

    private val _selectedThread = MutableStateFlow<Thread?>(null)
    val selectedThread: StateFlow<Thread?> = _selectedThread.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount.asStateFlow()

    private val _hasUserLiked = MutableStateFlow(false)
    val hasUserLiked: StateFlow<Boolean> = _hasUserLiked.asStateFlow()

    private val _hottestThreads = MutableStateFlow<List<Thread>>(emptyList())
    val hottestThreads: StateFlow<List<Thread>> = _hottestThreads.asStateFlow()

    init {
        // 2. ADD AUTH STATE LISTENER
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is logged in, now it's safe to fetch
                fetchAllThreads()
            } else {
                // User logged out, clear all data and listeners
                clearAllListenersAndData()
            }
        }
    }

    private fun fetchAllThreads() {
        // 3. Clear any previous listener
        threadsListener?.remove()
        viewModelScope.launch {
            try {
                // 4. Save the new listener
                threadsListener = firestore.collection("threads")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("ThreadViewModel", "Listen failed for all threads.", e)
                            _threads.value = emptyList()
                            return@addSnapshotListener
                        }
                        val threadList = snapshots?.mapNotNull { document ->
                            document.toObject<Thread>().copy(id = document.id)
                        } ?: emptyList()

                        _threads.value = threadList
                        if (threadList.isNotEmpty()) {
                            calculateHottestThreads(threadList)
                        }
                    }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching all threads", e)
                _threads.value = emptyList()
            }
        }
    }

    private data class ThreadWithStats(val thread: Thread, val score: Int)

    private fun calculateHottestThreads(threads: List<Thread>) {
        viewModelScope.launch {
            try {
                val threadsWithStats = threads.map { thread ->
                    async {
                        val likeCount = fetchLikeCount(thread.id)
                        val commentCount = fetchCommentCount(thread.id)
                        ThreadWithStats(thread, likeCount + commentCount)
                    }
                }.awaitAll()

                _hottestThreads.value = threadsWithStats
                    .sortedByDescending { it.score }
                    .take(3)
                    .map { it.thread }
                Log.d("ThreadViewModel", "Hottest threads updated.")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error calculating hottest threads", e)
                _hottestThreads.value = emptyList()
            }
        }
    }

    private suspend fun fetchLikeCount(threadId: String): Int {
        return try {
            val snapshot = firestore.collection("threads")
                .document(threadId)
                .collection("likes")
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("ThreadViewModel", "Error suspend fetching like count for thread $threadId", e)
            0
        }
    }

    private suspend fun fetchCommentCount(threadId: String): Int {
        return try {
            val snapshot = firestore.collection("threads")
                .document(threadId)
                .collection("comments")
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("ThreadViewModel", "Error suspend fetching comment count for thread $threadId", e)
            0
        }
    }

    fun fetchThreadById(threadId: String) {
        // Clear previous listeners before fetching new thread data
        commentsListener?.remove()
        likesListener?.remove()

        viewModelScope.launch {
            _selectedThread.value = null
            _comments.value = emptyList()
            try {
                val docSnapshot = firestore.collection("threads").document(threadId).get().await()
                _selectedThread.value = docSnapshot.toObject<Thread>()?.copy(id = docSnapshot.id)

                if (_selectedThread.value != null) {
                    // Fetch comments and likes for the *new* thread
                    fetchCommentsForThread(threadId)
                    listenForLikes(threadId) // This was missing, good to have
                } else {
                    Log.w("ThreadViewModel", "Thread with ID $threadId not found.")
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching thread by ID: $threadId", e)
            }
        }
    }

    private fun fetchCommentsForThread(threadId: String) {
        commentsListener?.remove() // 5. Clear previous comments listener
        viewModelScope.launch {
            try {
                // 6. Save comments listener
                commentsListener = firestore.collection("threads")
                    .document(threadId)
                    .collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("ThreadViewModel", "Listen failed for comments on thread $threadId.", e)
                            _comments.value = emptyList()
                            return@addSnapshotListener
                        }
                        val commentList = snapshots?.mapNotNull { document ->
                            document.toObject<Comment>().copy(id = document.id)
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

        val newCommentRef = firestore.collection("threads")
            .document(threadId)
            .collection("comments")
            .document()

        val comment = Comment(
            id = newCommentRef.id,
            threadId = threadId,
            authorId = userId,
            authorName = authorName,
            text = commentText,
            createdAt = Timestamp.now()
        )

        viewModelScope.launch {
            try {
                newCommentRef.set(comment).await()
                Log.d("ThreadViewModel", "Comment added successfully to thread $threadId")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error adding comment to thread $threadId", e)
            }
        }
    }

    fun listenForLikes(threadId: String) {
        likesListener?.remove() // 7. Clear previous likes listener
        val currentUserId = auth.currentUser?.uid

        // 8. Save likes listener
        likesListener = firestore.collection("threads")
            .document(threadId)
            .collection("likes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ThreadViewModel", "Listen failed for likes on thread $threadId.", e)
                    _likeCount.value = 0
                    _hasUserLiked.value = false
                    return@addSnapshotListener
                }

                val likes = snapshots?.mapNotNull { it.toObject<Like>() } ?: emptyList()
                _likeCount.value = likes.size
                _hasUserLiked.value = likes.any { it.userId == currentUserId }
            }
    }

    fun toggleLike(threadId: String, userId: String, authorName: String) {
        val likeDocRef = firestore.collection("threads")
            .document(threadId)
            .collection("likes")
            .document(userId)

        viewModelScope.launch {
            try {
                val snapshot = likeDocRef.get().await()
                if (snapshot.exists()) {
                    likeDocRef.delete().await()
                    Log.d("ThreadViewModel", "Removed like from user $userId on thread $threadId")
                } else {
                    val like = Like(
                        userId = userId,
                        authorName = authorName,
                        timestamp = Timestamp.now()
                    )
                    likeDocRef.set(like).await()
                    Log.d("ThreadViewModel", "Added like from user $userId on thread $threadId")
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error toggling like on thread $threadId", e)
            }
        }
    }

    fun createThread(
        header: String,
        paragraph: String,
        topic: String,
        type: String,
        authorName: String
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("ThreadViewModel", "User not logged in. Cannot create thread.")
            return
        }

        val newThreadRef = firestore.collection("threads").document()
        val thread = Thread(
            id = newThreadRef.id,
            authorId = userId,
            authorName = authorName,
            header = header,
            paragraph = paragraph,
            topic = topic,
            type = type,
            timestamp = System.currentTimeMillis()
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

    // 9. RENAME function to be more descriptive
    fun clearSelectedThreadAndListeners() {
        // Detach listeners
        commentsListener?.remove()
        commentsListener = null
        likesListener?.remove()
        likesListener = null

        // Reset states
        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false
    }

    // 10. NEW FUNCTION to clear everything on logout
    private fun clearAllListenersAndData() {
        threadsListener?.remove()
        threadsListener = null
        commentsListener?.remove()
        commentsListener = null
        likesListener?.remove()
        likesListener = null

        _threads.value = emptyList()
        _hottestThreads.value = emptyList()
        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false
        Log.d("ThreadViewModel", "Cleared all listeners and data.")
    }

    // 11. OVERRIDE onCleared for proper ViewModel lifecycle cleanup
    override fun onCleared() {
        super.onCleared()
        clearAllListenersAndData()
    }


    fun getLikesForThread(threadId: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("threads")
                    .document(threadId)
                    .collection("likes")
                    .get()
                    .await()

                val usernames = snapshot.documents.mapNotNull { it.getString("authorName") }
                onResult(usernames)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching likes for thread $threadId", e)
                onResult(emptyList())
            }
        }
    }

    fun getLikeCountForThread(threadId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("threads")
                    .document(threadId)
                    .collection("likes")
                    .get()
                    .await()

                onResult(snapshot.documents.size)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching like count for thread $threadId", e)
                onResult(0)
            }
        }
    }

    fun getCommentCountForThread(threadId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("threads")
                    .document(threadId)
                    .collection("comments")
                    .get()
                    .await()

                onResult(snapshot.documents.size)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching comment count for thread $threadId", e)
                onResult(0)
            }
        }
    }
}