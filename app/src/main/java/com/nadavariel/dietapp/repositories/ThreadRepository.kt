package com.nadavariel.dietapp.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.models.Comment
import com.nadavariel.dietapp.models.Like
import com.nadavariel.dietapp.models.Thread
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ThreadRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore

    // --- Real-time Flows ---

    fun getAllThreadsFlow(): Flow<List<Thread>> = callbackFlow {
        val listener = firestore.collection("threads")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ThreadRepo", "Listen failed for all threads.", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val threadList = snapshots?.mapNotNull { document ->
                    document.toObject<Thread>().copy(id = document.id)
                } ?: emptyList()
                trySend(threadList)
            }
        awaitClose { listener.remove() }
    }

    fun getUserThreadsFlow(userId: String): Flow<List<Thread>> = callbackFlow {
        val listener = firestore.collection("threads")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val threadsList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<Thread>()?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(threadsList)
            }
        awaitClose { listener.remove() }
    }

    fun getCommentsFlow(threadId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("threads")
            .document(threadId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val commentList = snapshots?.mapNotNull { document ->
                    document.toObject<Comment>().copy(id = document.id)
                } ?: emptyList()
                trySend(commentList)
            }
        awaitClose { listener.remove() }
    }

    fun getLikesFlow(threadId: String): Flow<List<Like>> = callbackFlow {
        val listener = firestore.collection("threads")
            .document(threadId)
            .collection("likes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val likes = snapshots?.mapNotNull { it.toObject<Like>() } ?: emptyList()
                trySend(likes)
            }
        awaitClose { listener.remove() }
    }

    // --- One-Shot Fetches ---

    suspend fun getThreadById(threadId: String): Thread? {
        val snapshot = firestore.collection("threads").document(threadId).get().await()
        return snapshot.toObject<Thread>()?.copy(id = snapshot.id)
    }

    suspend fun getLikeCount(threadId: String): Int {
        val snapshot = firestore.collection("threads").document(threadId).collection("likes").get().await()
        return snapshot.size()
    }

    suspend fun getCommentCount(threadId: String): Int {
        val snapshot = firestore.collection("threads").document(threadId).collection("comments").get().await()
        return snapshot.size()
    }

    suspend fun getLikeUsernames(threadId: String): List<String> {
        val snapshot = firestore.collection("threads").document(threadId).collection("likes").get().await()
        return snapshot.documents.mapNotNull { it.getString("authorName") }
    }

    // --- CRUD Operations ---

    suspend fun createThread(thread: Thread) {
        val ref = firestore.collection("threads").document() // Auto-ID
        // We set the ID in the object to match the document ID
        val threadWithId = thread.copy(id = ref.id)
        ref.set(threadWithId).await()
    }

    suspend fun deleteThread(threadId: String) {
        firestore.collection("threads").document(threadId).delete().await()
    }

    suspend fun updateThread(threadId: String, updates: Map<String, Any>) {
        firestore.collection("threads").document(threadId).update(updates).await()
    }

    suspend fun addComment(threadId: String, comment: Comment) {
        firestore.collection("threads")
            .document(threadId)
            .collection("comments")
            .add(comment) // Auto-ID for comment
            .await()
    }

    suspend fun toggleLike(threadId: String, userId: String, authorName: String) {
        val likeDocRef = firestore.collection("threads")
            .document(threadId)
            .collection("likes")
            .document(userId)

        val snapshot = likeDocRef.get().await()
        if (snapshot.exists()) {
            likeDocRef.delete().await()
        } else {
            val like = Like(
                userId = userId,
                authorName = authorName,
                timestamp = Timestamp.now()
            )
            likeDocRef.set(like).await()
        }
    }
}