package com.project.e_commerce.android.data.repository

import android.util.Log
import com.google.android.gms.common.util.CollectionUtils.mapOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val cred = auth.signInWithEmailAndPassword(email, password).await()
        cred.user ?: throw FirebaseAuthException("no-user", "User not found")
    }.mapError()

    override suspend fun signUp(email: String, password: String, displayName: String?): Result<FirebaseUser> =
        runCatching {
            val cred = auth.createUserWithEmailAndPassword(email, password).await()
            val user = cred.user ?: throw FirebaseAuthException("no-user", "User not found")

            // (اختياري) حدّث الاسم في Profile بتاع Auth
            if (!displayName.isNullOrBlank()) {
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profile).await()
            }

            // 👇 احفظ UserProfile كامل في Firestore: users/<uid>
            if (!displayName.isNullOrBlank()) {
                val db = FirebaseFirestore.getInstance()
                val userProfile = hashMapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to displayName,
                    "username" to displayName,
                    "bio" to "",
                    "phone" to "",
                    "profileImageUrl" to null,
                    "followersCount" to 0L,
                    "followingCount" to 0L,
                    "likesCount" to 0L,
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "lastUpdated" to com.google.firebase.Timestamp.now()
                )
                db.collection("users")
                    .document(user.uid) // docId = uid
                    .set(userProfile)
                    .await()
            }

            user
        }.mapError()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email).await()
            Unit
        }.mapError()

    override suspend fun changePassword(newPassword: String): Result<Unit> =
        runCatching {
            val user =
                auth.currentUser ?: throw FirebaseAuthException("no-user", "User not authenticated")
            user.updatePassword(newPassword).await()
            Unit
        }.recover { exception ->
            if (exception is FirebaseAuthException && exception.errorCode == "ERROR_REQUIRES_RECENT_LOGIN") {
                throw FirebaseAuthException(
                    "requires-recent-login",
                    "لتغيير كلمة المرور، يجب إعادة تسجيل الدخول أولاً"
                )
            }
            throw exception
        }.mapError()

    override suspend fun changePasswordWithReauth(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> =
        runCatching {
            val user =
                auth.currentUser ?: throw FirebaseAuthException("no-user", "User not authenticated")
            val email = user.email ?: throw FirebaseAuthException("no-email", "No email found")

            // Re-authenticate with current password
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()

            // Now change the password
            user.updatePassword(newPassword).await()
            Unit
        }.mapError()

    override suspend fun signOut(): Result<Unit> =
        runCatching {
            auth.signOut()
            Unit
        }.mapError()

    // Google Sign-In integration
    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        Log.d("FirebaseAuth", "Starting Google Sign-In with idToken")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val cred = auth.signInWithCredential(credential).await()
        val user = cred.user ?: throw FirebaseAuthException(
            "no-user",
            "User not found after Google Sign-In"
        )

        Log.d("FirebaseAuth", "Google Sign-In successful for user: ${user.email}")

        // احفظ UserProfile كامل في Firestore لو مش موجود
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(user.uid).get().await()
        if (!userDoc.exists()) {
            Log.d("FirebaseAuth", "Creating new user profile in Firestore")
            val userProfile = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: ""),
                "username" to (user.displayName ?: ""),
                "bio" to "",
                "phone" to (user.phoneNumber ?: ""),
                "profileImageUrl" to user.photoUrl?.toString(),
                "followersCount" to 0L,
                "followingCount" to 0L,
                "likesCount" to 0L,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "lastUpdated" to com.google.firebase.Timestamp.now()
            )
            db.collection("users")
                .document(user.uid)
                .set(userProfile)
                .await()
            Log.d("FirebaseAuth", "User profile created successfully")
        } else {
            Log.d("FirebaseAuth", "User profile already exists")
        }
        user
    }.recover { exception ->
        Log.e("FirebaseAuth", "Google Sign-In failed", exception)
        when (exception) {
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                        throw Exception("حساب بهذا البريد موجود بطريقة تسجيل دخول أخرى")

                    "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
                        throw Exception("هذا الحساب مستخدم بالفعل")

                    else -> throw Exception("فشل تسجيل الدخول بجوجل: ${exception.message}")
                }
            }

            else -> throw Exception("حدث خطأ أثناء تسجيل الدخول بجوجل")
        }
    }.mapError()

    override fun currentUser(): FirebaseUser? = auth.currentUser
}

private fun <T> Result<T>.mapError(): Result<T> = this.fold(
    onSuccess = { Result.success(it) },
    onFailure = { e ->
        val msg = when (e) {
            is FirebaseAuthException -> when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "البريد غير صالح"
                "ERROR_USER_NOT_FOUND" -> "الحساب غير موجود"
                "ERROR_WRONG_PASSWORD" -> "كلمة المرور غير صحيحة"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "الإيميل مستخدم بالفعل"
                "ERROR_WEAK_PASSWORD" -> "كلمة المرور ضعيفة"
                else -> e.localizedMessage ?: "حدث خطأ غير متوقع"
            }
            else -> e.localizedMessage ?: "حدث خطأ غير متوقع"
        }
        Result.failure(Exception(msg))
    }
)
