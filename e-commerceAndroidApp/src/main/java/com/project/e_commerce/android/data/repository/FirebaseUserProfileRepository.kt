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

    init {
        Log.d("REPO_DEBUG", "🚀 FirebaseUserProfileRepository initialized")
        Log.d("REPO_DEBUG", "🔍 Auth instance: ${auth.app.name}")
        Log.d("REPO_DEBUG", "🔍 Firestore instance: ${firestore.app.name}")
        
        // Test Firestore connection
        try {
            val testCollection = firestore.collection("test")
            Log.d("REPO_DEBUG", "✅ Firestore connection test successful")
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "❌ Firestore connection test failed: ${e.message}")
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val POSTS_COLLECTION = "posts"
        private const val PRODUCTS_COLLECTION = "products"
        private const val INTERACTIONS_COLLECTION = "interactions"
        private const val FOLLOWERS_COLLECTION = "followers"
        private const val FOLLOWING_COLLECTION = "following"
    }

    override suspend fun getUserProfile(uid: String): Result<UserProfile> = runCatching {
        // Validate UID before proceeding
        if (uid.isBlank()) {
            Log.e("REPO_DEBUG", "❌ Invalid UID: UID is blank or empty")
            throw IllegalArgumentException("User ID cannot be blank")
        }
        
        Log.d("REPO_DEBUG", "🔍 Getting profile for uid: '$uid'")
        Log.d("REPO_DEBUG", "🔍 UID length: ${uid.length}")
        Log.d("REPO_DEBUG", "🔍 UID contains only whitespace: ${uid.isBlank()}")
        Log.d("REPO_DEBUG", "📁 Document path: ${USERS_COLLECTION}/$uid")
        
        val documentRef: com.google.firebase.firestore.DocumentReference
        val document: com.google.firebase.firestore.DocumentSnapshot
        
        try {
            documentRef = firestore.collection(USERS_COLLECTION).document(uid)
            Log.d("REPO_DEBUG", "📄 Document reference: ${documentRef.path}")
            
            document = documentRef.get().await()
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "❌ Error creating document reference: ${e.message}")
            Log.e("REPO_DEBUG", "❌ UID value: '$uid'")
            Log.e("REPO_DEBUG", "❌ USERS_COLLECTION: '$USERS_COLLECTION'")
            throw e
        }
        
        if (document.exists()) {
            Log.d("REPO_DEBUG", "📄 Document exists, data: ${document.data}")

            // Debug the specific fields we care about
            Log.d("REPO_DEBUG", "🖼️ ===== FIREBASE PROFILE DEBUG =====")
            Log.d("REPO_DEBUG", "🖼️ Document ID: $uid")
            Log.d("REPO_DEBUG", "🖼️ Raw document data: ${document.data}")
            Log.d("REPO_DEBUG", "🖼️ displayName: '${document.getString("displayName")}'")
            Log.d("REPO_DEBUG", "🖼️ username: '${document.getString("username")}'")
            Log.d("REPO_DEBUG", "🖼️ email: '${document.getString("email")}'")
            Log.d("REPO_DEBUG", "🖼️ profileImageUrl: '${document.getString("profileImageUrl")}'")
            Log.d(
                "REPO_DEBUG",
                "🖼️ profileImageUrl exists: ${document.contains("profileImageUrl")}"
            )
            Log.d(
                "REPO_DEBUG",
                "🖼️ profileImageUrl is null: ${document.getString("profileImageUrl") == null}"
            )
            Log.d("REPO_DEBUG", "🖼️ ==========================================")

            // Check if it's the old format (just username) or new format (full UserProfile)
            val existingUsername = document.getString("username")
            val existingProfile = document.toObject(UserProfile::class.java)
            
            if (existingProfile != null) {
                // New format - return as is
                Log.d("REPO_DEBUG", "✅ Found new format profile: ${existingProfile.username}")
                Log.d(
                    "REPO_DEBUG",
                    "🖼️ New format profile - profileImageUrl: '${existingProfile.profileImageUrl}'"
                )
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
        // Validate UID before proceeding
        if (profile.uid.isBlank()) {
            Log.e("REPO_DEBUG", "❌ Invalid UID: UID is blank or empty")
            throw IllegalArgumentException("User ID cannot be blank")
        }
        
        Log.d("REPO_DEBUG", "🔄 Updating profile for UID: '${profile.uid}'")
        Log.d("REPO_DEBUG", "🔄 UID length: ${profile.uid.length}")
        Log.d("REPO_DEBUG", "🔄 UID contains only whitespace: ${profile.uid.isBlank()}")
        Log.d("REPO_DEBUG", "📁 Document path: ${USERS_COLLECTION}/${profile.uid}")
        val updatedProfile = profile.copy(lastUpdated = System.currentTimeMillis())
        
        try {
            val documentRef = firestore.collection(USERS_COLLECTION).document(profile.uid)
            Log.d("REPO_DEBUG", "📄 Document reference: ${documentRef.path}")
            
            documentRef.set(updatedProfile).await()
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "❌ Error creating document reference: ${e.message}")
            Log.e("REPO_DEBUG", "❌ UID value: '${profile.uid}'")
            Log.e("REPO_DEBUG", "❌ USERS_COLLECTION: '$USERS_COLLECTION'")
            throw e
        }
            
        Log.d("REPO_DEBUG", "✅ Profile updated successfully for UID: ${profile.uid}")
        updatedProfile
    }

    override suspend fun createUserProfile(profile: UserProfile): Result<UserProfile> = runCatching {
        // Validate UID before proceeding
        if (profile.uid.isBlank()) {
            Log.e("REPO_DEBUG", "❌ Invalid UID: UID is blank or empty")
            throw IllegalArgumentException("User ID cannot be blank")
        }
        
        Log.d("REPO_DEBUG", "🔄 Creating profile for UID: '${profile.uid}'")
        Log.d("REPO_DEBUG", "🔄 UID length: ${profile.uid.length}")
        Log.d("REPO_DEBUG", "🔄 UID contains only whitespace: ${profile.uid.isBlank()}")
        Log.d("REPO_DEBUG", "📁 Document path: ${USERS_COLLECTION}/${profile.uid}")
        
        try {
            val documentRef = firestore.collection(USERS_COLLECTION).document(profile.uid)
            Log.d("REPO_DEBUG", "📄 Document reference: ${documentRef.path}")
            
            documentRef.set(profile).await()
        } catch (e: Exception) {
            Log.e("REPO_DEBUG", "❌ Error creating document reference: ${e.message}")
            Log.e("REPO_DEBUG", "❌ UID value: '${profile.uid}'")
            Log.e("REPO_DEBUG", "❌ USERS_COLLECTION: '$USERS_COLLECTION'")
            throw e
        }
            
        Log.d("REPO_DEBUG", "✅ Profile created successfully for UID: ${profile.uid}")
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
            // Get user's reels with detailed filtering debug
            val snapshot = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("userId", uid)
                .whereEqualTo("type", "REEL")
                .whereEqualTo("isPublished", true)
                .get()
                .await()

            Log.d(
                "REPO_DEBUG",
                "📄 Query completed. Found ${snapshot.documents.size} documents matching userId=$uid, type=REEL, isPublished=true"
            )

            // Debug each document in detail
            snapshot.documents.forEachIndexed { index, doc ->
                Log.d("REPO_DEBUG", "📄 Document $index: ID=${doc.id}")
                Log.d(
                    "REPO_DEBUG",
                    "📄 Document $index: userId='${doc.getString("userId")}' (expected: '$uid')"
                )
                Log.d("REPO_DEBUG", "📄 Document $index: type='${doc.getString("type")}'")
                Log.d(
                    "REPO_DEBUG",
                    "📄 Document $index: isPublished=${doc.getBoolean("isPublished")}"
                )
                Log.d("REPO_DEBUG", "📄 Document $index: title='${doc.getString("title")}'")
                Log.d("REPO_DEBUG", "📄 Document $index: mediaUrl='${doc.getString("mediaUrl")}'")

                // Check if userId matches exactly
                val docUserId = doc.getString("userId")
                if (docUserId != uid) {
                    Log.e(
                        "REPO_DEBUG",
                        "❌ Document $index has WRONG userId! Expected '$uid', got '$docUserId'"
                    )
                } else {
                    Log.d("REPO_DEBUG", "✅ Document $index has correct userId")
                }
            }
            
            // Try to parse each document
            val reels = mutableListOf<UserPost>()
            snapshot.documents.forEachIndexed { index, doc ->
                try {
                    val userPost = doc.toObject(UserPost::class.java)
                    if (userPost != null) {
                        Log.d(
                            "REPO_DEBUG",
                            "✅ Successfully parsed document $index -> UserPost: title='${userPost.title}', userId='${userPost.userId}'"
                        )

                        // Double-check userId after parsing
                        if (userPost.userId != uid) {
                            Log.e(
                                "REPO_DEBUG",
                                "❌ AFTER PARSING: UserPost has wrong userId! Expected '$uid', got '${userPost.userId}'"
                            )
                        } else {
                            Log.d("REPO_DEBUG", "✅ AFTER PARSING: UserPost has correct userId")
                        }

                        reels.add(userPost)
                    } else {
                        Log.e("REPO_DEBUG", "❌ Failed to parse document $index to UserPost")
                    }
                } catch (e: Exception) {
                    Log.e("REPO_DEBUG", "❌ Error parsing document $index: ${e.message}")
                    e.printStackTrace()
                }
            }

            Log.d("REPO_DEBUG", "✅ Final result: Parsed ${reels.size} reels for user $uid")
            Log.d(
                "REPO_DEBUG",
                "🔍 Returning reels with IDs: ${reels.map { "${it.id}:${it.userId}" }}"
            )

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
            .set(hashMapOf("followedAt" to com.google.firebase.Timestamp.now()))
            .await()
        
        // Add to followers collection
        firestore.collection(USERS_COLLECTION)
            .document(targetUserId)
            .collection(FOLLOWERS_COLLECTION)
            .document(currentUserId)
            .set(hashMapOf("followedAt" to com.google.firebase.Timestamp.now()))
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
