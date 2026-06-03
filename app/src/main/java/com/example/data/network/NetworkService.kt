package com.example.data.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkService {
    private const val TAG = "NetworkService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun generateWebpageContent(
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelName: String,
        systemGuidance: String,
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            when {
                provider.equals("Gemini", ignoreCase = true) -> {
                    callGeminiApi(apiKey, baseUrl, modelName, systemGuidance, prompt)
                }
                else -> {
                    // OpenAI-compatible (TheOpenCode GO, OpenRouter, Custom)
                    callOpenAiCompatibleApi(provider, apiKey, baseUrl, modelName, systemGuidance, prompt)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating page", e)
            Result.failure(e)
        }
    }

    private fun callGeminiApi(
        apiKey: String,
        customBaseUrl: String,
        model: String,
        systemInstruction: String,
        prompt: String
    ): Result<String> {
        val rootUrl = if (customBaseUrl.isNotBlank()) customBaseUrl else "https://generativelanguage.googleapis.com"
        // Ensure clean endpoint construction
        val endpoint = "${rootUrl.trimEnd('/')}/v1beta/models/$model:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            // contents array
            val contentsArray = JSONArray().apply {
                val turn = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "System Instructions:\n$systemInstruction\n\nPrompt:\n$prompt")
                        })
                    })
                }
                put(turn)
            }
            put("contents", contentsArray)

            // systemInstruction (supported in v1beta)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemInstruction)
                    })
                })
            })

            // generationConfig
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Gemini API error status ${response.code}: $errBody")
                return Result.failure(IOException("Gemini API Error: ${response.code} $errBody"))
            }

            val bodyString = response.body?.string() ?: return Result.failure(IOException("Empty response from Gemini"))
            try {
                val responseObj = JSONObject(bodyString)
                val candidates = responseObj.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        var text = parts.getJSONObject(0).getString("text")
                        // Clean up markdown code wrapping if present
                        text = cleanMarkdown(text)
                        return Result.success(text)
                    }
                }
                return Result.failure(IOException("Could not scan text parts in Gemini response body"))
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error: ${e.message}", e)
                return Result.failure(IOException("Gemini response parsing error: ${e.message}"))
            }
        }
    }

    private fun callOpenAiCompatibleApi(
        provider: String,
        apiKey: String,
        customBaseUrl: String,
        model: String,
        systemInstruction: String,
        prompt: String
    ): Result<String> {
        val rootUrl = when {
            customBaseUrl.isNotBlank() -> customBaseUrl.trimEnd('/')
            provider.equals("TheOpenCode GO", ignoreCase = true) -> "https://api.thebopencode.com/v1"
            provider.equals("OpenRouter", ignoreCase = true) -> "https://openrouter.ai/api/v1"
            else -> "https://api.openai.com/v1"
        }
        val endpoint = "$rootUrl/chat/completions"

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
        }

        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")

        if (provider.equals("OpenRouter", ignoreCase = true)) {
            // OpenRouter extra headers
            requestBuilder.addHeader("HTTP-Referer", "https://ai.studio")
            requestBuilder.addHeader("X-Title", "Webpage Creator App")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "$provider API error status ${response.code}: $errBody")
                return Result.failure(IOException("$provider API Error: ${response.code} $errBody"))
            }

            val bodyString = response.body?.string() ?: return Result.failure(IOException("Empty response from $provider"))
            try {
                val responseObj = JSONObject(bodyString)
                val choices = responseObj.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val message = firstChoice.getJSONObject("message")
                    var text = message.getString("content")
                    text = cleanMarkdown(text)
                    return Result.success(text)
                }
                return Result.failure(IOException("Could not parse choices in $provider response"))
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error: ${e.message}", e)
                return Result.failure(IOException("Response parsing error: ${e.message}"))
            }
        }
    }

    private fun cleanMarkdown(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```html", ignoreCase = true)) {
            clean = clean.substring("```html".length)
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length - 3)
            }
        } else if (clean.startsWith("```", ignoreCase = true)) {
            clean = clean.substring("```".length)
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length - 3)
            }
        }
        return clean.trim()
    }
}
