package com.project.e_commerce.android.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.project.e_commerce.android.domain.model.FollowRelationship
import com.project.e_commerce.android.domain.model.FollowingStatus
import com.project.e_commerce.android.domain.repository.FollowingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseFollowingRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FollowingRepository {

    companion object {
        private const val TAG = "FirebaseFollowingRepo"
        private const val FOLLOWS_COLLECTION = "follows"
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun followUser(followerId: String, followedId: String): Result<Unit> = try {
        Log.d(TAG, "🔄 Following user: $followerId -> $followedId")
        
        // Prevent self-following
        if (followerId == followedId) {
            throw IllegalArgumentException("Cannot follow yourself")
        }

        val batch = firestore.batch()
        
        // Create follow relationship
        val followDoc = firestore.collection(FOLLOWS_COLLECTION)
            .document("${followerId}_${followedId}")
        
        val followData = FollowRelationship(
            id = "${followerId}_${followedId}",
            followerId = followerId,
            followedId = followedId,
            createdAt = System.currentTimeMillis()
        )
        
        batch.set(followDoc, followData)
        
        // Update follower's following count
        val followerDoc = firestore.collection(USERS_COLLECTION).document(followerId)
        batch.update(followerDoc, "followingCount", FieldValue.increment(1))
        
        // Update followed user's followers count
        val followedDoc = firestore.collection(USERS_COLLECTION).document(followedId)
        batch.update(followedDoc, "followersCount", FieldValue.increment(1))
        
        batch.commit().await()
        Log.d(TAG, "✅ Successfully followed user: $followedId")
        Result.success(Unit)
    } catch (error: Exception) {
        Log.e(TAG, "❌ Failed to follow user: ${error.message}")
        Result.failure(error)
    }

    override suspend fun unfollowUser(followerId: String, followedId: String): Result<Unit> = try {
        Log.d(TAG, "🔄 Unfollowing user: $followerId -> $followedId")
        
        val batch = firestore.batch()
        
        // Remove follow relationship
        val followDoc = firestore.collection(FOLLOWS_COLLECTION)
            .document("${followerId}_${followedId}")
        batch.delete(followDoc)
        
        // Update follower's following count
        val followerDoc = firestore.collection(USERS_COLLECTION).document(followerId)
        batch.update(followerDoc, "followingCount", FieldValue.increment(-1))
        
        // Update followed user's followers count
        val followedDoc = firestore.collection(USERS_COLLECTION).document(followedId)
        batch.update(followedDoc, "followersCount", FieldValue.increment(-1))
        
        batch.commit().await()
        Log.d(TAG, "✅ Successfully unfollowed user: $followedId")
        Result.success(Unit)
    } catch (error: Exception) {
        Log.e(TAG, "❌ Failed to unfollow user: ${error.message}")
        Result.failure(error)
    }

    override suspend fun getFollowingStatus(followerId: String, followedId: String): FollowingStatus {
        Log.d(TAG, "🔍 Getting following status: $followerId -> $followedId")
        
        val isFollowing = isFollowing(followerId, followedId)
        val isFollowedBy = isFollowedBy(followerId, followedId)
        
        return FollowingStatus(
            isFollowing = isFollowing,
            isFollowedBy = isFollowedBy,
            isMutual = isFollowing && isFollowedBy
        )
    }

    override suspend fun isFollowing(followerId: String, followedId: String): Boolean {
        val followDoc = firestore.collection(FOLLOWS_COLLECTION)
            .document("${followerId}_${followedId}")
        
        val snapshot = followDoc.get().await()
        return snapshot.exists()
    }

    override suspend fun isFollowedBy(followerId: String, followedId: String): Boolean {
        val followDoc = firestore.collection(FOLLOWS_COLLECTION)
            .document("${followedId}_${followerId}")
        
        val snapshot = followDoc.get().await()
        return snapshot.exists()
    }

    override suspend fun getFollowers(userId: String): List<String> {
        Log.d(TAG, "🔍 Getting followers for user: $userId")
        
        val snapshot = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followedId", userId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.getString("followerId")
        }
    }

    override suspend fun getFollowing(userId: String): List<String> {
        Log.d(TAG, "🔍 Getting following for user: $userId")
        
        val snapshot = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.getString("followedId")
        }
    }

    override suspend fun getFollowersCount(userId: String): Int {
        val snapshot = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followedId", userId)
            .get()
            .await()
        
        return snapshot.size()
    }

    override suspend fun getFollowingCount(userId: String): Int {
        val snapshot = firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .get()
            .await()
        
        return snapshot.size()
    }

    override fun observeFollowingStatus(followerId: String, followedId: String): Flow<FollowingStatus> {
        val flow = MutableStateFlow(FollowingStatus())
        
        // Set up real-time listener for follow relationship
        val followDoc = firestore.collection(FOLLOWS_COLLECTION)
            .document("${followerId}_${followedId}")
        
        followDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "❌ Error observing following status: ${error.message}")
                return@addSnapshotListener
            }
            
            val isFollowing = snapshot?.exists() ?: false
            
            // Also check if followed by (mutual following)
            firestore.collection(FOLLOWS_COLLECTION)
                .document("${followedId}_${followerId}")
                .get()
                .addOnSuccessListener { followedBySnapshot ->
                    val isFollowedBy = followedBySnapshot.exists()
                    val status = FollowingStatus(
                        isFollowing = isFollowing,
                        isFollowedBy = isFollowedBy,
                        isMutual = isFollowing && isFollowedBy
                    )
                    flow.value = status
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "❌ Error checking followed by status: ${error.message}")
                }
        }
        
        return flow.asStateFlow()
    }

    override fun observeFollowersCount(userId: String): Flow<Int> {
        val flow = MutableStateFlow(0)
        
        firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followedId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error observing followers count: ${error.message}")
                    return@addSnapshotListener
                }
                
                flow.value = snapshot?.size() ?: 0
            }
        
        return flow.asStateFlow()
    }

    override fun observeFollowingCount(userId: String): Flow<Int> {
        val flow = MutableStateFlow(0)
        
        firestore.collection(FOLLOWS_COLLECTION)
            .whereEqualTo("followerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error observing following count: ${error.message}")
                    return@addSnapshotListener
                }
                
                flow.value = snapshot?.size() ?: 0
            }
        
        return flow.asStateFlow()
    }
}
