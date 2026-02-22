package com.project.e_commerce.data.validator

import com.project.e_commerce.domain.model.ValidationResult

/**
 * Validateur d'inputs utilisateur conforme OWASP.
 * 
 * Sanitise et valide tous les inputs utilisateur avant traitement.
 * IMPORTANT: La validation backend doit AUSSI être effectuée (ne jamais faire confiance au client).
 */
object InputValidator {
    
    // Regex de validation
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_]{3,50}$")
    private val PHONE_REGEX = Regex("^\\+?[0-9]{10,15}$")
    private val ALPHANUMERIC_REGEX = Regex("^[a-zA-Z0-9\\s]+$")
    
    // Caractères dangereux à supprimer (XSS prevention)
    private val DANGEROUS_CHARS = Regex("[<>\"'`;&|]")
    
    /**
     * Sanitise une chaîne en supprimant les caractères dangereux.
     * 
     * @param input Chaîne à sanitiser
     * @param maxLength Longueur maximum autorisée
     * @return Chaîne sanitisée
     */
    fun sanitizeString(input: String, maxLength: Int = 255): String {
        return input
            .trim()
            .take(maxLength)
            .replace(DANGEROUS_CHARS, "")
    }
    
    /**
     * Valide un email.
     * 
     * @param email Email à valider
     * @return ValidationResult.Success si valide, sinon ValidationResult.Error
     */
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult.Error("Email is required")
        }
        
        val sanitized = sanitizeString(email, 320)
        
        return if (EMAIL_REGEX.matches(sanitized)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("Invalid email format")
        }
    }
    
    /**
     * Valide un nom d'utilisateur.
     * 
     * Règles:
     * - 3-50 caractères
     * - Alphanumerique + underscore uniquement
     * 
     * @param username Nom d'utilisateur à valider
     * @return ValidationResult
     */
    fun validateUsername(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Error("Username is required")
            username.length < 3 -> ValidationResult.Error("Username must be at least 3 characters")
            username.length > 50 -> ValidationResult.Error("Username must be less than 50 characters")
            !USERNAME_REGEX.matches(username) -> ValidationResult.Error("Username can only contain letters, numbers and underscores")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide un mot de passe.
     * 
     * Règles:
     * - Minimum 8 caractères
     * - Au moins une majuscule
     * - Au moins une minuscule
     * - Au moins un chiffre
     * 
     * @param password Mot de passe à valider
     * @return ValidationResult
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Password is required")
            password.length < 8 -> ValidationResult.Error("Password must be at least 8 characters")
            !password.any { it.isUpperCase() } -> ValidationResult.Error("Password must contain at least one uppercase letter")
            !password.any { it.isLowerCase() } -> ValidationResult.Error("Password must contain at least one lowercase letter")
            !password.any { it.isDigit() } -> ValidationResult.Error("Password must contain at least one number")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide un numéro de téléphone.
     * 
     * @param phone Numéro de téléphone à valider
     * @return ValidationResult
     */
    fun validatePhone(phone: String): ValidationResult {
        if (phone.isBlank()) {
            return ValidationResult.Error("Phone number is required")
        }
        
        val sanitized = phone.replace(Regex("[\\s-()]"), "")
        
        return if (PHONE_REGEX.matches(sanitized)) {
            ValidationResult.Success
        } else {
            ValidationResult.Error("Invalid phone number format")
        }
    }
    
    /**
     * Valide une bio utilisateur.
     * 
     * Règles:
     * - Maximum 500 caractères
     * - Pas de caractères dangereux
     * 
     * @param bio Bio à valider
     * @return ValidationResult
     */
    fun validateBio(bio: String): ValidationResult {
        return when {
            bio.length > 500 -> ValidationResult.Error("Bio must be less than 500 characters")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide un prix de produit.
     * 
     * @param price Prix à valider
     * @return ValidationResult
     */
    fun validatePrice(price: String): ValidationResult {
        return when {
            price.isBlank() -> ValidationResult.Error("Price is required")
            price.toDoubleOrNull() == null -> ValidationResult.Error("Price must be a valid number")
            price.toDouble() < 0 -> ValidationResult.Error("Price cannot be negative")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide une quantité de produit.
     * 
     * @param quantity Quantité à valider
     * @return ValidationResult
     */
    fun validateQuantity(quantity: String): ValidationResult {
        return when {
            quantity.isBlank() -> ValidationResult.Error("Quantity is required")
            quantity.toIntOrNull() == null -> ValidationResult.Error("Quantity must be a valid number")
            quantity.toInt() < 0 -> ValidationResult.Error("Quantity cannot be negative")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide un nom de produit.
     * 
     * @param name Nom à valider
     * @return ValidationResult
     */
    fun validateProductName(name: String): ValidationResult {
        val sanitized = sanitizeString(name, 200)
        
        return when {
            sanitized.isBlank() -> ValidationResult.Error("Product name is required")
            sanitized.length < 3 -> ValidationResult.Error("Product name must be at least 3 characters")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Valide une description de produit.
     * 
     * @param description Description à valider
     * @return ValidationResult
     */
    fun validateDescription(description: String): ValidationResult {
        val sanitized = sanitizeString(description, 2000)
        
        return when {
            sanitized.isBlank() -> ValidationResult.Error("Description is required")
            sanitized.length < 10 -> ValidationResult.Error("Description must be at least 10 characters")
            else -> ValidationResult.Success
        }
    }
}
