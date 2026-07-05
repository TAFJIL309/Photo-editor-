package com.example.domain.model

import android.graphics.Bitmap

enum class EditTool(val displayName: String) {
    PROMPT("Prompt"),
    MASK("Mask"),
    REMOVE("Remove"),
    REPLACE("Replace"),
    EXPAND("Expand"),
    ENHANCE("Enhance"),
    BACKGROUND("Background"),
    STYLE("Style"),
    ADJUST("Adjust")
}

enum class ProviderType {
    GEMINI,
    MOCK
}

enum class AspectRatio(val ratio: String, val ratioValue: Float) {
    SQUARE("1:1", 1.0f),
    PORTRAIT("3:4", 0.75f),
    LANDSCAPE("4:3", 1.33f),
    TALL("9:16", 0.5625f),
    WIDE("16:9", 1.777f)
}

data class GeneratorSettings(
    val creativity: Float = 0.5f,
    val imageStrength: Float = 0.5f,
    val promptStrength: Float = 0.5f,
    val negativePrompt: String = "",
    val seed: Long? = null,
    val isRandomSeed: Boolean = true,
    val aspectRatio: AspectRatio = AspectRatio.SQUARE,
    val model: String = "gemini-2.5-flash-image",
    val provider: ProviderType = ProviderType.MOCK
)

data class ImageAdjustment(
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val temperature: Float = 0.0f,
    val sharpness: Float = 0.0f
)

data class EditRequest(
    val originalImage: Bitmap,
    val prompt: String,
    val tool: EditTool,
    val maskImage: Bitmap? = null,
    val settings: GeneratorSettings,
    val adjustment: ImageAdjustment = ImageAdjustment()
)

sealed class EditResponse {
    data class Success(val generatedImage: Bitmap, val info: GenerationInfo) : EditResponse()
    data class Error(val message: String) : EditResponse()
}

data class GenerationInfo(
    val model: String,
    val resolution: String,
    val durationMs: Long,
    val seed: Long
)
