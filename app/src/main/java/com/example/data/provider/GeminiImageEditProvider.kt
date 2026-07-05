package com.example.data.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.domain.model.EditRequest
import com.example.domain.model.EditResponse
import com.example.domain.model.GenerationInfo
import com.example.domain.provider.ImageEditProvider
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Models (Moshi-compatible) ---

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "imageConfig") val imageConfig: GeminiImageConfig? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "topP") val topP: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String = "1K"
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash-image:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}

class GeminiImageEditProvider(private val apiKey: String) : ImageEditProvider {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    override suspend fun editImage(request: EditRequest): EditResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext EditResponse.Error(
                "Gemini API Key is not configured. Please enter your key in the AI Studio Secrets panel."
            )
        }

        try {
            val startTime = System.currentTimeMillis()

            // 1. Convert source image to base64
            val originalBase64 = request.originalImage.toBase64()

            // 2. Build parts
            val parts = mutableListOf<GeminiPart>()
            
            // Build a descriptive instruction based on the tool
            val instructionText = when (request.tool) {
                com.example.domain.model.EditTool.PROMPT -> request.prompt
                com.example.domain.model.EditTool.MASK -> {
                    "Inpaint the selected masked area based on this instruction: '${request.prompt}'."
                }
                com.example.domain.model.EditTool.REMOVE -> "Remove the object or areas as requested: ${request.prompt}"
                com.example.domain.model.EditTool.REPLACE -> "Replace the background or elements: ${request.prompt}"
                com.example.domain.model.EditTool.EXPAND -> "Outpaint and expand this image naturally, following instruction: ${request.prompt}"
                com.example.domain.model.EditTool.ENHANCE -> "Enhance the details of this image, make it cinematic and professional. ${request.prompt}"
                com.example.domain.model.EditTool.BACKGROUND -> "Modify or replace the background: ${request.prompt}"
                com.example.domain.model.EditTool.STYLE -> "Transform this image into the style of: ${request.prompt}"
                com.example.domain.model.EditTool.ADJUST -> "Adjust color levels, brightness: ${request.prompt}"
            }

            parts.add(GeminiPart(text = instructionText))
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = originalBase64)))

            // If a mask exists, we can overlay or attach it as well
            request.maskImage?.let { mask ->
                val maskBase64 = mask.toBase64()
                parts.add(GeminiPart(text = "This is the brush edit mask overlay in white pixels on black background."))
                parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = maskBase64)))
            }

            // 3. Build request
            val ratioString = when (request.settings.aspectRatio) {
                com.example.domain.model.AspectRatio.SQUARE -> "1:1"
                com.example.domain.model.AspectRatio.PORTRAIT -> "3:4"
                com.example.domain.model.AspectRatio.LANDSCAPE -> "4:3"
                com.example.domain.model.AspectRatio.TALL -> "9:16"
                com.example.domain.model.AspectRatio.WIDE -> "16:9"
            }

            val systemPrompt = "You are a professional image editing AI model. Modify the input image as instructed. Output ONLY the resulting modified image."

            val geminiRequest = GeminiGenerateRequest(
                contents = listOf(GeminiContent(parts = parts)),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("IMAGE"),
                    imageConfig = GeminiImageConfig(aspectRatio = ratioString),
                    temperature = request.settings.creativity
                ),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
            )

            // 4. Execute call
            val response = api.generateContent(apiKey, geminiRequest)

            // 5. Parse response
            val candidate = response.candidates?.firstOrNull()
            val responseParts = candidate?.content?.parts
            
            // Search for image data returned from model
            val imagePart = responseParts?.firstOrNull { it.inlineData?.mimeType?.startsWith("image/") == true }
            
            if (imagePart != null && imagePart.inlineData != null) {
                val decodedBytes = Base64.decode(imagePart.inlineData.data, Base64.DEFAULT)
                val generatedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                
                if (generatedBitmap != null) {
                    val duration = System.currentTimeMillis() - startTime
                    val finalSeed = request.settings.seed ?: (100000L..999999L).random()
                    return@withContext EditResponse.Success(
                        generatedImage = generatedBitmap,
                        info = GenerationInfo(
                            model = request.settings.model,
                            resolution = "${generatedBitmap.width}x${generatedBitmap.height}",
                            durationMs = duration,
                            seed = finalSeed
                        )
                    )
                }
            }

            // Check if there was text returned instead of an image (e.g., error explanation)
            val textPart = responseParts?.firstOrNull { !it.text.isNullOrBlank() }
            if (textPart != null) {
                return@withContext EditResponse.Error("API Response Error: ${textPart.text}")
            }

            return@withContext EditResponse.Error("The API returned an empty or invalid image output.")

        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: ""
            return@withContext EditResponse.Error("API Request Failed (${e.code()}): $errorBody")
        } catch (e: Exception) {
            return@withContext EditResponse.Error("Network Error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
