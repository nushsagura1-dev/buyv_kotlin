package com.project.e_commerce.domain.model

/**
 * Supported application locales (SET-001).
 *
 * @param code  BCP-47 language tag passed to AppCompatDelegate.setApplicationLocales() (Android)
 *              and Bundle.setLanguage() (iOS).
 * @param label Human-readable name shown in the language picker.
 * @param rtl   True for right-to-left languages.
 */
enum class AppLocale(val code: String, val label: String, val rtl: Boolean) {
    ARABIC("ar", "العربية", rtl = true),
    ENGLISH("en", "English", rtl = false),
    FRENCH("fr", "Français", rtl = false);

    companion object {
        fun fromCode(code: String): AppLocale =
            entries.firstOrNull { it.code == code } ?: ENGLISH

        val default: AppLocale = ENGLISH
    }
}
