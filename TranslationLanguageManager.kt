package com.example.domain

import android.content.Context
import android.os.Build
import java.util.Locale

object TranslationLanguageManager {
    private const val PREFS_NAME = "jobnexa_translation_prefs"
    private const val KEY_TARGET_LANGUAGE = "target_translation_language"

    // Curated popular target languages for fast 1-tap selection in the job translation control
    val POPULAR_LANGUAGES = listOf(
        "Spanish",
        "Arabic",
        "Hindi",
        "Urdu",
        "French",
        "German",
        "Tagalog",
        "Bengali",
        "Indonesian",
        "Portuguese",
        "Turkish",
        "Polish",
        "Russian",
        "Italian",
        "Japanese",
        "Korean",
        "Chinese",
        "Vietnamese",
        "Thai",
        "Dutch",
        "Greek",
        "Romanian",
        "English"
    )

    /**
     * Detects the Android device's preferred system language.
     * Returns the English name of the language (e.g., "Spanish", "Arabic", "Hindi", "German").
     * If detection is unavailable or returns blank, defaults to "English".
     */
    fun detectDeviceLanguage(context: Context): String {
        return try {
            val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val locales = context.resources.configuration.locales
                if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }

            val languageName = locale.getDisplayLanguage(Locale.ENGLISH)
            if (languageName.isNotBlank()) languageName else "English"
        } catch (_: Exception) {
            "English"
        }
    }

    /**
     * Retrieves the persisted target language or detects it from device on first launch.
     */
    fun getSavedOrDetectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_TARGET_LANGUAGE, null)
        if (!saved.isNullOrBlank()) {
            return saved
        }

        val detected = detectDeviceLanguage(context)
        prefs.edit().putString(KEY_TARGET_LANGUAGE, detected).apply()
        return detected
    }

    /**
     * Persists the user's chosen target translation language override.
     */
    fun saveTargetLanguage(context: Context, language: String) {
        val cleanLang = language.trim()
        if (cleanLang.isNotBlank()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TARGET_LANGUAGE, cleanLang).apply()
        }
    }
}
