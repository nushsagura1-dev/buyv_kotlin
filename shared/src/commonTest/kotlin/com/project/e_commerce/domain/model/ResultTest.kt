package com.project.e_commerce.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ResultTest {

    // ========== Result.Success ==========

    @Test
    fun success_isSuccess_true() {
        val result: Result<String> = Result.Success("data")
        assertTrue(result.isSuccess)
    }

    @Test
    fun success_isError_false() {
        val result: Result<String> = Result.Success("data")
        assertFalse(result.isError)
    }

    @Test
    fun success_isLoading_false() {
        val result: Result<String> = Result.Success("data")
        assertFalse(result.isLoading)
    }

    @Test
    fun success_getOrNull_returns_data() {
        val result: Result<String> = Result.Success("hello")
        assertEquals("hello", result.getOrNull())
    }

    @Test
    fun success_getOrThrow_returns_data() {
        val result: Result<Int> = Result.Success(42)
        assertEquals(42, result.getOrThrow())
    }

    // ========== Result.Error ==========

    @Test
    fun error_isSuccess_false() {
        val result: Result<String> = Result.Error(ApiError.NetworkError)
        assertFalse(result.isSuccess)
    }

    @Test
    fun error_isError_true() {
        val result: Result<String> = Result.Error(ApiError.NetworkError)
        assertTrue(result.isError)
    }

    @Test
    fun error_getOrNull_returns_null() {
        val result: Result<String> = Result.Error(ApiError.NotFound)
        assertNull(result.getOrNull())
    }

    @Test
    fun error_getOrThrow_throws() {
        val result: Result<String> = Result.Error(ApiError.Unauthorized)
        assertFailsWith<Exception> {
            result.getOrThrow()
        }
    }

    @Test
    fun error_getOrThrow_message_from_apiError() {
        val result: Result<String> = Result.Error(ApiError.Unauthorized)
        val ex = assertFailsWith<Exception> {
            result.getOrThrow()
        }
        assertEquals("Unauthorized access", ex.message)
    }

    // ========== Result.Loading ==========

    @Test
    fun loading_isLoading_true() {
        val result: Result<String> = Result.Loading
        assertTrue(result.isLoading)
    }

    @Test
    fun loading_isSuccess_false() {
        val result: Result<String> = Result.Loading
        assertFalse(result.isSuccess)
    }

    @Test
    fun loading_getOrNull_returns_null() {
        val result: Result<String> = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun loading_getOrThrow_throws_illegalState() {
        val result: Result<String> = Result.Loading
        assertFailsWith<IllegalStateException> {
            result.getOrThrow()
        }
    }

    // ========== ApiError types ==========

    @Test
    fun networkError_message() {
        assertEquals("Network connection error", ApiError.NetworkError.message)
    }

    @Test
    fun unauthorized_message() {
        assertEquals("Unauthorized access", ApiError.Unauthorized.message)
    }

    @Test
    fun forbidden_message() {
        assertEquals("Access forbidden", ApiError.Forbidden.message)
    }

    @Test
    fun notFound_message() {
        assertEquals("Resource not found", ApiError.NotFound.message)
    }

    @Test
    fun serverError_message() {
        assertEquals("Server error occurred", ApiError.ServerError.message)
    }

    @Test
    fun validationError_custom_message() {
        val error = ApiError.ValidationError("Field is invalid")
        assertEquals("Field is invalid", error.message)
    }

    @Test
    fun unknown_default_message() {
        val error = ApiError.Unknown()
        assertEquals("An error occurred", error.message)
    }

    @Test
    fun unknown_custom_message() {
        val error = ApiError.Unknown("Custom error")
        assertEquals("Custom error", error.message)
    }

    // ========== ApiError.fromException ==========

    @Test
    fun fromException_network_error() {
        val error = ApiError.fromException(Exception("network timeout"))
        assertIs<ApiError.NetworkError>(error)
    }

    @Test
    fun fromException_401_unauthorized() {
        val error = ApiError.fromException(Exception("HTTP 401 error"))
        assertIs<ApiError.Unauthorized>(error)
    }

    @Test
    fun fromException_404_notFound() {
        val error = ApiError.fromException(Exception("HTTP 404 not found"))
        assertIs<ApiError.NotFound>(error)
    }

    @Test
    fun fromException_500_serverError() {
        val error = ApiError.fromException(Exception("HTTP 500 internal server error"))
        assertIs<ApiError.ServerError>(error)
    }

    @Test
    fun fromException_unknown_with_message() {
        val error = ApiError.fromException(Exception("Something weird"))
        assertIs<ApiError.Unknown>(error)
        assertEquals("Something weird", error.message)
    }

    @Test
    fun fromException_null_message() {
        val error = ApiError.fromException(Exception())
        assertIs<ApiError.Unknown>(error)
        assertEquals("Unknown error", error.message)
    }

    // ========== ValidationResult ==========

    @Test
    fun validationResult_success_isValid() {
        assertTrue(ValidationResult.Success.isValid)
    }

    @Test
    fun validationResult_error_not_valid() {
        assertFalse(ValidationResult.Error("oops").isValid)
    }

    @Test
    fun validationResult_success_errors_empty() {
        assertTrue(ValidationResult.Success.errors.isEmpty())
    }

    @Test
    fun validationResult_error_errors_contains_message() {
        val result = ValidationResult.Error("Required field")
        assertEquals(listOf("Required field"), result.errors)
    }
}
