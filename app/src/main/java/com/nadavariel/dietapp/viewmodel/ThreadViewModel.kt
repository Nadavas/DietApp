package com.nadavariel.dietapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nadavariel.dietapp.model.Thread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ThreadViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _threads = MutableStateFlow<List<Thread>>(emptyList())
    val threads: StateFlow<List<Thread>> = _threads

    init {
        fetchThreads()
    }

    fun createThread(title: String, topic: String, authorName: String) {
        val uid = auth.currentUser?.uid ?: return
        val newThreadRef = firestore.collection("threads").document()
        val thread = Thread(
            id = newThreadRef.id,
            title = title,
            topic = topic,
            authorId = uid,
            authorName = authorName,
            timestamp = System.currentTimeMillis()
        )

        newThreadRef.set(thread)
    }

    private fun fetchThreads() {
        firestore.collection("threads")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(Thread::class.java) }
                    _threads.value = list
                }
            }
    }
}
