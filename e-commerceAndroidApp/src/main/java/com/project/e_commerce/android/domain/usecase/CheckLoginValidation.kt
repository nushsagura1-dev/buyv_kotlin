package com.project.e_commerce.android.domain.usecase

class CheckLoginValidation {

    operator fun invoke(email: String?, password: String?): Pair<Boolean, String> {
        if (email == null || password == null) {
            return Pair(false, "Email and password must not be null")
        }
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@(.+)$")

        if (!emailPattern.matches(email)) {
            return Pair(false, "Invalid email format")
        }

        if (password.isEmpty()) {
            return Pair(false, "Password cannot be empty")
        }

        // Removed all password complexity requirements
        // Password can now be only numbers or any combination

        return Pair(true, "Login credentials are valid")
    }
}
