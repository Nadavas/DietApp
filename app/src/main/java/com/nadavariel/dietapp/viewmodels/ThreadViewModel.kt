package com.nadavariel.dietapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.models.*
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.NewsRepository
import com.nadavariel.dietapp.repositories.ThreadRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class ThreadViewModel(
    private val authRepository: AuthRepository,
    private val threadRepository: ThreadRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    // --- State ---
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

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    private val _isNewsLoading = MutableStateFlow(false)
    val isNewsLoading: StateFlow<Boolean> = _isNewsLoading.asStateFlow()

    private val _newsError = MutableStateFlow<String?>(null)
    val newsError: StateFlow<String?> = _newsError.asStateFlow()

    // --- Jobs & Listeners ---
    private var allThreadsJob: Job? = null
    private var userThreadsJob: Job? = null
    private var commentsJob: Job? = null
    private var likesJob: Job? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        loadNewsArticles()

        authStateListener = authRepository.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                fetchAllThreads()
            } else {
                clearAllListenersAndData()
            }
        }
    }

    // --- News Logic ---

    fun loadNewsArticles() {
        viewModelScope.launch {
            try {
                _isNewsLoading.value = true
                _newsError.value = null
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

    // --- Threads Logic ---

    private fun fetchAllThreads() {
        allThreadsJob?.cancel()
        allThreadsJob = viewModelScope.launch {
            threadRepository.getAllThreadsFlow().collect { list ->
                _threads.value = list
                if (list.isNotEmpty()) {
                    calculateHottestThreads(list)
                }
            }
        }
    }

    fun fetchUserThreads(userId: String) {
        userThreadsJob?.cancel()
        userThreadsJob = viewModelScope.launch {
            threadRepository.getUserThreadsFlow(userId).collect { list ->
                _userThreads.value = list
            }
        }
    }

    // --- CRUD ---

    fun createThread(
        header: String,
        paragraph: String,
        topic: String,
        type: String,
        authorName: String
    ) {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            Log.e("ThreadViewModel", "User not logged in. Cannot create thread.")
            return
        }

        val thread = Thread(
            id = "", // Set by Repository
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
                threadRepository.createThread(thread)
                Log.d("ThreadViewModel", "Thread created successfully")
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error creating thread", e)
            }
        }
    }

    fun deleteThread(threadId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                threadRepository.deleteThread(threadId)
                Log.d("ThreadViewModel", "Thread deleted successfully")
                onSuccess()
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error deleting thread", e)
                onError(e.message ?: "Error deleting thread")
            }
        }
    }

    fun updateThread(threadId: String, newHeader: String, newContent: String, onSuccess: () -> Unit) {
        val updates = mapOf(
            "header" to newHeader,
            "paragraph" to newContent,
            "timestamp" to System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                threadRepository.updateThread(threadId, updates)
                if (_selectedThread.value?.id == threadId) {
                    // Refresh selected thread if needed
                    fetchThreadById(threadId)
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error updating thread", e)
            }
        }
    }

    // --- Details & Stats ---

    private fun calculateHottestThreads(threads: List<Thread>) {
        viewModelScope.launch {
            try {
                val threadsWithStats = threads.map { thread ->
                    async {
                        val likeCount = threadRepository.getLikeCount(thread.id)
                        val commentCount = threadRepository.getCommentCount(thread.id)
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
            }
        }
    }

    fun fetchThreadById(threadId: String) {
        commentsJob?.cancel()
        likesJob?.cancel()

        viewModelScope.launch {
            _selectedThread.value = null
            _comments.value = emptyList()
            try {
                val thread = threadRepository.getThreadById(threadId)
                _selectedThread.value = thread

                if (thread != null) {
                    fetchCommentsForThread(threadId)
                    listenForLikes(threadId)
                }
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error fetching thread by ID", e)
            }
        }
    }

    // --- Comments ---

    private fun fetchCommentsForThread(threadId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            threadRepository.getCommentsFlow(threadId).collect { list ->
                _comments.value = list
            }
        }
    }

    fun addComment(threadId: String, commentText: String, authorName: String) {
        val userId = authRepository.currentUser?.uid ?: return
        if (commentText.isBlank()) return

        val comment = Comment(
            id = "", // Set by Repository
            threadId = threadId,
            authorId = userId,
            authorName = authorName,
            text = commentText,
            createdAt = Timestamp.now()
        )

        viewModelScope.launch {
            try {
                threadRepository.addComment(threadId, comment)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error adding comment", e)
            }
        }
    }

    fun getCommentCountForThread(threadId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(threadRepository.getCommentCount(threadId))
            } catch (_: Exception) {
                onResult(0)
            }
        }
    }

    // --- Likes ---

    fun listenForLikes(threadId: String) {
        likesJob?.cancel()
        val currentUserId = authRepository.currentUser?.uid

        likesJob = viewModelScope.launch {
            threadRepository.getLikesFlow(threadId).collect { likes ->
                _likeCount.value = likes.size
                _hasUserLiked.value = likes.any { it.userId == currentUserId }
            }
        }
    }

    fun toggleLike(threadId: String, userId: String, authorName: String) {
        viewModelScope.launch {
            try {
                threadRepository.toggleLike(threadId, userId, authorName)
            } catch (e: Exception) {
                Log.e("ThreadViewModel", "Error toggling like", e)
            }
        }
    }

    fun getLikesForThread(threadId: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(threadRepository.getLikeUsernames(threadId))
            } catch (_: Exception) {
                onResult(emptyList())
            }
        }
    }

    fun getLikeCountForThread(threadId: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(threadRepository.getLikeCount(threadId))
            } catch (_: Exception) {
                onResult(0)
            }
        }
    }

    // --- Cleanup ---

    fun clearSelectedThreadAndListeners() {
        commentsJob?.cancel()
        likesJob?.cancel()

        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false
    }

    private fun clearAllListenersAndData() {
        allThreadsJob?.cancel()
        userThreadsJob?.cancel()
        commentsJob?.cancel()
        likesJob?.cancel()

        _threads.value = emptyList()
        _hottestThreads.value = emptyList()
        _userThreads.value = emptyList()
        _selectedThread.value = null
        _comments.value = emptyList()
        _likeCount.value = 0
        _hasUserLiked.value = false
        _newsArticles.value = emptyList()

        Log.d("ThreadViewModel", "Cleared all listeners and data.")
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
        clearAllListenersAndData()
    }
}