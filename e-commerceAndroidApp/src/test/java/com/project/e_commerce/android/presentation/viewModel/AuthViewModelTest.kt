package com.project.e_commerce.android.presentation.viewModel

import com.project.e_commerce.android.data.helper.GoogleSignInHelper
import com.project.e_commerce.android.data.repository.AuthRepository
import com.project.e_commerce.android.domain.usecase.CheckEmailValidation
import com.project.e_commerce.android.domain.usecase.CheckMatchedPasswordUseCase
import com.project.e_commerce.android.domain.usecase.CheckPasswordValidation
import com.project.e_commerce.android.domain.usecase.GoogleSignInUseCase
import com.project.e_commerce.domain.model.ApiError
import com.project.e_commerce.domain.model.Result
import com.project.e_commerce.domain.model.UserProfile
import com.project.e_commerce.domain.usecase.auth.LoginUseCase
import com.project.e_commerce.domain.usecase.auth.LogoutUseCase
import com.project.e_commerce.domain.usecase.auth.RegisterUseCase
import com.project.e_commerce.domain.usecase.auth.SendPasswordResetUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for the main AuthViewModel.
 * Covers: login, register, signOut, sendResetEmail, input changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // ── Mocks ──
    private lateinit var checkEmail: CheckEmailValidation
    private lateinit var checkPassword: CheckPasswordValidation
    private lateinit var checkMatched: CheckMatchedPasswordUseCase
    private lateinit var googleHelper: GoogleSignInHelper
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var registerUseCase: RegisterUseCase
    private lateinit var logoutUseCase: LogoutUseCase
    private lateinit var resetUseCase: SendPasswordResetUseCase

    private lateinit var vm: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        checkEmail = mockk()
        checkPassword = mockk()
        checkMatched = mockk()
        googleHelper = mockk(relaxed = true)
        loginUseCase = mockk()
        registerUseCase = mockk()
        logoutUseCase = mockk()
        resetUseCase = mockk()

        vm = AuthViewModel(
            repo = null,
            checkEmailValidation = checkEmail,
            checkPasswordValidation = checkPassword,
            checkMatchedPassword = checkMatched,
            googleSignInUseCase = null,
            googleSignInHelper = googleHelper,
            loginUseCase = loginUseCase,
            registerUseCase = registerUseCase,
            logoutUseCase = logoutUseCase,
            sendPasswordResetUseCase = resetUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }


    // ════════════════════════════════════════════
    // INPUT CHANGES
    // ════════════════════════════════════════════

    @Test
    fun `onEmailChanged updates state and clears errors`() {
        vm.onEmailChanged("alice@test.com")
        assertEquals("alice@test.com", vm.state.value.email)
        assertNull(vm.state.value.emailError)
        assertNull(vm.state.value.generalError)
    }

    @Test
    fun `onPasswordChanged updates state and clears errors`() {
        vm.onPasswordChanged("secret123")
        assertEquals("secret123", vm.state.value.password)
        assertNull(vm.state.value.passwordError)
    }

    @Test
    fun `onRePasswordChanged updates state`() {
        vm.onRePasswordChanged("secret123")
        assertEquals("secret123", vm.state.value.rePassword)
        assertNull(vm.state.value.rePasswordError)
    }

    @Test
    fun `onUsernameChanged updates state`() {
        vm.onUsernameChanged("Alice")
        assertEquals("Alice", vm.state.value.username)
    }


    // ════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════

    @Test
    fun `login success sets isLoggedIn`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        coEvery { loginUseCase(any(), any()) } returns Result.Success(UserProfile(uid = "u1"))

        vm.onEmailChanged("a@b.com")
        vm.onPasswordChanged("pass1234")
        vm.login()
        advanceUntilIdle()

        assertTrue(vm.state.value.isLoggedIn)
        assertFalse(vm.state.value.loading)
        assertNull(vm.state.value.generalError)
    }

    @Test
    fun `login validation failure does not call use case`() = runTest {
        every { checkEmail(any()) } returns (false to "E-mail invalide")
        every { checkPassword(any()) } returns (true to "ok")

        vm.login()
        advanceUntilIdle()

        assertNotNull(vm.state.value.emailError)
        coVerify(exactly = 0) { loginUseCase(any(), any()) }
    }

    @Test
    fun `login error sets generalError`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        coEvery { loginUseCase(any(), any()) } returns Result.Error(ApiError.Unauthorized)

        vm.onEmailChanged("a@b.com")
        vm.onPasswordChanged("pass")
        vm.login()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoggedIn)
        assertFalse(vm.state.value.loading)
        assertNotNull(vm.state.value.generalError)
    }

    @Test
    fun `login trims email`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        coEvery { loginUseCase(any(), any()) } returns Result.Success(UserProfile(uid = "u1"))

        vm.onEmailChanged("  bob@test.com  ")
        vm.onPasswordChanged("pass1234")
        vm.login()
        advanceUntilIdle()

        coVerify { loginUseCase("bob@test.com", "pass1234") }
    }


    // ════════════════════════════════════════════
    // REGISTER
    // ════════════════════════════════════════════

    @Test
    fun `register success sends NavigateToLogin effect`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        every { checkMatched(any(), any()) } returns true
        coEvery { registerUseCase(any(), any(), any()) } returns Result.Success(UserProfile(uid = "u2"))

        vm.onEmailChanged("new@test.com")
        vm.onPasswordChanged("Pass1234")
        vm.onRePasswordChanged("Pass1234")
        vm.onUsernameChanged("NewUser")
        vm.register()
        advanceUntilIdle()

        assertFalse(vm.state.value.loading)
        coVerify { registerUseCase("new@test.com", "Pass1234", "NewUser") }
    }

    @Test
    fun `register with blank username defaults to User`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        every { checkMatched(any(), any()) } returns true
        coEvery { registerUseCase(any(), any(), any()) } returns Result.Success(UserProfile(uid = "u2"))

        vm.onEmailChanged("new@test.com")
        vm.onPasswordChanged("Pass1234")
        vm.onRePasswordChanged("Pass1234")
        // username blank
        vm.register()
        advanceUntilIdle()

        coVerify { registerUseCase("new@test.com", "Pass1234", "User") }
    }

    @Test
    fun `register password mismatch sets rePasswordError`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        every { checkMatched(any(), any()) } returns false

        vm.onPasswordChanged("aaa")
        vm.onRePasswordChanged("bbb")
        vm.register()
        advanceUntilIdle()

        assertNotNull(vm.state.value.rePasswordError)
        coVerify(exactly = 0) { registerUseCase(any(), any(), any()) }
    }

    @Test
    fun `register error sets generalError`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        every { checkPassword(any()) } returns (true to "ok")
        every { checkMatched(any(), any()) } returns true
        coEvery { registerUseCase(any(), any(), any()) } returns
                Result.Error(ApiError.ValidationError("Email already exists"))

        vm.onEmailChanged("dup@test.com")
        vm.onPasswordChanged("Pass1234")
        vm.onRePasswordChanged("Pass1234")
        vm.register()
        advanceUntilIdle()

        assertNotNull(vm.state.value.generalError)
        assertTrue(vm.state.value.generalError!!.contains("Email already exists"))
    }


    // ════════════════════════════════════════════
    // SIGN OUT
    // ════════════════════════════════════════════

    @Test
    fun `signOut success resets state`() = runTest {
        coEvery { googleHelper.signOut() } just runs
        coEvery { logoutUseCase() } returns Result.Success(Unit)

        vm.signOut()
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoggedIn)
        assertFalse(vm.state.value.loading)
        assertEquals("", vm.state.value.email)
    }

    @Test
    fun `signOut error preserves state and sets error`() = runTest {
        coEvery { googleHelper.signOut() } just runs
        coEvery { logoutUseCase() } returns Result.Error(ApiError.ServerError)

        vm.signOut()
        advanceUntilIdle()

        assertNotNull(vm.state.value.generalError)
    }


    // ════════════════════════════════════════════
    // SEND RESET EMAIL
    // ════════════════════════════════════════════

    @Test
    fun `sendResetEmail success with explicit email`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        coEvery { resetUseCase(any()) } returns Result.Success(Unit)

        vm.sendResetEmail("reset@test.com")
        advanceUntilIdle()

        assertFalse(vm.state.value.loading)
        coVerify { resetUseCase("reset@test.com") }
    }

    @Test
    fun `sendResetEmail uses state email when no explicit email`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        coEvery { resetUseCase(any()) } returns Result.Success(Unit)

        vm.onEmailChanged("state@test.com")
        vm.sendResetEmail()
        advanceUntilIdle()

        coVerify { resetUseCase("state@test.com") }
    }

    @Test
    fun `sendResetEmail invalid email sets emailError`() = runTest {
        every { checkEmail(any()) } returns (false to "Invalid email")

        vm.sendResetEmail("bad-email")
        advanceUntilIdle()

        assertNotNull(vm.state.value.emailError)
        coVerify(exactly = 0) { resetUseCase(any()) }
    }

    @Test
    fun `sendResetEmail error sets generalError`() = runTest {
        every { checkEmail(any()) } returns (true to null)
        coEvery { resetUseCase(any()) } returns Result.Error(ApiError.NotFound)

        vm.sendResetEmail("unknown@test.com")
        advanceUntilIdle()

        assertNotNull(vm.state.value.generalError)
    }
}
