package com.project.e_commerce.android.data.repository


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
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

            // 👇 احفظ "username فقط" في Firestore: users/<uid>
            if (!displayName.isNullOrBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(user.uid) // docId = uid
                    .set(mapOf("username" to displayName))
                    .await()
            }

            user
        }.mapError()

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            auth.sendPasswordResetEmail(email).await()
            Unit
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
