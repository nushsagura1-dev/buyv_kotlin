package com.project.e_commerce.data.validator

import com.project.e_commerce.domain.model.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InputValidatorTest {

    // ========== sanitizeString ==========

    @Test
    fun sanitizeString_trims_whitespace() {
        assertEquals("hello", InputValidator.sanitizeString("  hello  "))
    }

    @Test
    fun sanitizeString_truncates_to_maxLength() {
        assertEquals("abc", InputValidator.sanitizeString("abcdef", maxLength = 3))
    }

    @Test
    fun sanitizeString_removes_dangerous_chars() {
        assertEquals("scriptalert(1)/script", InputValidator.sanitizeString("<script>alert(1)</script>"))
    }

    @Test
    fun sanitizeString_removes_sql_injection_chars() {
        assertEquals("1 OR 1=1--", InputValidator.sanitizeString("1; OR 1=1--'"))
    }

    @Test
    fun sanitizeString_preserves_safe_chars() {
        assertEquals("Hello World 123!@#\$%^()", InputValidator.sanitizeString("Hello World 123!@#\$%^()"))
    }

    @Test
    fun sanitizeString_empty_input_returns_empty() {
        assertEquals("", InputValidator.sanitizeString(""))
    }

    @Test
    fun sanitizeString_defaults_maxLength_255() {
        val longString = "a".repeat(300)
        assertEquals(255, InputValidator.sanitizeString(longString).length)
    }

    // ========== validateEmail ==========

    @Test
    fun validateEmail_valid_email_succeeds() {
        assertTrue(InputValidator.validateEmail("user@example.com").isValid)
    }

    @Test
    fun validateEmail_valid_with_plus() {
        assertTrue(InputValidator.validateEmail("user+tag@example.com").isValid)
    }

    @Test
    fun validateEmail_valid_with_dots() {
        assertTrue(InputValidator.validateEmail("first.last@example.co.uk").isValid)
    }

    @Test
    fun validateEmail_blank_returns_required_error() {
        val result = InputValidator.validateEmail("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Email is required", result.message)
    }

    @Test
    fun validateEmail_whitespace_only_returns_required_error() {
        val result = InputValidator.validateEmail("   ")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Email is required", result.message)
    }

    @Test
    fun validateEmail_no_at_sign_fails() {
        val result = InputValidator.validateEmail("userexample.com")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Invalid email format", result.message)
    }

    @Test
    fun validateEmail_no_domain_fails() {
        val result = InputValidator.validateEmail("user@")
        assertIs<ValidationResult.Error>(result)
    }

    @Test
    fun validateEmail_no_tld_fails() {
        val result = InputValidator.validateEmail("user@example")
        assertIs<ValidationResult.Error>(result)
    }

    @Test
    fun validateEmail_single_char_tld_fails() {
        val result = InputValidator.validateEmail("user@example.c")
        assertIs<ValidationResult.Error>(result)
    }

    // ========== validateUsername ==========

    @Test
    fun validateUsername_valid_alphanumeric() {
        assertTrue(InputValidator.validateUsername("john123").isValid)
    }

    @Test
    fun validateUsername_valid_with_underscore() {
        assertTrue(InputValidator.validateUsername("john_doe").isValid)
    }

    @Test
    fun validateUsername_min_3_chars() {
        assertTrue(InputValidator.validateUsername("abc").isValid)
    }

    @Test
    fun validateUsername_max_50_chars() {
        assertTrue(InputValidator.validateUsername("a".repeat(50)).isValid)
    }

    @Test
    fun validateUsername_blank_returns_required() {
        val result = InputValidator.validateUsername("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Username is required", result.message)
    }

    @Test
    fun validateUsername_too_short() {
        val result = InputValidator.validateUsername("ab")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Username must be at least 3 characters", result.message)
    }

    @Test
    fun validateUsername_too_long() {
        val result = InputValidator.validateUsername("a".repeat(51))
        assertIs<ValidationResult.Error>(result)
        assertEquals("Username must be less than 50 characters", result.message)
    }

    @Test
    fun validateUsername_special_chars_fail() {
        val result = InputValidator.validateUsername("john@doe")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Username can only contain letters, numbers and underscores", result.message)
    }

    @Test
    fun validateUsername_spaces_fail() {
        val result = InputValidator.validateUsername("john doe")
        assertIs<ValidationResult.Error>(result)
    }

    // ========== validatePassword ==========

    @Test
    fun validatePassword_valid_password() {
        assertTrue(InputValidator.validatePassword("Abcdef1g").isValid)
    }

    @Test
    fun validatePassword_blank_returns_required() {
        val result = InputValidator.validatePassword("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Password is required", result.message)
    }

    @Test
    fun validatePassword_too_short() {
        val result = InputValidator.validatePassword("Abc1xyz")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Password must be at least 8 characters", result.message)
    }

    @Test
    fun validatePassword_no_uppercase() {
        val result = InputValidator.validatePassword("abcdef1g")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Password must contain at least one uppercase letter", result.message)
    }

    @Test
    fun validatePassword_no_lowercase() {
        val result = InputValidator.validatePassword("ABCDEF1G")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Password must contain at least one lowercase letter", result.message)
    }

    @Test
    fun validatePassword_no_digit() {
        val result = InputValidator.validatePassword("Abcdefgh")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Password must contain at least one number", result.message)
    }

    // ========== validatePhone ==========

    @Test
    fun validatePhone_valid_10_digits() {
        assertTrue(InputValidator.validatePhone("0123456789").isValid)
    }

    @Test
    fun validatePhone_valid_with_plus_prefix() {
        assertTrue(InputValidator.validatePhone("+33612345678").isValid)
    }

    @Test
    fun validatePhone_valid_15_digits() {
        assertTrue(InputValidator.validatePhone("123456789012345").isValid)
    }

    @Test
    fun validatePhone_strips_spaces_dashes_parens() {
        assertTrue(InputValidator.validatePhone("+33 6 12-34-(56)78").isValid)
    }

    @Test
    fun validatePhone_blank_returns_required() {
        val result = InputValidator.validatePhone("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Phone number is required", result.message)
    }

    @Test
    fun validatePhone_too_short() {
        val result = InputValidator.validatePhone("12345")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Invalid phone number format", result.message)
    }

    @Test
    fun validatePhone_too_long() {
        val result = InputValidator.validatePhone("1234567890123456")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Invalid phone number format", result.message)
    }

    @Test
    fun validatePhone_letters_fail() {
        val result = InputValidator.validatePhone("012345abcd")
        assertIs<ValidationResult.Error>(result)
    }

    // ========== validateBio ==========

    @Test
    fun validateBio_empty_is_valid() {
        assertTrue(InputValidator.validateBio("").isValid)
    }

    @Test
    fun validateBio_within_limit_is_valid() {
        assertTrue(InputValidator.validateBio("This is a short bio.").isValid)
    }

    @Test
    fun validateBio_exactly_500_chars_is_valid() {
        assertTrue(InputValidator.validateBio("a".repeat(500)).isValid)
    }

    @Test
    fun validateBio_over_500_chars_fails() {
        val result = InputValidator.validateBio("a".repeat(501))
        assertIs<ValidationResult.Error>(result)
        assertEquals("Bio must be less than 500 characters", result.message)
    }

    // ========== validatePrice ==========

    @Test
    fun validatePrice_valid_integer() {
        assertTrue(InputValidator.validatePrice("100").isValid)
    }

    @Test
    fun validatePrice_valid_decimal() {
        assertTrue(InputValidator.validatePrice("19.99").isValid)
    }

    @Test
    fun validatePrice_zero_is_valid() {
        assertTrue(InputValidator.validatePrice("0").isValid)
    }

    @Test
    fun validatePrice_blank_returns_required() {
        val result = InputValidator.validatePrice("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Price is required", result.message)
    }

    @Test
    fun validatePrice_non_numeric_fails() {
        val result = InputValidator.validatePrice("abc")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Price must be a valid number", result.message)
    }

    @Test
    fun validatePrice_negative_fails() {
        val result = InputValidator.validatePrice("-5")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Price cannot be negative", result.message)
    }

    // ========== validateQuantity ==========

    @Test
    fun validateQuantity_valid_number() {
        assertTrue(InputValidator.validateQuantity("10").isValid)
    }

    @Test
    fun validateQuantity_zero_is_valid() {
        assertTrue(InputValidator.validateQuantity("0").isValid)
    }

    @Test
    fun validateQuantity_blank_returns_required() {
        val result = InputValidator.validateQuantity("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Quantity is required", result.message)
    }

    @Test
    fun validateQuantity_decimal_fails() {
        val result = InputValidator.validateQuantity("3.5")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Quantity must be a valid number", result.message)
    }

    @Test
    fun validateQuantity_negative_fails() {
        val result = InputValidator.validateQuantity("-1")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Quantity cannot be negative", result.message)
    }

    // ========== validateProductName ==========

    @Test
    fun validateProductName_valid_name() {
        assertTrue(InputValidator.validateProductName("Cool T-Shirt").isValid)
    }

    @Test
    fun validateProductName_min_3_chars() {
        assertTrue(InputValidator.validateProductName("abc").isValid)
    }

    @Test
    fun validateProductName_blank_returns_required() {
        val result = InputValidator.validateProductName("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Product name is required", result.message)
    }

    @Test
    fun validateProductName_too_short() {
        val result = InputValidator.validateProductName("ab")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Product name must be at least 3 characters", result.message)
    }

    @Test
    fun validateProductName_sanitizes_dangerous_chars() {
        // Name with only dangerous chars shorter than 3 becomes too short after sanitization
        val result = InputValidator.validateProductName("<>\"")
        assertIs<ValidationResult.Error>(result)
    }

    @Test
    fun validateProductName_truncates_to_200() {
        // Even a 300-char name is truncated to 200, but still valid (>3)
        assertTrue(InputValidator.validateProductName("a".repeat(300)).isValid)
    }

    // ========== validateDescription ==========

    @Test
    fun validateDescription_valid() {
        assertTrue(InputValidator.validateDescription("This is a valid product description.").isValid)
    }

    @Test
    fun validateDescription_min_10_chars() {
        assertTrue(InputValidator.validateDescription("1234567890").isValid)
    }

    @Test
    fun validateDescription_blank_returns_required() {
        val result = InputValidator.validateDescription("")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Description is required", result.message)
    }

    @Test
    fun validateDescription_too_short() {
        val result = InputValidator.validateDescription("Short")
        assertIs<ValidationResult.Error>(result)
        assertEquals("Description must be at least 10 characters", result.message)
    }

    @Test
    fun validateDescription_truncates_to_2000() {
        // Even a 3000-char desc is truncated to 2000, but still valid (>10)
        assertTrue(InputValidator.validateDescription("a".repeat(3000)).isValid)
    }
}
