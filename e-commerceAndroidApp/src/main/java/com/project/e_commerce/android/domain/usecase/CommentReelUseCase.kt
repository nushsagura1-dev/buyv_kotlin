package com.project.e_commerce.android.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.presentation.ui.screens.reelsScreen.Comment
import android.util.Log
import kotlinx.coroutines.tasks.await

class AddCommentUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(
        reelId: String,
        userId: String,
        userName: String,
        commentText: String
    ): Result<Comment> {
        return try {
            val commentId = firestore.collection("reel_comments").document().id

            val commentData = hashMapOf<String, Any>(
                "commentId" to commentId,
                "reelId" to reelId,
                "userId" to userId,
                "userName" to userName,
                "comment" to commentText,
                "timestamp" to FieldValue.serverTimestamp(),
                "timestampMillis" to System.currentTimeMillis(),
                "likeCount" to 0L,
                "dislikeCount" to 0L,
                "isEdited" to false
            )

            // Add comment to Firestore
            firestore.collection("reel_comments")
                .document(commentId)
                .set(commentData)
                .await()

            // Update the reel's comment count
            firestore.collection("reels")
                .document(reelId)
                .update("commentCount", FieldValue.increment(1))
                .await()

            val newComment = Comment(
                id = commentId,
                userId = userId,
                userName = userName,
                comment = commentText,
                time = "now",
                isLoved = false
            )

            Log.d("AddCommentUseCase", "✅ Added comment to reel $reelId by user $userId")
            Result.success(newComment)
        } catch (e: Exception) {
            Log.e("AddCommentUseCase", "❌ Error adding comment: ${e.message}")
            Result.failure(e)
        }
    }
}

class GetReelCommentsUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(reelId: String): Result<List<Comment>> {
        return try {
            Log.d("GetReelCommentsUseCase", "🔍 Querying Firebase for reelId: '$reelId'")
            Log.d(
                "GetReelCommentsUseCase",
                "🔍 Query: collection('reel_comments').whereEqualTo('reelId', '$reelId') [NO orderBy to avoid composite index requirement]"
            )

            val commentsSnapshot = firestore.collection("reel_comments")
                .whereEqualTo("reelId", reelId)
                // Removed orderBy to avoid Firebase composite index requirement
                // Will sort in-memory instead
                .get()
                .await()

            Log.d("GetReelCommentsUseCase", "🔍 Firebase query completed")
            Log.d(
                "GetReelCommentsUseCase",
                "🔍 Query returned ${commentsSnapshot.documents.size} documents"
            )

            // Log all documents returned by the query
            commentsSnapshot.documents.forEachIndexed { index, doc ->
                Log.d("GetReelCommentsUseCase", "📄 Document $index: id='${doc.id}'")
                Log.d(
                    "GetReelCommentsUseCase",
                    "📄 Document $index reelId: '${doc.getString("reelId")}'"
                )
                Log.d(
                    "GetReelCommentsUseCase",
                    "📄 Document $index userId: '${doc.getString("userId")}'"
                )
                Log.d(
                    "GetReelCommentsUseCase",
                    "📄 Document $index userName: '${doc.getString("userName")}'"
                )
                Log.d(
                    "GetReelCommentsUseCase",
                    "📄 Document $index comment: '${doc.getString("comment")}'"
                )
                Log.d(
                    "GetReelCommentsUseCase",
                    "📄 Document $index timestampMillis: '${doc.getLong("timestampMillis")}'"
                )
            }

            val comments = commentsSnapshot.documents.mapNotNull { doc ->
                try {
                    val timestampMillis =
                        doc.getLong("timestampMillis") ?: System.currentTimeMillis()
                    val timeAgo = calculateTimeAgo(timestampMillis)

                    val comment = Comment(
                        id = doc.getString("commentId") ?: doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "User",
                        comment = doc.getString("comment") ?: "",
                        time = timeAgo,
                        isLoved = false // This will be determined by checking if current user liked this comment
                    )

                    Log.d(
                        "GetReelCommentsUseCase",
                        "📝 Mapped comment: id='${comment.id}', userName='${comment.userName}', comment='${comment.comment}'"
                    )

                    // Return pair of comment and timestamp for sorting
                    Pair(comment, timestampMillis)
                } catch (e: Exception) {
                    Log.e("GetReelCommentsUseCase", "❌ Error mapping comment: ${e.message}")
                    null
                }
            }.sortedByDescending { it.second } // Sort by timestamp descending (newest first)
                .map { it.first } // Extract just the comments

            Log.d("GetReelCommentsUseCase", "✅ Loaded ${comments.size} comments for reel $reelId")
            Result.success(comments)
        } catch (e: Exception) {
            Log.e("GetReelCommentsUseCase", "❌ Error loading comments: ${e.message}")
            Log.e("GetReelCommentsUseCase", "❌ Exception details: ${e.printStackTrace()}")
            Result.failure(e)
        }
    }

    private fun calculateTimeAgo(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - timestampMillis
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffSeconds < 60 -> "now"
            diffMinutes < 60 -> "${diffMinutes}m"
            diffHours < 24 -> "${diffHours}h"
            diffDays < 7 -> "${diffDays}d"
            else -> "${diffDays / 7}w"
        }
    }
}

class LikeCommentUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(commentId: String, userId: String): Result<Boolean> {
        return try {
            val likeRef = firestore.collection("comment_likes").document("${commentId}_${userId}")
            val commentRef = firestore.collection("reel_comments").document(commentId)

            // Check if user already liked this comment
            val likeDoc = likeRef.get().await()
            val isCurrentlyLiked = likeDoc.exists()

            if (isCurrentlyLiked) {
                // Unlike: Remove like document and decrement count
                firestore.runBatch { batch ->
                    batch.delete(likeRef)
                    batch.update(commentRef, "likeCount", FieldValue.increment(-1))
                }.await()
                Log.d("LikeCommentUseCase", "✅ Unliked comment $commentId by user $userId")
                Result.success(false)
            } else {
                // Like: Add like document and increment count
                firestore.runBatch { batch ->
                    batch.set(
                        likeRef, hashMapOf(
                            "commentId" to commentId,
                            "userId" to userId,
                            "timestamp" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(commentRef, "likeCount", FieldValue.increment(1))
                }.await()
                Log.d("LikeCommentUseCase", "✅ Liked comment $commentId by user $userId")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("LikeCommentUseCase", "❌ Error toggling comment like: ${e.message}")
            Result.failure(e)
        }
    }
}

class GetCommentLikeStatusUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(commentId: String, userId: String): Result<Boolean> {
        return try {
            val likeDoc = firestore.collection("comment_likes")
                .document("${commentId}_${userId}")
                .get()
                .await()

            Result.success(likeDoc.exists())
        } catch (e: Exception) {
            Log.e(
                "GetCommentLikeStatusUseCase",
                "❌ Error getting comment like status: ${e.message}"
            )
            Result.failure(e)
        }
    }
}

class GetCommentLikeCountUseCase(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(commentId: String): Result<Long> {
        return try {
            val commentDoc = firestore.collection("reel_comments")
                .document(commentId)
                .get()
                .await()

            val likeCount = commentDoc.getLong("likeCount") ?: 0
            Result.success(likeCount)
        } catch (e: Exception) {
            Log.e("GetCommentLikeCountUseCase", "❌ Error getting comment like count: ${e.message}")
            Result.failure(e)
        }
    }
}