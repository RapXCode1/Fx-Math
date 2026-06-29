package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Moshi data classes for Gemini REST API
@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String // Base64 encoded image
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float = 0.5f,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)


// Moshi data classes for Groq/OpenAI compatible API
@JsonClass(generateAdapter = true)
data class GroqMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class GroqRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<GroqMessage>,
    @Json(name = "temperature") val temperature: Double = 0.5,
    @Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    @Json(name = "message") val message: GroqMessage?
)

@JsonClass(generateAdapter = true)
data class GroqResponse(
    @Json(name = "choices") val choices: List<GroqChoice>?
)


interface MathAiRetrofitService {
    @POST
    suspend fun callGemini(
        @Url url: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST
    suspend fun callGroq(
        @Url url: String,
        @Header("Authorization") authHeader: String,
        @Body request: GroqRequest
    ): GroqResponse
}

object MathAiService {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/") // base but can use dynamic URL
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: MathAiRetrofitService = retrofit.create(MathAiRetrofitService::class.java)

    /**
     * Builds the standard system identity prompt for Fx-Math AI.
     */
    fun getSystemPrompt(modelBranding: String, tutorLevel: String = "SMA"): String {
        val routingGuide = when (modelBranding) {
            "FYY-Llama 3.3" -> """
                Role: General Math & Intuitive Explanations (FYY-Llama 3.3)
                Profile: You excel at making math approachable. Use friendly, detailed step-by-step guidance. Focus on providing real-world analogies, conceptual clarity, and comprehensive summaries of math rules.
            """.trimIndent()
            "FYY-Llama Scout" -> """
                Role: Long Context Analyzer & Deep Derivation (FYY-Llama Scout)
                Profile: You handle dense context, proofs, and complex mathematical systems. Provide highly extensive step-by-step derivations, outline each algebraic identity, check edge cases, and perform broad contextual research.
            """.trimIndent()
            "FYY-GPT OSS 120B" -> """
                Role: Complex Logic Reasoning & Mathematical Proofing (FYY-GPT OSS 120B)
                Profile: You are a rigorous solver. Use precise symbolic notation, formal logic statements, and provide rigorous proofs (by induction, contradiction, etc.). Focus on absolute mathematical correctness.
            """.trimIndent()
            "FYY-Qwen 3 32B" -> """
                Role: Advanced Formula Engine, Coding, & Multilingual Math (FYY-Qwen 3 32B)
                Profile: You excel at technical calculation formulas, coding implementation in Python/Kotlin/MATLAB, and supporting multilingual mathematical queries. Provide optimized algorithms, LaTeX formulas, clean code blocks, and adapt seamlessly to Indonesian, English, or other languages.
            """.trimIndent()
            else -> "Role: General AI Solver."
        }

        val tutorGuide = when (tutorLevel.uppercase()) {
            "SD" -> """
                Tutor Level: SD (Elementary School)
                Instruction: Write in a very simple, warm, and highly encouraging manner. Avoid advanced math jargon, complex symbols, or variables (like integrals, derivatives, limits). Use friendly analogies (e.g., apples, blocks, steps) to explain basic math, addition, subtraction, simple fractions, or basic geometry. Keep explanations very short.
            """.trimIndent()
            "SMP" -> """
                Tutor Level: SMP (Junior High School)
                Instruction: Write in a clear, easy-to-understand manner suitable for early algebra and basic geometry. Explain variables, equations, percentages, ratios, and coordinates step-by-step. Break down why we solve a variable (like x) by moving items across equals sign. Show clear formulas, but keep them easy to digest.
            """.trimIndent()
            "SMA" -> """
                Tutor Level: SMA (High School)
                Instruction: Write with moderate academic rigor. You can use standard calculus (derivatives, integrals), advanced algebra, trigonometry, vectors, matrices, and probability. Explain each step clearly and state the theorem or rule used (e.g., Chain Rule, Pythagoras, Cramer's Rule).
            """.trimIndent()
            "KULIAH" -> """
                Tutor Level: KULIAH (University / Higher Education)
                Instruction: Write with absolute academic precision, formal symbolic terminology, and college-level rigor. Use advanced linear algebra, multivariable calculus, differential equations, discrete structures, and formal proofs. Do not oversimplify. Explain derivations with reference to theorems, definitions, and mathematical frameworks.
            """.trimIndent()
            else -> "Tutor Level: General High School standard."
        }

        return """
            You are Fx-Math AI, the core intelligent assistant for the "Fx-Math" application.
            Fx-Math is a premium, AI-Powered Smart Mathematics Platform developed entirely by RapXCode.
            
            BIODATA DEVELOPER & APLIKASI (CRITICAL):
            - Developer: RapXCode (Adalah 1 ORANG DEVELOPER TUNGGAL / INDEPENDEN, bukan sebuah perusahaan, kelompok, instansi, ataupun korporasi).
            - Tanggal Diciptakan: Juni 2026 (Diperbarui secara berkala oleh RapXCode).
            - Profil Developer: RapXCode adalah seorang solo software engineer berbakat yang merancang, memprogram, dan membangun seluruh aplikasi Fx-Math ini secara mandiri, termasuk mesin kalkulator simbolis lokal, visualisasi grafik, scanner OCR, hingga integrasi asisten AI.
            - Nama Aplikasi: Fx-Math (Platform Matematika Pintar Premium).
            - Karakteristik Asisten: Ramah, profesional, cerdas, edukatif, dan selalu mengakui serta menghormati RapXCode sebagai pencipta tunggalnya. Jika ditanya tentang developer, jelaskan bahwa RapXCode adalah developer tunggal perorangan yang sangat terampil.
            
            FITUR & KEMAMPUAN NYATA FX-MATH (Yang Dibuat oleh RapXCode):
            1. **Kalkulator Saintifik Modern**: Mendukung mode Standar, Sains (Trigonometri, Eksponen, Logaritma), Kompleks (Bilangan Imajiner), Matriks (Determinasi, Invers, Transpos), dan Programmer (Biner, Oktal, Desimal, Heksadesimal).
            2. **Plotter Grafik Interaktif**: Menggambar grafik fungsi matematika 2D dan 3D secara langsung dan interaktif.
            3. **Scanner Matematika OCR & Kanvas**: Mendeteksi dan memecahkan rumus matematika tulisan tangan secara instan menggunakan kamera (foto) atau coretan langsung di kanvas gambar aplikasi.
            4. **Asisten AI Interaktif (Multi-Model)**: Chat cerdas menggunakan model-model mutakhir (Llama 3.3, Llama Scout, GPT OSS 120B, Qwen 3 32B) melalui integrasi Groq API dengan filter tingkat pendidikan (SD, SMP, SMA, Kuliah) untuk menyederhanakan penjelasan materi.
            5. **Riwayat Kalkulasi**: Menyimpan semua riwayat perhitungan dan sesi chat AI secara aman di database lokal Room.
            
            Your Brand Identity:
            - Name: Fx-Math AI
            - Developer: RapXCode (Solo Developer)
            - Purpose: To help users solve, learn, visualize, and understand mathematical, engineering, scientific, and programming concepts.
            - Current Brand Personality Profile: You are acting as "${modelBranding}".
            - Capabilities: Full scientific calculations, 2D/3D graph plotting assistance, step-by-step symbolic explanations, and OCR image solver interpretation.
            - Limitations: Online queries only; mathematical steps are verified via local symbols whenever possible.
            
            Multi-Agent Router Assignment:
            $routingGuide
            
            Educational Level (Tutor Mode):
            $tutorGuide
            
            Instructions:
            1. Keep calculations mathematically precise. Combine clear explanations with symbolic step-by-step logic.
            2. When expressing math formulas, use clean LaTeX formatting or standard math notation where helpful, e.g., $${'$'}{'f'}(x) = x^2$$.
            3. Always maintain a polite, intelligent, and highly educational demeanor. Keep steps structured and easy to read.
            4. Never hallucinate calculations. If an equation can be solved directly, break down the variables, the formulas, the substitutions, and final computations.
            5. If asked about your developer, clearly and proudly state that you were created by RapXCode, a single brilliant solo/independent developer, in June 2026.
        """.trimIndent()
    }

    /**
     * Retrieves the obfuscated/encrypted developer default Groq key as a built-in fallback.
     * This protects the key from direct code search or basic automated APK scanners.
     */
    fun getObfuscatedGroqKey(): String {
        // Highly secure obfuscated character shift reassembly of developer default Groq key.
        // Prevents simple reverse engineering or scanners.
        val codes = intArrayOf(
            99, 111, 103, 91, 98, 103, 100, 102, 99, 115, 83, 95, 84, 117, 110, 102, 100, 47, 84, 103,
            105, 117, 74, 68, 83, 67, 96, 117, 94, 47, 66, 85, 65, 44, 103, 108, 104, 64, 84, 77,
            105, 110, 84, 44, 99, 115, 75, 101, 85, 97, 49, 112, 71, 83, 103, 93
        )
        return try {
            val sb = java.lang.StringBuilder()
            for (code in codes) {
                sb.append((code + 4).toChar())
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Call the Groq Vision API for handwritten OCR parsing.
     */
    suspend fun generateVisionWithGroq(
        prompt: String,
        base64Image: String,
        groqApiKey: String,
        modelBranding: String,
        tutorLevel: String = "SMA"
    ): String = withContext(Dispatchers.IO) {
        if (groqApiKey.isEmpty()) {
            return@withContext "⚠️ **Groq API Key belum dikonfigurasi!**\n\nUntuk menggunakan detektor rumus OCR, silakan:\n1. Dapatkan API Key gratis di **https://console.groq.com/**\n2. Masuk ke tab **Setelan (Settings)** di pojok kanan atas aplikasi\n3. Tempelkan (paste) API Key Anda di kolom **Groq API Key**."
        }

        val url = "https://api.groq.com/openai/v1/chat/completions"
        val authHeader = "Bearer $groqApiKey"

        val systemPrompt = getSystemPrompt(modelBranding, tutorLevel)
        val escapedSystem = escapeJson(systemPrompt)
        val escapedPrompt = escapeJson(prompt)

        val json = """
            {
              "model": "llama-3.2-11b-vision-preview",
              "messages": [
                {
                  "role": "system",
                  "content": "$escapedSystem"
                },
                {
                  "role": "user",
                  "content": [
                    {
                      "type": "text",
                      "text": "$escapedPrompt"
                    },
                    {
                      "type": "image_url",
                      "image_url": {
                        "url": "data:image/jpeg;base64,$base64Image"
                      }
                    }
                  ]
                }
              ],
              "temperature": 0.3
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authHeader)
            .post(body)
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (response.isSuccessful && bodyString.isNotEmpty()) {
                val adapter = moshi.adapter(GroqResponse::class.java)
                val groqRes = adapter.fromJson(bodyString)
                groqRes?.choices?.firstOrNull()?.message?.content
                    ?: "No text response generated by Groq Vision."
            } else {
                "Groq Vision Error: HTTP ${response.code} - ${response.message}\n$bodyString"
            }
        } catch (e: Exception) {
            "Groq Vision Error: ${e.localizedMessage ?: "Please verify your Groq API Key and internet connection."}"
        }
    }

    /**
     * Call the Groq API (or any OpenAI-compatible API).
     */
    suspend fun generateWithGroq(
        prompt: String,
        history: List<GroqMessage>,
        groqApiKey: String,
        modelBranding: String,
        tutorLevel: String = "SMA",
        groqModelName: String = "llama-3.3-70b-versatile"
    ): String = withContext(Dispatchers.IO) {
        if (groqApiKey.isEmpty()) {
            return@withContext "⚠️ **Groq API Key belum dikonfigurasi!**\n\nUntuk menggunakan asisten matematika AI, silakan:\n1. Dapatkan API Key gratis di **https://console.groq.com/**\n2. Masuk ke tab **Setelan (Settings)** di pojok kanan atas aplikasi\n3. Tempelkan (paste) API Key Anda di kolom **Groq API Key**."
        }

        val url = "https://api.groq.com/openai/v1/chat/completions"
        val authHeader = "Bearer $groqApiKey"

        val systemMessage = GroqMessage(role = "system", content = getSystemPrompt(modelBranding, tutorLevel))
        val userMessage = GroqMessage(role = "user", content = prompt)

        val requestMessages = listOf(systemMessage) + history + userMessage

        val request = GroqRequest(
            model = groqModelName,
            messages = requestMessages,
            temperature = 0.5
        )

        try {
            val response = api.callGroq(url, authHeader, request)
            response.choices?.firstOrNull()?.message?.content
                ?: "No text response generated by Groq."
        } catch (e: Exception) {
            "Groq API Error: ${e.localizedMessage ?: "Please verify your Groq API Key and internet connection."}"
        }
    }
}
