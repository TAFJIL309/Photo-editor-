package com.example.data.provider

import android.graphics.*
import com.example.domain.model.EditRequest
import com.example.domain.model.EditResponse
import com.example.domain.model.EditTool
import com.example.domain.model.GenerationInfo
import com.example.domain.provider.ImageEditProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MockImageEditProvider : ImageEditProvider {

    override suspend fun editImage(request: EditRequest): EditResponse = withContext(Dispatchers.IO) {
        // Simulate remote latency
        delay(1800)

        val startTime = System.currentTimeMillis()
        val original = request.originalImage
        val width = original.width
        val height = original.height

        // Create a mutable copy of the original bitmap to apply operations
        val resultBitmap = Bitmap.createBitmap(width, height, original.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Draw original
        canvas.drawBitmap(original, 0f, 0f, paint)

        // Apply Image Adjustments first
        applyAdjustments(canvas, resultBitmap, request)

        // Handle specific AI tools and filters
        when (request.tool) {
            EditTool.STYLE -> {
                applyStyleFilter(canvas, resultBitmap, request.prompt)
            }
            EditTool.BACKGROUND -> {
                applyBackgroundReplacement(canvas, resultBitmap, request.prompt)
            }
            EditTool.REMOVE -> {
                applyObjectRemoval(canvas, resultBitmap, request.maskImage)
            }
            EditTool.REPLACE -> {
                applyObjectReplacement(canvas, resultBitmap, request.maskImage, request.prompt)
            }
            EditTool.MASK -> {
                applyMaskInpaint(canvas, resultBitmap, request.maskImage, request.prompt)
            }
            EditTool.EXPAND -> {
                // Outpainting effect: scale down a bit and fill background with gradient or blurred copy
                val scaled = Bitmap.createScaledBitmap(resultBitmap, (width * 0.85).toInt(), (height * 0.85).toInt(), true)
                canvas.drawColor(Color.parseColor("#15151A"))
                
                // Draw a beautiful ambient glow in the background
                val gradPaint = Paint().apply {
                    shader = RadialGradient(
                        width / 2f, height / 2f, width / 1.5f,
                        Color.parseColor("#251F35"), Color.parseColor("#0A0A0C"),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), gradPaint)
                canvas.drawBitmap(scaled, (width - scaled.width) / 2f, (height - scaled.height) / 2f, paint)
            }
            EditTool.ENHANCE -> {
                // Glow & detail enhancement
                applyEnhancement(canvas, resultBitmap)
            }
            EditTool.PROMPT -> {
                // AI edit based on text: blend a subtle colored ambient light to represent prompt influence
                applyPromptBlend(canvas, resultBitmap, request.prompt)
            }
            else -> {
                // No-op or minor tweak
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val seed = request.settings.seed ?: (100000L..999999L).random()

        return@withContext EditResponse.Success(
            generatedImage = resultBitmap,
            info = GenerationInfo(
                model = request.settings.model + " (Offline)",
                resolution = "${width}x${height}",
                durationMs = duration,
                seed = seed
            )
        )
    }

    private fun applyAdjustments(canvas: Canvas, bitmap: Bitmap, request: EditRequest) {
        val adj = request.adjustment
        val width = bitmap.width
        val height = bitmap.height

        val colorMatrix = ColorMatrix().apply {
            // Brightness, Contrast, Saturation
            val b = adj.brightness
            val c = adj.contrast
            val s = adj.saturation

            // Saturation matrix
            setSaturation(s)

            // Contrast & Brightness matrix
            val t = (1.0f - c) * 128f
            val postMatrix = ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, t + (b - 1f) * 255f,
                0f, c, 0f, 0f, t + (b - 1f) * 255f,
                0f, 0f, c, 0f, t + (b - 1f) * 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(postMatrix)

            // Temperature / Tint
            if (adj.temperature != 0.0f) {
                val rTemp = 1.0f + (adj.temperature * 0.15f)
                val bTemp = 1.0f - (adj.temperature * 0.15f)
                val tempMatrix = ColorMatrix(floatArrayOf(
                    rTemp, 0f, 0f, 0f, 0f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, bTemp, 0f, 0f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                postConcat(tempMatrix)
            }
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        // Draw onto the canvas using the color filter
        val tempBitmap = Bitmap.createBitmap(bitmap)
        canvas.drawBitmap(tempBitmap, 0f, 0f, paint)

        // Apply sharpness (basic high-pass edge highlight) if specified
        if (adj.sharpness > 0.05f) {
            applySharpness(canvas, bitmap, adj.sharpness)
        }
    }

    private fun applySharpness(canvas: Canvas, bitmap: Bitmap, amount: Float) {
        // Simple pixel overlay shift to simulate high-pass crispness
        val overlayPaint = Paint().apply {
            alpha = (amount * 80).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        }
        canvas.drawBitmap(bitmap, 1f, 1f, overlayPaint)
    }

    private fun applyStyleFilter(canvas: Canvas, bitmap: Bitmap, stylePrompt: String) {
        val lower = stylePrompt.lowercase()
        val width = bitmap.width
        val height = bitmap.height

        when {
            lower.contains("watercolor") || lower.contains("water") -> {
                // Watercolor styling: blur, high contrast, and warm sepia/vibrant paper blend
                val filterPaint = Paint().apply {
                    colorFilter = PorterDuffColorFilter(Color.parseColor("#15FFD2A0"), PorterDuff.Mode.MULTIPLY)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), filterPaint)
                
                // Add soft artistic vignette
                val grad = RadialGradient(
                    width / 2f, height / 2f, Math.max(width, height) / 1.4f,
                    Color.TRANSPARENT, Color.parseColor("#443E2B"), Shader.TileMode.CLAMP
                )
                val vigPaint = Paint().apply { shader = grad }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vigPaint)
            }
            lower.contains("sketch") || lower.contains("pencil") || lower.contains("drawing") -> {
                // Pencil sketch style: convert to grayscale and apply contour lines
                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                val grayPaint = Paint().apply { colorFilter = ColorMatrixColorFilter(grayMatrix) }
                
                val temp = Bitmap.createBitmap(bitmap)
                canvas.drawBitmap(temp, 0f, 0f, grayPaint)
                
                // Blend drawing outlines using inverse difference overlay
                val linePaint = Paint().apply {
                    alpha = 150
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
                }
                canvas.drawBitmap(temp, -1.5f, -1.5f, linePaint)
                
                // Invert filter back slightly to make it look hand-drawn white paper background
                canvas.drawColor(Color.parseColor("#10FFFFFF"), PorterDuff.Mode.SCREEN)
            }
            lower.contains("anime") || lower.contains("cartoon") -> {
                // Vibrant color-blocking anime look
                val animeMatrix = ColorMatrix().apply {
                    setSaturation(1.6f)
                }
                val animePaint = Paint().apply { colorFilter = ColorMatrixColorFilter(animeMatrix) }
                val temp = Bitmap.createBitmap(bitmap)
                canvas.drawBitmap(temp, 0f, 0f, animePaint)
                
                // Add cinematic purple/blue ambient gradient lighting
                val lighting = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#229B51E0"), Color.parseColor("#222F80ED"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = lighting })
            }
            lower.contains("oil painting") || lower.contains("painting") -> {
                // Smooth impasto style: heavy color blend and warm brush stroke look
                canvas.drawBitmap(bitmap, 2f, 2f, Paint().apply {
                    alpha = 120
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
                })
                canvas.drawColor(Color.parseColor("#089A4B1B"), PorterDuff.Mode.DARKEN)
            }
            lower.contains("cinematic") || lower.contains("hollywood") || lower.contains("night") -> {
                // Teal and Orange Hollywood color grade
                val matrix = ColorMatrix(floatArrayOf(
                    1.15f, 0f, 0f, 0f, 15f,  // More red highlights
                    0f, 0.95f, 0f, 0f, 0f,   // Standard green
                    0f, 0f, 1.25f, 0f, -10f, // Deep cool blues
                    0f, 0f, 0f, 1.0f, 0f
                ))
                val p = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
                val temp = Bitmap.createBitmap(bitmap)
                canvas.drawBitmap(temp, 0f, 0f, p)
            }
            else -> {
                // Default style: modern cyber-glow overlay
                val gradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#159B51E0"), Color.parseColor("#153B82F6"),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = gradient })
            }
        }
    }

    private fun applyBackgroundReplacement(canvas: Canvas, bitmap: Bitmap, backgroundPrompt: String) {
        val width = bitmap.width
        val height = bitmap.height
        
        // Background replacement: Draw a premium generative studio backdrop
        val lower = backgroundPrompt.lowercase()
        val bgPaint = Paint()
        
        when {
            lower.contains("hotel") || lower.contains("luxury") -> {
                // Luxury gold/champagne hotel backdrop
                bgPaint.shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    Color.parseColor("#2C2216"), Color.parseColor("#0F0B07"),
                    Shader.TileMode.CLAMP
                )
            }
            lower.contains("neon") || lower.contains("city") || lower.contains("cyber") -> {
                // Cyberpunk studio neon backdrop
                bgPaint.shader = RadialGradient(
                    width / 2f, height / 2f, Math.max(width, height) / 1.2f,
                    Color.parseColor("#2E103E"), Color.parseColor("#0A030C"),
                    Shader.TileMode.CLAMP
                )
            }
            else -> {
                // Professional dark slate studio backdrop
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.parseColor("#1E1E24"), Color.parseColor("#0D0D11"),
                    Shader.TileMode.CLAMP
                )
            }
        }
        
        // Simulating background selection: standard portrait centers are preserved, outer edges are replaced
        val temp = Bitmap.createBitmap(bitmap)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Draw the subject (simulated subject extraction with soft blend)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        
        // Soft focus gradient circle for subject
        val radGrad = RadialGradient(
            width / 2f, height / 2f, Math.max(width, height) / 2.2f,
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        val cutoutMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val cutoutCanvas = Canvas(cutoutMask)
        cutoutCanvas.drawCircle(width / 2f, height / 2f, Math.max(width, height) / 2.3f, Paint().apply { shader = radGrad })
        
        // Intersect original subject with mask
        val subjectBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val subjectCanvas = Canvas(subjectBitmap)
        subjectCanvas.drawBitmap(temp, 0f, 0f, null)
        subjectCanvas.drawBitmap(cutoutMask, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        
        // Draw subject on background
        canvas.drawBitmap(subjectBitmap, 0f, 0f, maskPaint)
    }

    private fun applyObjectRemoval(canvas: Canvas, bitmap: Bitmap, mask: Bitmap?) {
        if (mask == null) return
        val width = bitmap.width
        val height = bitmap.height

        // Object removal: replace marked (white) area in mask with neighborhood average pixels
        val temp = Bitmap.createBitmap(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        
        // Blur neighboring source pixels onto mask area (inpaint simulation)
        val matrix = ColorMatrix().apply { setSaturation(0.9f) }
        val blurPaint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        
        // Draw slightly shifted original over mask area to clone background
        canvas.drawBitmap(temp, -12f, -12f, blurPaint)
        
        // Use mask to only affect painted area
        val maskInverted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(maskInverted)
        mCanvas.drawColor(Color.WHITE)
        mCanvas.drawBitmap(mask, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) })
        
        // Redraw original subject on non-masked areas
        canvas.drawBitmap(temp, 0f, 0f, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
        })
    }

    private fun applyObjectReplacement(canvas: Canvas, bitmap: Bitmap, mask: Bitmap?, prompt: String) {
        if (mask == null) return
        val width = bitmap.width
        val height = bitmap.height

        // Fill mask with prompt-relevant texture/color
        val replacementBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val repCanvas = Canvas(replacementBitmap)
        val repPaint = Paint().apply {
            style = Paint.Style.FILL
            color = when {
                prompt.lowercase().contains("jacket") || prompt.lowercase().contains("black") -> Color.parseColor("#222225")
                prompt.lowercase().contains("red") || prompt.lowercase().contains("dress") -> Color.parseColor("#C92A2A")
                prompt.lowercase().contains("gold") -> Color.parseColor("#FCC419")
                prompt.lowercase().contains("neon") || prompt.lowercase().contains("pink") -> Color.parseColor("#F783AC")
                else -> Color.parseColor("#495057")
            }
        }
        
        repCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), repPaint)
        
        // Apply artistic lighting/shading on replacement
        val shad = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.TRANSPARENT, Color.parseColor("#77000000"), Shader.TileMode.CLAMP)
        repCanvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = shad })

        // Apply replacement mask
        val maskedRep = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mCanvas = Canvas(maskedRep)
        mCanvas.drawBitmap(replacementBitmap, 0f, 0f, null)
        mCanvas.drawBitmap(mask, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })

        // Draw onto target canvas
        canvas.drawBitmap(maskedRep, 0f, 0f, null)
    }

    private fun applyMaskInpaint(canvas: Canvas, bitmap: Bitmap, mask: Bitmap?, prompt: String) {
        applyObjectReplacement(canvas, bitmap, mask, prompt)
    }

    private fun applyEnhancement(canvas: Canvas, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        
        // High fidelity glow and professional color lift
        val matrix = ColorMatrix(floatArrayOf(
            1.08f, 0f, 0f, 0f, 10f,
            0f, 1.08f, 0f, 0f, 10f,
            0f, 0f, 1.12f, 0f, 15f,
            0f, 0f, 0f, 1.0f, 0f
        ))
        val p = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        val temp = Bitmap.createBitmap(bitmap)
        canvas.drawBitmap(temp, 0f, 0f, p)
        
        // Blend a overlay to pop skin tones/highlights
        canvas.drawBitmap(temp, 0f, 0f, Paint().apply {
            alpha = 45
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        })
    }

    private fun applyPromptBlend(canvas: Canvas, bitmap: Bitmap, prompt: String) {
        val width = bitmap.width
        val height = bitmap.height
        val lower = prompt.lowercase()
        
        val blendColor = when {
            lower.contains("red") || lower.contains("warm") || lower.contains("fire") -> Color.parseColor("#1FCE1A1A")
            lower.contains("blue") || lower.contains("cool") || lower.contains("water") -> Color.parseColor("#1F1A77CE")
            lower.contains("gold") || lower.contains("luxury") || lower.contains("sun") -> Color.parseColor("#1FCED61A")
            lower.contains("purple") || lower.contains("neon") || lower.contains("cyber") -> Color.parseColor("#1F8A1ACE")
            else -> Color.parseColor("#159E7AFF") // Purple studio default
        }
        
        canvas.drawColor(blendColor, PorterDuff.Mode.SRC_ATOP)
    }
}
