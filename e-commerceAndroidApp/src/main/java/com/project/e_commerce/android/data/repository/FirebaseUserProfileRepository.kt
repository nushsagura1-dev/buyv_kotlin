package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.e_commerce.android.domain.model.*
import com.project.e_commerce.android.domain.repository.UserProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseUserProfileRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val POSTS_COLLECTION = "posts"
        private const val PRODUCTS_COLLECTION = "products"
        private const val INTERACTIONS_COLLECTION = "interactions"
        private const val FOLLOWERS_COLLECTION = "followers"
        private const val FOLLOWING_COLLECTION = "following"
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile> = runCatching {
        Log.d("REPO_DEBUG", "🔍 Getting profile for uid: $uid")
        val document = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        
        if (document.exists()) {
            Log.d("REPO_DEBUG", "📄 Document exists, data: ${document.data}")
            
            // Check if it's the old format (just username) or new format (full UserProfile)
            val existingUsername = document.getString("username")
            val existingProfile = document.toObject(UserProfile::class.java)
            
            if (existingProfile != null) {
                // New format - return as is
                Log.d("REPO_DEBUG", "✅ Found new format profile: ${existingProfile.username}")
                existingProfile
            } else if (existingUsername != null) {
                // Old format - convert to new format
                Log.d("REPO_DEBUG", "🔄 Converting old format username: $existingUsername")
                val profile = UserProfile(
                    uid = uid,
                    email = auth.currentUser?.email ?: "",
                    displayName = existingUsername, // Use username as display name initially
                    username = existingUsername,
                    bio = "",
                    profileImageUrl = null,
                    followersCount = 0,
                    followingCount = 0,
                    likesCount = 0,
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = System.currentTimeMillis()
                )
                // Update to new format
                updateUserProfile(profile).getOrThrow()
            } else {
                // No data - create default profile
                Log.d("REPO_DEBUG", "📝 No data found, creating default profile")
                val defaultProfile = UserProfile(
                    uid = uid,
                    email = auth.currentUser?.email ?: "",
                    displayName = "",
                    username = ""
                )
                createUserProfile(defaultProfile).getOrThrow()
            }
        } else {
            // Create default profile if doesn't exist
            Log.d("REPO_DEBUG", "📝 Document doesn't exist, creating default profile")
            val defaultProfile = UserProfile(
                uid = uid,
                email = auth.currentUser?.email ?: "",
                displayName = "",
                username = ""
            )
            createUserProfile(defaultProfile).getOrThrow()
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> = runCatching {
        val updatedProfile = profile.copy(lastUpdated = System.currentTimeMillis())
        firestore.collection(USERS_COLLECTION)
            .document(profile.uid)
            .set(updatedProfile)
            .await()
        updatedProfile
    }

    override suspend fun createUserProfile(profile: UserProfile): Result<UserProfile> = runCatching {
        firestore.collection(USERS_COLLECTION)
            .document(profile.uid)
            .set(profile)
            .await()
        profile
    }

    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val listener = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profile = snapshot?.toObject(UserProfile::class.java)
                trySend(profile)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun getUserPosts(uid: String): Result<List<UserPost>> = runCatching {
        val snapshot = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { it.toObject(UserPost::class.java) }
    }

    override suspend fun getUserReels(uid: String): Result<List<UserPost>> = runCatching {
        Log.d("REPO_DEBUG", "🔍 Getting reels for user: $uid")
        
        try {
            // First, let's check what's actually in the posts collection
            val allPostsSnapshot = firestore.collection(POSTS_COLLECTION).get().await()
            Log.d("REPO_DEBUG", "📄 Total posts in collection: ${allPostsSnapshot.documents.size}")
            allPostsSnapshot.documents.forEach { doc ->
                Log.d("REPO_DEBUG", "📄 All post: userId=${doc.getString("userId")}, type=${doc.getString("type")}, title=${doc.getString("title")}")
            }
            
            // Now get user's reels - removed orderBy to avoid potential issues
            val snapshot = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("userId", uid)
                .whereEqualTo("type", "REEL")
                .whereEqualTo("isPublished", true)
                .get()
                .await()
            
            Log.d("REPO_DEBUG", "📄 Found ${snapshot.documents.size} reel documents for user $uid")
            snapshot.documents.forEach { doc ->
                Log.d("REPO_DEBUG", "📄 Reel doc: ${doc.data}")
            }
            
            // Try to parse each document
            val reels = mutableListOf<UserPost>()
            snapshot.documents.forEach { doc ->
                try {
                    val userPost = doc.toObject(UserPost::class.java)
                    if (userPost != null) {
                        Log.d("REPO_DEBUG", "✅ Successfully parsed reel: ${userPost.title}")
                        reels.add(userPost)
                    } else {
                        Log.e("REPO_DEBUG", "❌ Failed to parse reel document: ${doc.id}")
                    }
                } catch (e: Exception) {
                    Log.e("REPO_DEBUG", "❌ Error parsing reel document ${doc.id}: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            Log.d("REPO_DEBUG", "✅ Parsed ${reels.size} reels successfully")
            reels
            
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "❌ Error in getUserReels: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    override suspend fun getUserProducts(uid: String): Result<List<UserProduct>> = runCatching {
        val snapshot = firestore.collection(PRODUCTS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { it.toObject(UserProduct::class.java) }
    }

    override fun getUserPostsFlow(uid: String): Flow<List<UserPost>> = callbackFlow {
        val listener = firestore.collection(POSTS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("isPublished", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val posts = snapshot?.documents?.mapNotNull { it.toObject(UserPost::class.java) } ?: emptyList()
                trySend(posts)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun getUserLikedPosts(uid: String): Result<List<UserPost>> = runCatching {
        val interactions = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.LIKE)
            .get()
            .await()
        
        val postIds = interactions.documents.mapNotNull { it.getString("targetId") }
        if (postIds.isEmpty()) return@runCatching emptyList()
        
        val posts = mutableListOf<UserPost>()
        for (postId in postIds) {
            val postDoc = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            postDoc.toObject(UserPost::class.java)?.let { posts.add(it) }
        }
        posts
    }

    override suspend fun getUserLikedProducts(uid: String): Result<List<UserProduct>> = runCatching {
        val interactions = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.LIKE)
            .get()
            .await()
        
        val productIds = interactions.documents.mapNotNull { it.getString("targetId") }
        if (productIds.isEmpty()) return@runCatching emptyList()
        
        val products = mutableListOf<UserProduct>()
        for (productId in productIds) {
            val productDoc = firestore.collection(PRODUCTS_COLLECTION).document(productId).get().await()
            productDoc.toObject(UserProduct::class.java)?.let { products.add(it) }
        }
        products
    }

    override suspend fun getUserBookmarkedPosts(uid: String): Result<List<UserPost>> = runCatching {
        val interactions = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.BOOKMARK)
            .get()
            .await()
        
        val postIds = interactions.documents.mapNotNull { it.getString("targetId") }
        if (postIds.isEmpty()) return@runCatching emptyList()
        
        val posts = mutableListOf<UserPost>()
        for (postId in postIds) {
            val postDoc = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            postDoc.toObject(UserPost::class.java)?.let { posts.add(it) }
        }
        posts
    }

    override suspend fun getUserBookmarkedProducts(uid: String): Result<List<UserProduct>> = runCatching {
        val interactions = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.BOOKMARK)
            .get()
            .await()
        
        val productIds = interactions.documents.mapNotNull { it.getString("targetId") }
        if (productIds.isEmpty()) return@runCatching emptyList()
        
        val products = mutableListOf<UserProduct>()
        for (productId in productIds) {
            val productDoc = firestore.collection(PRODUCTS_COLLECTION).document(productId).get().await()
            productDoc.toObject(UserProduct::class.java)?.let { products.add(it) }
        }
        products
    }

    override fun getUserLikedContentFlow(uid: String): Flow<List<UserPost>> = callbackFlow {
        val listener = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.LIKE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                // For now, return empty list. In a real app, you'd fetch the actual posts
                trySend(emptyList())
            }
        
        awaitClose { listener.remove() }
    }

    override fun getUserBookmarkedContentFlow(uid: String): Flow<List<UserPost>> = callbackFlow {
        val listener = firestore.collection(INTERACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .whereEqualTo("targetType", InteractionType.BOOKMARK)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                // For now, return empty list. In a real app, you'd fetch the actual posts
                trySend(emptyList())
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> = runCatching {
        // Add to following collection
        firestore.collection(USERS_COLLECTION)
            .document(currentUserId)
            .collection(FOLLOWING_COLLECTION)
            .document(targetUserId)
            .set(mapOf("followedAt" to System.currentTimeMillis()))
            .await()
        
        // Add to followers collection
        firestore.collection(USERS_COLLECTION)
            .document(targetUserId)
            .collection(FOLLOWERS_COLLECTION)
            .document(currentUserId)
            .set(mapOf("followedAt" to System.currentTimeMillis()))
            .await()
        
        // Update follower counts
        updateFollowerCounts(currentUserId, targetUserId, true)
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> = runCatching {
        // Remove from following collection
        firestore.collection(USERS_COLLECTION)
            .document(currentUserId)
            .collection(FOLLOWING_COLLECTION)
            .document(targetUserId)
            .delete()
            .await()
        
        // Remove from followers collection
        firestore.collection(USERS_COLLECTION)
            .document(targetUserId)
            .collection(FOLLOWERS_COLLECTION)
            .document(currentUserId)
            .delete()
            .await()
        
        // Update follower counts
        updateFollowerCounts(currentUserId, targetUserId, false)
    }

    override suspend fun getFollowers(userId: String): Result<List<String>> = runCatching {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FOLLOWERS_COLLECTION)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { it.id }
    }

    override suspend fun getFollowing(userId: String): Result<List<String>> = runCatching {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FOLLOWING_COLLECTION)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { it.id }
    }

    override suspend fun uploadProfileImage(uid: String, imageUri: String): Result<String> = runCatching {
        // For now, return the URI. In a real app, you'd upload to Firebase Storage
        imageUri
    }

    override suspend fun deleteProfileImage(uid: String): Result<Unit> = runCatching {
        // Update profile to remove image URL
        val profile = getUserProfile(uid).getOrThrow()
        updateUserProfile(profile.copy(profileImageUrl = null))
    }

    private suspend fun updateFollowerCounts(currentUserId: String, targetUserId: String, isFollowing: Boolean) {
        val increment = if (isFollowing) 1 else -1
        
        // Update current user's following count
        firestore.collection(USERS_COLLECTION).document(currentUserId)
            .update("followingCount", increment)
            .await()
        
        // Update target user's followers count
        firestore.collection(USERS_COLLECTION).document(targetUserId)
            .update("followersCount", increment)
            .await()
    }
}
