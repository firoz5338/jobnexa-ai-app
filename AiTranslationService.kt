package com.example.domain

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class TranslationResult {
    data class Success(
        val originalText: String,
        val translatedText: String,
        val targetLanguage: String,
        val isCached: Boolean = false
    ) : TranslationResult()

    data class Error(
        val originalText: String,
        val errorMessage: String,
        val targetLanguage: String
    ) : TranslationResult()
}

class AiTranslationService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    // In-memory cache to avoid unnecessary repeat translation API calls
    // Key format: "${jobId}_${targetLanguage.lowercase()}"
    private val translationCache = ConcurrentHashMap<String, String>()

    suspend fun translateJobDescription(
        jobId: String,
        rawText: String,
        targetLanguage: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        val trimmedText = rawText.trim()
        val trimmedTargetLang = targetLanguage.trim()

        if (trimmedText.isBlank()) {
            return@withContext TranslationResult.Error(
                originalText = rawText,
                errorMessage = "The job description is empty and cannot be translated.",
                targetLanguage = trimmedTargetLang
            )
        }

        // If target language is already English, no translation needed
        if (trimmedTargetLang.equals("English", ignoreCase = true)) {
            return@withContext TranslationResult.Success(
                originalText = rawText,
                translatedText = rawText,
                targetLanguage = "English",
                isCached = true
            )
        }

        // Check in-memory cache
        val cacheKey = "${jobId}_${trimmedTargetLang.lowercase()}"
        val cached = translationCache[cacheKey]
        if (!cached.isNullOrBlank()) {
            return@withContext TranslationResult.Success(
                originalText = rawText,
                translatedText = cached,
                targetLanguage = trimmedTargetLang,
                isCached = true
            )
        }

        // Retrieve Gemini API key securely from BuildConfig via reflection
        val geminiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String)?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }

        if (geminiKey.isBlank() || geminiKey.contains("MY_GEMINI_API_KEY")) {
            return@withContext TranslationResult.Error(
                originalText = rawText,
                errorMessage = "AI translation service credentials (GEMINI_API_KEY) are not configured. Showing employer's original English description.",
                targetLanguage = trimmedTargetLang
            )
        }

        try {
            val prompt = """
                You are a professional certified recruitment translator.
                Translate the following employment vacancy description completely and accurately into $trimmedTargetLang.
                
                STRICT TRANSLATION RULES:
                1. Translate all explanatory and descriptive sentences faithfully into $trimmedTargetLang.
                2. Preserve all factual job details, salary figures, numbers, dates, locations, company names, job titles, certifications, technical terms, and visa conditions.
                3. Do not invent missing job details or alter employment conditions.
                4. Output ONLY the raw translated description text in $trimmedTargetLang. Do not add markdown backticks, conversational preamble, notes, or explanations.
                
                JOB VACANCY TEXT:
                $trimmedText
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                val contents = org.json.JSONArray().apply {
                    val part = JSONObject().apply { put("text", prompt) }
                    val parts = org.json.JSONArray().apply { put(part) }
                    val content = JSONObject().apply { put("parts", parts) }
                    put(content)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates")
                val candidate = candidates?.optJSONObject(0)
                val translatedText = candidate?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim() ?: ""

                if (translatedText.isNotBlank()) {
                    translationCache[cacheKey] = translatedText
                    return@withContext TranslationResult.Success(
                        originalText = rawText,
                        translatedText = translatedText,
                        targetLanguage = trimmedTargetLang,
                        isCached = false
                    )
                }
            }

            TranslationResult.Error(
                originalText = rawText,
                errorMessage = "Translation failed (HTTP ${response.code}). Showing employer's original description.",
                targetLanguage = trimmedTargetLang
            )
        } catch (e: Exception) {
            TranslationResult.Error(
                originalText = rawText,
                errorMessage = "Network or service error during translation: ${e.localizedMessage ?: "Unknown error"}. Showing employer's original description.",
                targetLanguage = trimmedTargetLang
            )
        }
    }

    fun getCachedTranslation(jobId: String, targetLanguage: String): String? {
        val cacheKey = "${jobId}_${targetLanguage.trim().lowercase()}"
        return translationCache[cacheKey]
    }

    fun clearCache() {
        translationCache.clear()
    }

    fun putInCache(jobId: String, targetLanguage: String, translatedText: String) {
        val cacheKey = "${jobId}_${targetLanguage.trim().lowercase()}"
        translationCache[cacheKey] = translatedText
    }
}
