package com.project.e_commerce.domain.usecase.auth

import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * UseCase tests for auth domain: Login, Register, Logout, GetCurrentUser,
 * GoogleSignIn, SendPasswordReset.
 *
 * Uses hand-written FakeAuthRepository — no mocking framework needed.
 */

// ── Fake Repository ────────────────────────────────────────
private class FakeAuthRepository(
    var signInResult: Result<UserProfile> = Result.Success(UserProfile(uid = "1")),
    var signUpResult: Result<UserProfile> = Result.Success(UserProfile(uid = "1")),
    var signOutResult: Result<Unit> = Result.Success(Unit),
    var currentUserResult: Result<UserProfile?> = Result.Success(null),
    var isSignedIn: Boolean = false,
    var googleSignInResult: Result<UserProfile> = Result.Success(UserProfile(uid = "g1")),
    var resetResult: Result<Unit> = Result.Success(Unit),
    var confirmResetResult: Result<Unit> = Result.Success(Unit),
    var updateProfileResult: Result<Unit> = Result.Success(Unit),
    var deleteResult: Result<Unit> = Result.Success(Unit),
) : AuthRepository {
    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastDisplayName: String? = null
    var signOutCalled = false
    var lastIdToken: String? = null

    override suspend fun signInWithEmail(email: String, password: String): Result<UserProfile> {
        lastEmail = email; lastPassword = password
        return signInResult
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<UserProfile> {
        lastEmail = email; lastPassword = password; lastDisplayName = displayName
        return signUpResult
    }

    override suspend fun signInWithGoogle(idToken: String): Result<UserProfile> {
        lastIdToken = idToken
        return googleSignInResult
    }

    override suspend fun signOut(): Result<Unit> {
        signOutCalled = true
        return signOutResult
    }

    override suspend fun getCurrentUser(): Result<UserProfile?> = currentUserResult
    override suspend fun isUserSignedIn(): Boolean = isSignedIn
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        lastEmail = email; return resetResult
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String): Result<Unit> = confirmResetResult

    override suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> = updateProfileResult
    override suspend fun deleteAccount(): Result<Unit> = deleteResult
}


// ════════════════════════════════════════════════
// LOGIN USE CASE
// ════════════════════════════════════════════════

class LoginUseCaseTest {

    @Test
    fun login_success_returns_profile() = runTest {
        val profile = UserProfile(uid = "u1", email = "test@test.com", displayName = "Test")
        val repo = FakeAuthRepository(signInResult = Result.Success(profile))
        val result = LoginUseCase(repo)("test@test.com", "password123")

        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("u1", result.data.uid)
        assertEquals("test@test.com", result.data.email)
    }

    @Test
    fun login_delegates_credentials_to_repository() = runTest {
        val repo = FakeAuthRepository()
        LoginUseCase(repo)("alice@example.com", "secret")

        assertEquals("alice@example.com", repo.lastEmail)
        assertEquals("secret", repo.lastPassword)
    }

    @Test
    fun login_unauthorized_propagates_error() = runTest {
        val repo = FakeAuthRepository(signInResult = Result.Error(ApiError.Unauthorized))
        val result = LoginUseCase(repo)("test@test.com", "wrong")

        assertIs<Result.Error>(result)
        assertIs<ApiError.Unauthorized>(result.error)
    }

    @Test
    fun login_network_error_propagates() = runTest {
        val repo = FakeAuthRepository(signInResult = Result.Error(ApiError.NetworkError))
        val result = LoginUseCase(repo)("test@test.com", "pass")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NetworkError>(result.error)
    }
}


// ════════════════════════════════════════════════
// REGISTER USE CASE
// ════════════════════════════════════════════════

class RegisterUseCaseTest {

    @Test
    fun register_success() = runTest {
        val profile = UserProfile(uid = "new1", email = "new@test.com", displayName = "New User")
        val repo = FakeAuthRepository(signUpResult = Result.Success(profile))
        val result = RegisterUseCase(repo)("new@test.com", "ValidPass123!", "New User")

        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("new1", result.data.uid)
    }

    @Test
    fun register_delegates_all_fields() = runTest {
        val repo = FakeAuthRepository()
        RegisterUseCase(repo)("bob@test.com", "MyPass99", "Bob")

        assertEquals("bob@test.com", repo.lastEmail)
        assertEquals("MyPass99", repo.lastPassword)
        assertEquals("Bob", repo.lastDisplayName)
    }

    @Test
    fun register_duplicate_email_returns_validation_error() = runTest {
        val repo = FakeAuthRepository(
            signUpResult = Result.Error(ApiError.ValidationError("Email already registered"))
        )
        val result = RegisterUseCase(repo)("dup@test.com", "Pass1234", "Dup")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ValidationError>(result.error)
    }

    @Test
    fun register_server_error_propagates() = runTest {
        val repo = FakeAuthRepository(signUpResult = Result.Error(ApiError.ServerError))
        val result = RegisterUseCase(repo)("x@x.com", "pass", "X")

        assertIs<Result.Error>(result)
        assertIs<ApiError.ServerError>(result.error)
    }
}


// ════════════════════════════════════════════════
// LOGOUT USE CASE
// ════════════════════════════════════════════════

class LogoutUseCaseTest {

    @Test
    fun logout_success() = runTest {
        val repo = FakeAuthRepository()
        val result = LogoutUseCase(repo)()

        assertIs<Result.Success<Unit>>(result)
        assertTrue(repo.signOutCalled)
    }

    @Test
    fun logout_error_propagates() = runTest {
        val repo = FakeAuthRepository(signOutResult = Result.Error(ApiError.ServerError))
        val result = LogoutUseCase(repo)()

        assertIs<Result.Error>(result)
    }
}


// ════════════════════════════════════════════════
// GET CURRENT USER USE CASE
// ════════════════════════════════════════════════

class GetCurrentUserUseCaseTest {

    @Test
    fun returns_user_when_signed_in() = runTest {
        val profile = UserProfile(uid = "u5", email = "me@test.com")
        val repo = FakeAuthRepository(
            currentUserResult = Result.Success(profile),
            isSignedIn = true,
        )
        val result = GetCurrentUserUseCase(repo)()

        assertIs<Result.Success<UserProfile?>>(result)
        assertEquals("u5", result.data?.uid)
    }

    @Test
    fun returns_null_when_not_signed_in() = runTest {
        val repo = FakeAuthRepository(
            currentUserResult = Result.Success(null),
            isSignedIn = false,
        )
        val result = GetCurrentUserUseCase(repo)()
        assertIs<Result.Success<UserProfile?>>(result)
        assertEquals(null, result.data)
    }

    @Test
    fun isUserSignedIn_delegates_to_repo() = runTest {
        val repo = FakeAuthRepository(isSignedIn = true)
        assertTrue(GetCurrentUserUseCase(repo).isUserSignedIn())

        repo.isSignedIn = false
        assertFalse(GetCurrentUserUseCase(repo).isUserSignedIn())
    }
}


// ════════════════════════════════════════════════
// GOOGLE SIGN IN USE CASE
// ════════════════════════════════════════════════

class GoogleSignInUseCaseTest {

    @Test
    fun google_sign_in_success() = runTest {
        val profile = UserProfile(uid = "g1", email = "google@test.com")
        val repo = FakeAuthRepository(googleSignInResult = Result.Success(profile))
        val result = GoogleSignInUseCase(repo)("google-id-token-123")

        assertIs<Result.Success<UserProfile>>(result)
        assertEquals("g1", result.data.uid)
        assertEquals("google-id-token-123", repo.lastIdToken)
    }

    @Test
    fun google_sign_in_error() = runTest {
        val repo = FakeAuthRepository(
            googleSignInResult = Result.Error(ApiError.Unauthorized)
        )
        val result = GoogleSignInUseCase(repo)("bad-token")
        assertIs<Result.Error>(result)
    }
}


// ════════════════════════════════════════════════
// SEND PASSWORD RESET USE CASE
// ════════════════════════════════════════════════

class SendPasswordResetUseCaseTest {

    @Test
    fun send_reset_success() = runTest {
        val repo = FakeAuthRepository()
        val result = SendPasswordResetUseCase(repo)("reset@test.com")

        assertIs<Result.Success<Unit>>(result)
        assertEquals("reset@test.com", repo.lastEmail)
    }

    @Test
    fun send_reset_error() = runTest {
        val repo = FakeAuthRepository(resetResult = Result.Error(ApiError.NotFound))
        val result = SendPasswordResetUseCase(repo)("unknown@test.com")

        assertIs<Result.Error>(result)
        assertIs<ApiError.NotFound>(result.error)
    }
}
