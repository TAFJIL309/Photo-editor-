package com.example.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.utils.ImageUtils
import com.example.data.local.ProjectEntity
import com.example.data.provider.GeminiImageEditProvider
import com.example.data.provider.MockImageEditProvider
import com.example.domain.model.*
import com.example.domain.provider.ImageEditProvider
import com.example.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat

class SharedViewModel(
    private val repository: ProjectRepository,
    private val context: Context
) : ViewModel() {

    // --- State Variables ---
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var selectedProjectId by mutableStateOf<Int?>(null)
        private set

    var projectName by mutableStateOf("")
        private set

    var originalImage by mutableStateOf<Bitmap?>(null)
        private set

    var maskImage by mutableStateOf<Bitmap?>(null)
        private set

    var generatedImage by mutableStateOf<Bitmap?>(null)
        private set

    var currentTool by mutableStateOf(EditTool.PROMPT)
        private set

    var settings by mutableStateOf(GeneratorSettings())
        private set

    var adjustment by mutableStateOf(ImageAdjustment())
        private set

    var promptText by mutableStateOf("")
    var negativePromptText by mutableStateOf("")

    var isGenerating by mutableStateOf(false)
        private set

    var generationStage by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)

    var lastGeneratedInfo by mutableStateOf<GenerationInfo?>(null)
        private set

    var beforeAfterProgress by mutableStateOf(0.5f)

    var providerType by mutableStateOf(ProviderType.MOCK)
        private set

    var selectedModel by mutableStateOf("gemini-2.5-flash-image")
        private set

    // Active providers
    private val mockProvider = MockImageEditProvider()
    private var geminiProvider: GeminiImageEditProvider? = null

    init {
        // Initialize Gemini provider with key from BuildConfig
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        geminiProvider = GeminiImageEditProvider(apiKey)
    }

    private fun getActiveProvider(): ImageEditProvider {
        return if (providerType == ProviderType.GEMINI) {
            geminiProvider ?: mockProvider
        } else {
            mockProvider
        }
    }

    // --- Project Operations ---

    fun createProjectFromUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = ImageUtils.loadDownsampledBitmap(context, uri)
            if (bitmap != null) {
                originalImage = bitmap
                maskImage = null
                generatedImage = null
                errorMessage = null
                lastGeneratedInfo = null
                promptText = ""
                negativePromptText = ""
                adjustment = ImageAdjustment()
                currentTool = EditTool.PROMPT

                val name = "Project " + SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                projectName = name

                // Save to local managed app copy
                val localPath = ImageUtils.saveBitmapToInternalStorage(context, bitmap, "original_")
                
                val entity = ProjectEntity(
                    name = name,
                    originalImagePath = localPath,
                    generatedImagePath = null,
                    maskImagePath = null,
                    prompt = "",
                    negativePrompt = "",
                    modelUsed = settings.model,
                    creativity = settings.creativity,
                    imageStrength = settings.imageStrength,
                    promptStrength = settings.promptStrength,
                    aspectRatio = settings.aspectRatio.name
                )
                val id = repository.insertProject(entity)
                selectedProjectId = id.toInt()
            } else {
                errorMessage = "Failed to load image format. Ensure it is a valid JPG/PNG/WEBP."
            }
        }
    }

    fun loadProject(project: ProjectEntity) {
        viewModelScope.launch {
            selectedProjectId = project.id
            projectName = project.name
            errorMessage = null
            promptText = project.prompt
            negativePromptText = project.negativePrompt
            settings = settings.copy(
                model = project.modelUsed,
                creativity = project.creativity,
                imageStrength = project.imageStrength,
                promptStrength = project.promptStrength,
                aspectRatio = AspectRatio.values().firstOrNull { it.name == project.aspectRatio } ?: AspectRatio.SQUARE
            )

            // Load bitmaps from local paths
            originalImage = project.originalImagePath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            generatedImage = project.generatedImagePath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            maskImage = project.maskImagePath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }
            
            lastGeneratedInfo = generatedImage?.let {
                GenerationInfo(
                    model = project.modelUsed,
                    resolution = "${it.width}x${it.height}",
                    durationMs = 0L,
                    seed = 0L
                )
            }
        }
    }

    fun renameProject(id: Int, newName: String) {
        viewModelScope.launch {
            val p = repository.getProjectById(id)
            if (p != null) {
                val updated = p.copy(name = newName, lastModifiedAt = System.currentTimeMillis())
                repository.updateProject(updated)
                if (selectedProjectId == id) {
                    projectName = newName
                }
            }
        }
    }

    fun duplicateProject(project: ProjectEntity) {
        viewModelScope.launch {
            val duplicated = project.copy(
                id = 0, // Reset for autoGen
                name = "${project.name} Copy",
                createdAt = System.currentTimeMillis(),
                lastModifiedAt = System.currentTimeMillis()
            )
            repository.insertProject(duplicated)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (selectedProjectId == project.id) {
                clearWorkspace()
            }
        }
    }

    fun clearWorkspace() {
        selectedProjectId = null
        projectName = ""
        originalImage = null
        maskImage = null
        generatedImage = null
        errorMessage = null
        lastGeneratedInfo = null
        promptText = ""
        negativePromptText = ""
        adjustment = ImageAdjustment()
    }

    // --- Configuration changes ---

    fun changeTool(tool: EditTool) {
        currentTool = tool
        if (tool == EditTool.MASK && maskImage == null && originalImage != null) {
            // Initialize empty mask
            val w = originalImage!!.width
            val h = originalImage!!.height
            maskImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }
    }

    fun updateSettings(newSettings: GeneratorSettings) {
        settings = newSettings
    }

    fun updateAdjustment(newAdjustment: ImageAdjustment) {
        adjustment = newAdjustment
    }

    fun updateProvider(type: ProviderType) {
        providerType = type
        settings = settings.copy(provider = type)
    }

    fun updateModel(modelName: String) {
        selectedModel = modelName
        settings = settings.copy(model = modelName)
    }

    fun updateMaskBitmap(bitmap: Bitmap) {
        maskImage = bitmap
    }

    fun clearMask() {
        maskImage?.let { mask ->
            mask.eraseColor(android.graphics.Color.TRANSPARENT)
            // Trigger recompose by forcing reference update
            maskImage = Bitmap.createBitmap(mask)
        }
    }

    fun invertMask() {
        maskImage?.let { mask ->
            val width = mask.width
            val height = mask.height
            val inverted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(inverted)
            val paint = android.graphics.Paint().apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            canvas.drawBitmap(mask, 0f, 0f, paint)
            maskImage = inverted
        }
    }

    // --- AI Engine Operations ---

    fun enhancePrompt() {
        if (promptText.isBlank()) {
            promptText = "A dramatic studio model portrait"
        }
        val enhancements = listOf(
            "highly detailed, volumetric dramatic sunset rim-lighting, 8k resolution, cinematic aesthetic, shallow depth of field",
            "hyper-realistic photography, ultra-fine details, modern professional photography studio, elegant ambient atmosphere",
            "glowing neon cyberpunk vibe, rich high-contrast colors, cinematic commercial production, masterfully sharp, detailed texturing",
            "soft editorial studio illumination, professional color grading, ultra-sharp focus, commercial beauty portraiture style"
        )
        promptText = "$promptText, ${enhancements.random()}"
    }

    fun executeGeneration() {
        val original = originalImage ?: return
        errorMessage = null
        isGenerating = true

        viewModelScope.launch {
            try {
                // Multi-step premium animation pipeline states
                generationStage = "Preparing Image..."
                delay(700)
                generationStage = "Analyzing Edit parameters..."
                delay(800)
                generationStage = "Generating pixel matrices..."
                delay(1000)
                generationStage = "Enhancing fine textures..."
                delay(600)
                generationStage = "Finalizing composition..."
                delay(400)

                val request = EditRequest(
                    originalImage = original,
                    prompt = promptText,
                    tool = currentTool,
                    maskImage = maskImage,
                    settings = settings,
                    adjustment = adjustment
                )

                val provider = getActiveProvider()
                when (val response = provider.editImage(request)) {
                    is EditResponse.Success -> {
                        generatedImage = response.generatedImage
                        lastGeneratedInfo = response.info
                        beforeAfterProgress = 0.5f

                        // Save generated result locally
                        val genPath = ImageUtils.saveBitmapToInternalStorage(context, response.generatedImage, "gen_")
                        val maskPath = maskImage?.let { ImageUtils.saveBitmapToInternalStorage(context, it, "mask_") }

                        // Update in Database
                        selectedProjectId?.let { projectId ->
                            val p = repository.getProjectById(projectId)
                            if (p != null) {
                                val updated = p.copy(
                                    generatedImagePath = genPath,
                                    maskImagePath = maskPath,
                                    prompt = promptText,
                                    negativePrompt = negativePromptText,
                                    modelUsed = settings.model,
                                    lastModifiedAt = System.currentTimeMillis()
                                )
                                repository.updateProject(updated)
                            }
                        }
                    }
                    is EditResponse.Error -> {
                        errorMessage = response.message
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "An unexpected error occurred during generation."
            } finally {
                isGenerating = false
                generationStage = ""
            }
        }
    }

    // --- Save and Share API ---

    fun saveToGallery(onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val imageToSave = generatedImage ?: originalImage ?: return
        viewModelScope.launch {
            val uri = ImageUtils.exportBitmapToGallery(context, imageToSave, "Creative_Studio")
            if (uri != null) {
                onSuccess("Saved to gallery: Pictures/AICreativeStudio")
            } else {
                onFailure("Failed to export image.")
            }
        }
    }

    private suspend fun delay(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }
}

class SharedViewModelFactory(
    private val repository: ProjectRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
