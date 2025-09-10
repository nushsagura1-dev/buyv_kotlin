package com.project.e_commerce.android.domain.usecase

class CheckPasswordValidation {

    operator fun invoke(password: String): Pair<Boolean, String> {
        // Allow any password - no restrictions
        // Can be only numbers, letters, or any combination
        return when {
            password.isEmpty() -> Pair(false, "كلمة المرور لا يمكن أن تكون فارغة")
            else -> Pair(true, "كلمة المرور صالحة")
        }
    }
}