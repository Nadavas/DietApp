package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.Comment
import com.nadavariel.dietapp.model.Like
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.model.NewsArticle
import com.nadavariel.dietapp.data.NewsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ThreadViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth: FirebaseAuth = Firebase.auth
    private val newsRepository = NewsRepository() // Initialize News Repository

    // --- LISTENER REGISTRATIONS ---
    private var threadsListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var likesListener: ListenerRegistration? = null
    private var userThreadsListener: ListenerRegistration? = null

    // --- THREAD STATE FLOWS ---
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

    private val _userThreads = MutableStateFlow<List<Thread>>(emptyList())
    val userThreads: StateFlow<List<Thread>> = _userThreads.asStateFlow()

    // --- NEWS STATE FLOWS (Added from NewsViewModel) ---
    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError: StateFlow<String?> = _newsError.asStateFlow()

    init {
        // Load News immediately on init
        loadNewsArticles()

        // Load Threads when authenticated
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                fetchAllThreads()
            } else {
                clearAllListenersAndData()
            }
        }
    }

    // --- NEWS FUNCTIONALITY (Added) ---

    fun loadNewsArticles() {
        viewModelScope.launch {
            try {
                _isNewsLoading.value = true
                _newsError.value = null
                // Fetch latest 5 articles
                val articles = newsRepository.fetchLatestArticles(5)
                _newsArticles.value = articles
            } catch (e: Exception) {
                _newsError.value = "Failed to load news articles"
                e.printStackTrace()
            } finally {
                _isNewsLoading.value = false
            }
        }
    }

    fun refreshNews() {
        loadNewsArticles()
    }

    // --- THREAD FUNCTIONALITY (Existing) ---

    private fun fetchAllThreads() {
        threadsListener?.remove()
        viewModelScope.launch {
            try {
                threadsListener = firestore.collection("threads")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            Log.w("ThreadViewModel", "Listen failed for all threads.", e)
                            _threads.value = emptyList()
                            return@addSnapshotListener
                        }
                        val threadList = snapshots?.mapNotNull { document ->
                            document.toObject(Thread::class.java).copy(id = document.id)
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

    // --- USER THREADS FUNCTIONALITY ---

    fun fetchUserThreads(userId: String) {
        userThreadsListener?.remove()

        userThreadsListener = firestore.collection("threads")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ThreadViewModel", "Error fetching user threads", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val threadsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Thread::class.java)?.copy(id = doc.id)
                    }
                    _userThreads.value = threadsList.sortedByDescending { it.timestamp }
                }
            }
    }

    // --- DELETE FUNCTIONALITY ---

    fun deleteThread(threadId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        firestore.collection("threads").document(threadId)
            .delete()
            .addOnSuccessListener {
                Log.d("ThreadViewModel", "Thread deleted successfully: $threadId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ThreadViewModel", "Error deleting thread", e)
                onError(e.message ?: "Error deleting thread")
            }
    }

    // --- UPDATE FUNCTIONALITY ---

    fun updateThread(threadId: String, newHeader: String, newContent: String, onSuccess: () -> Unit) {
        val updates = mapOf(
            "header" to newHeader,
            "paragraph" to newContent,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("threads").document(threadId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ThreadViewModel", "Thread updated successfully: $threadId")
                if (_selectedThread.value?.id == threadId) {
                    fetchThreadById(threadId)
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ThreadViewModel", "Error updating thread", e)
            }
    }

    // --- HELPER FUNCTIONS ---

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
        commentsListener?.remove()
        likesListener?.remove()

        viewModelScope.launch {
            _selectedThread.value = null
            _comments.value = emptyList()
            try {
                val docSnapshot = firestore.collection("threads").document(threadId).get().await()
                _selectedThread.value = docSnapshot.toObject(Thread::class.java)?.copy(id = docSnapshot.id)

                if (_selectedThread.value != null) {
                    fetchCommentsForThread(threadId)
                    listenForLikes(threadId)
                } else {
                    Log.w("ThreadViewModel", "Thread with ID $threadId not found.")
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching thread by ID: $threadId", e)
            }
        }
    }

    private fun fetchCommentsForThread(threadId: String) {
        commentsListener?.remove()
        viewModelScope.launch {
            try {
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
                            document.toObject(Comment::class.java).copy(id = document.id)
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
        likesListener?.remove()
        val currentUserId = auth.currentUser?.uid

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

                val likes = snapshots?.mapNotNull { it.toObject(Like::class.java) } ?: emptyList()
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

    fun clearSelectedThreadAndListeners() {
        commentsListener?.remove()
        commentsListener = null
        likesListener?.remove()
        likesListener = null

        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false
    }

    private fun clearAllListenersAndData() {
        threadsListener?.remove()
        threadsListener = null
        commentsListener?.remove()
        commentsListener = null
        likesListener?.remove()
        likesListener = null
        userThreadsListener?.remove()
        userThreadsListener = null

        _threads.value = emptyList()
        _hottestThreads.value = emptyList()
        _userThreads.value = emptyList()
        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false

        // News cleanup is usually not needed as it's not a listener,
        // but we can clear the list if desired
        _newsArticles.value = emptyList()

        Log.d("ThreadViewModel", "Cleared all listeners and data.")
    }

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