package com.project.e_commerce.android.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import android.util.Log
import kotlinx.coroutines.tasks.await

class UpdateUserLikeCountUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(userId: String, increment: Boolean): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val incrementValue = if (increment) 1 else -1

            userRef.update("likesCount", FieldValue.increment(incrementValue.toLong())).await()

            Log.d(
                "UpdateUserLikeCountUseCase",
                "✅ Updated like count for user $userId by $incrementValue"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UpdateUserLikeCountUseCase", "❌ Error updating user like count: ${e.message}")
            Result.failure(e)
        }
    }
}

class LikeReelUseCase(
    private val firestore: FirebaseFirestore,
    private val updateUserLikeCountUseCase: UpdateUserLikeCountUseCase
) {
    suspend operator fun invoke(reelId: String, userId: String): Result<Boolean> {
        return try {
            // First ensure the reel document exists
            ensureReelDocumentExists(reelId)

            val reelRef = firestore.collection("reels").document(reelId)
            val likeRef = firestore.collection("reel_likes").document("${reelId}_${userId}")

            // Check if user already liked this reel
            val likeDoc = likeRef.get().await()
            val isCurrentlyLiked = likeDoc.exists()

            if (isCurrentlyLiked) {
                // Unlike: Remove like document and decrement count
                firestore.runBatch { batch ->
                    batch.delete(likeRef)
                    batch.update(reelRef, "likeCount", FieldValue.increment(-1))
                }.await()

                // Update user's like count in profile
                updateUserLikeCountUseCase(userId, false)

                Log.d("LikeReelUseCase", "✅ Unliked reel $reelId by user $userId")
                Result.success(false)
            } else {
                // Like: Add like document and increment count
                firestore.runBatch { batch ->
                    batch.set(
                        likeRef, hashMapOf(
                            "reelId" to reelId,
                            "userId" to userId,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(reelRef, "likeCount", FieldValue.increment(1))
                }.await()

                // Update user's like count in profile
                updateUserLikeCountUseCase(userId, true)

                Log.d("LikeReelUseCase", "✅ Liked reel $reelId by user $userId")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("LikeReelUseCase", "❌ Error toggling like for reel $reelId: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun ensureReelDocumentExists(reelId: String) {
        try {
            val reelRef = firestore.collection("reels").document(reelId)
            val reelDoc = reelRef.get().await()

            if (!reelDoc.exists()) {
                // Create the reel document with initial like count
                reelRef.set(
                    hashMapOf(
                        "reelId" to reelId,
                        "likeCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Log.d("LikeReelUseCase", "✅ Created reel document for $reelId")
            }
        } catch (e: Exception) {
            Log.e("LikeReelUseCase", "❌ Error ensuring reel document exists: ${e.message}")
            throw e
        }
    }
}

class GetReelLikeStatusUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(reelId: String, userId: String): Result<Boolean> {
        return try {
            val likeDoc = firestore.collection("reel_likes")
                .document("${reelId}_${userId}")
                .get()
                .await()

            Result.success(likeDoc.exists())
        } catch (e: Exception) {
            Log.e("GetReelLikeStatusUseCase", "❌ Error getting like status: ${e.message}")
            Result.failure(e)
        }
    }
}

class GetReelLikeCountUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(reelId: String): Result<Int> {
        return try {
            val reelDoc = firestore.collection("reels")
                .document(reelId)
                .get()
                .await()

            val likeCount = reelDoc.getLong("likeCount")?.toInt() ?: 0
            Result.success(likeCount)
        } catch (e: Exception) {
            Log.e("GetReelLikeCountUseCase", "❌ Error getting like count: ${e.message}")
            Result.failure(e)
        }
    }
}