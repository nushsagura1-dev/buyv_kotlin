package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Reels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date

class RecentlyViewedRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val COLLECTION_RECENTLY_VIEWED = "recently_viewed"
        private const val MAX_RECENTLY_VIEWED = 50 // Keep last 50 viewed reels
    }

    fun addReelToRecentlyViewed(reel: Reels) {
        val currentUser = auth.currentUser
        Log.d("RecentlyViewedRepository", "Adding reel ${reel.id} to recently viewed")

        if (currentUser == null) {
            Log.e("RecentlyViewedRepository", "No authenticated user found")
            return
        }

        try {
            val viewedReel = mapOf(
                "userId" to currentUser.uid,
                "reelId" to reel.id,
                "reelUserId" to reel.userId,
                "userName" to reel.userName,
                "userImage" to reel.userImage,
                "productName" to reel.productName,
                "productPrice" to reel.productPrice,
                "productImage" to reel.productImage,
                "contentDescription" to reel.contentDescription,
                "video" to reel.video?.toString(),
                "images" to reel.images?.map { it.toString() },
                "fallbackImageRes" to reel.fallbackImageRes,
                "viewedAt" to Date(),
                "timestamp" to System.currentTimeMillis()
            )

            val documentId = "${currentUser.uid}_${reel.id}"

            // Use reelId as document ID to avoid duplicates
            firestore.collection(COLLECTION_RECENTLY_VIEWED)
                .document(documentId)
                .set(viewedReel)
                .addOnSuccessListener {
                    Log.d(
                        "RecentlyViewedRepository",
                        "✅ Successfully added reel ${reel.id} to recently viewed"
                    )
                    // Clean up old entries
                    cleanupOldEntries(currentUser.uid)
                }
                .addOnFailureListener { e ->
                    Log.e("RecentlyViewedRepository", "❌ Failed to add reel to recently viewed", e)
                }
        } catch (e: Exception) {
            Log.e("RecentlyViewedRepository", "❌ Error adding reel to recently viewed", e)
        }
    }

    fun getRecentlyViewedReels(): Flow<List<Reels>> = flow {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("RecentlyViewedRepository", "No authenticated user found for retrieval")
            emit(emptyList())
            return@flow
        }

        try {
            Log.d("RecentlyViewedRepository", "Loading recently viewed reels...")

            try {
                // Try with ordering first
                val querySnapshot = firestore.collection(COLLECTION_RECENTLY_VIEWED)
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(MAX_RECENTLY_VIEWED.toLong())
                    .get()
                    .await()

                Log.d(
                    "RecentlyViewedRepository",
                    "Found ${querySnapshot.documents.size} recently viewed reels"
                )

                val reels = processDocuments(querySnapshot.documents)
                emit(reels)

            } catch (indexError: Exception) {
                Log.w(
                    "RecentlyViewedRepository",
                    "Ordered query failed (likely missing index), using fallback"
                )

                // Fallback: Simple query without ordering
                val querySnapshot = firestore.collection(COLLECTION_RECENTLY_VIEWED)
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(MAX_RECENTLY_VIEWED.toLong())
                    .get()
                    .await()

                Log.d(
                    "RecentlyViewedRepository",
                    "Fallback query found ${querySnapshot.documents.size} reels"
                )

                val reels = processDocuments(querySnapshot.documents)
                    .sortedByDescending {
                        // Sort in memory since we can't sort in query
                        try {
                            (it as? Reels)?.let { reel ->
                                // We can't access the timestamp directly, so we sort by reel ID which contains timestamp
                                reel.id.substringAfterLast("_").toLongOrNull() ?: 0L
                            } ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }
                    .take(MAX_RECENTLY_VIEWED)

                emit(reels)
            }

        } catch (e: Exception) {
            Log.e("RecentlyViewedRepository", "❌ Error fetching recently viewed reels", e)
            emit(emptyList())
        }
    }

    private fun processDocuments(documents: List<com.google.firebase.firestore.DocumentSnapshot>): List<Reels> {
        return documents.mapNotNull { document ->
            try {
                val data = document.data

                if (data == null) {
                    Log.w("RecentlyViewedRepository", "Document ${document.id} has null data")
                    return@mapNotNull null
                }

                // Convert Firebase document to Reels object
                val reel = Reels(
                    id = data["reelId"] as? String ?: "",
                    userId = data["reelUserId"] as? String ?: "",
                    userName = data["userName"] as? String ?: "",
                    userImage = (data["userImage"] as? Long)?.toInt() ?: 0,
                    video = (data["video"] as? String)?.let {
                        if (it.isNotEmpty()) android.net.Uri.parse(it) else null
                    },
                    images = (data["images"] as? List<*>)?.mapNotNull { url ->
                        (url as? String)?.let {
                            if (it.isNotEmpty()) android.net.Uri.parse(it) else null
                        }
                    },
                    fallbackImageRes = (data["fallbackImageRes"] as? Long)?.toInt()
                        ?: com.project.e_commerce.android.R.drawable.reelsphoto,
                    contentDescription = data["contentDescription"] as? String ?: "",
                    productName = data["productName"] as? String ?: "",
                    productPrice = data["productPrice"] as? String ?: "",
                    productImage = data["productImage"] as? String ?: "",
                    isLoading = false,
                    isError = false
                )

                reel
            } catch (e: Exception) {
                Log.e(
                    "RecentlyViewedRepository",
                    "Error parsing reel document ${document.id}",
                    e
                )
                null
            }
        }
    }

    private fun cleanupOldEntries(userId: String) {
        try {
            firestore.collection(COLLECTION_RECENTLY_VIEWED)
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val documents = querySnapshot.documents
                    if (documents.size > MAX_RECENTLY_VIEWED) {
                        // Delete oldest entries
                        val toDelete = documents.drop(MAX_RECENTLY_VIEWED)
                        toDelete.forEach { document ->
                            document.reference.delete()
                        }
                        Log.d("RecentlyViewedRepository", "Cleaned up ${toDelete.size} old entries")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RecentlyViewedRepository", "Failed to cleanup old entries", e)
                }
        } catch (e: Exception) {
            Log.e("RecentlyViewedRepository", "Error during cleanup", e)
        }
    }

    fun clearRecentlyViewed() {
        val currentUser = auth.currentUser ?: return

        try {
            firestore.collection(COLLECTION_RECENTLY_VIEWED)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { document ->
                        batch.delete(document.reference)
                    }
                    batch.commit().addOnSuccessListener {
                        Log.d("RecentlyViewedRepository", "Successfully cleared recently viewed")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RecentlyViewedRepository", "Failed to clear recently viewed", e)
                }
        } catch (e: Exception) {
            Log.e("RecentlyViewedRepository", "Error clearing recently viewed", e)
        }
    }

    fun clearTestData() {
        val currentUser = auth.currentUser ?: return

        try {
            // Clear test reels that contain "test_reel" or "Test User"
            firestore.collection(COLLECTION_RECENTLY_VIEWED)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { document ->
                        val data = document.data
                        val reelId = data?.get("reelId") as? String ?: ""
                        val userName = data?.get("userName") as? String ?: ""

                        // Delete test data
                        if (reelId.contains("test_reel") || userName.contains("Test User")) {
                            batch.delete(document.reference)
                            Log.d(
                                "RecentlyViewedRepository",
                                "Marking test data for deletion: $reelId"
                            )
                        }
                    }
                    batch.commit().addOnSuccessListener {
                        Log.d("RecentlyViewedRepository", "Successfully cleared test data")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RecentlyViewedRepository", "Failed to clear test data", e)
                }
        } catch (e: Exception) {
            Log.e("RecentlyViewedRepository", "Error clearing test data", e)
        }
    }
}