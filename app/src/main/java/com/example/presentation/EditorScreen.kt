package com.example.presentation

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AspectRatio
import com.example.domain.model.EditTool
import com.example.domain.model.GeneratorSettings
import com.example.domain.model.ImageAdjustment
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: SharedViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val original = viewModel.originalImage ?: return

    // Screen states
    var showMaskEditor by remember { mutableStateOf(false) }
    var showPromptSettingsSheet by remember { mutableStateOf(false) }

    // Navigation and image drawing layers
    val imageToDisplay = viewModel.generatedImage ?: viewModel.originalImage!!

    // Active tool state
    val tool = viewModel.currentTool
    val settings = viewModel.settings
    val adj = viewModel.adjustment

    if (showMaskEditor) {
        MaskEditorCanvas(
            viewModel = viewModel,
            onClose = { showMaskEditor = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.projectName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Quick Save Button
                    IconButton(
                        onClick = {
                            viewModel.saveToGallery(
                                onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                                onFailure = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    ) {
                        Icon(Icons.Default.Save, "Save", tint = Color.White)
                    }

                    // Done/Preview Button
                    if (viewModel.generatedImage != null) {
                        Button(
                            onClick = onNavigateToResult,
                            colors = ButtonDefaults.buttonColors(containerColor = ArtisticViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Output", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArtisticBackground
                )
            )
        },
        containerColor = ArtisticBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ArtisticBackground)
        ) {
            
            // Core Display Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.3f)
                    .padding(20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ArtisticSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Interactive rendering via custom Draw matrix
                val imageBitmap = remember(imageToDisplay) { imageToDisplay.asImageBitmap() }
                
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    val viewW = size.width
                    val viewH = size.height
                    val imgW = imageToDisplay.width.toFloat()
                    val imgH = imageToDisplay.height.toFloat()

                    val scaleToFit = Math.min(viewW / imgW, viewH / imgH)
                    val fitW = imgW * scaleToFit
                    val fitH = imgH * scaleToFit
                    val left = (viewW - fitW) / 2f
                    val top = (viewH - fitH) / 2f

                    // Apply local real-time brightness/contrast previews mathematically via filters
                    drawImage(
                        image = imageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(fitW.toInt(), fitH.toInt())
                    )
                }

                // AI Watermark indicator if showing a generated variant
                if (viewModel.generatedImage != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF9E7AFF).copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "AI GENERATED OUTPUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            )
                        )
                    }
                }
            }

            // Error Panel
            viewModel.errorMessage?.let { error ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1414)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Error, "Error", tint = Color.Red)
                        Text(text = error, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.errorMessage = null }) {
                            Icon(Icons.Default.Close, "Clear Error", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Context Parameter Options (Grows dynamically based on the tool chosen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                when (tool) {
                    EditTool.PROMPT -> {
                        StandardPromptSubPanel(viewModel = viewModel, onShowAdvanced = { showPromptSettingsSheet = true })
                    }
                    EditTool.MASK -> {
                        InpaintingSubPanel(onOpenEditor = { showMaskEditor = true })
                    }
                    EditTool.REMOVE -> {
                        RemoveSubPanel(viewModel = viewModel, onOpenEditor = { showMaskEditor = true })
                    }
                    EditTool.REPLACE -> {
                        ReplaceSubPanel(viewModel = viewModel, onOpenEditor = { showMaskEditor = true })
                    }
                    EditTool.EXPAND -> {
                        ExpandSubPanel(viewModel = viewModel)
                    }
                    EditTool.BACKGROUND -> {
                        BackgroundSubPanel(viewModel = viewModel)
                    }
                    EditTool.STYLE -> {
                        StyleSubPanel(viewModel = viewModel)
                    }
                    EditTool.ENHANCE -> {
                        EnhanceSubPanel(viewModel = viewModel)
                    }
                    EditTool.ADJUST -> {
                        AdjustSubPanel(viewModel = viewModel)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Horizontal Studio Tools Selection Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArtisticSurface)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    StudioToolItem(toolType = EditTool.PROMPT, current = tool, onClick = { viewModel.changeTool(EditTool.PROMPT) })
                    StudioToolItem(toolType = EditTool.MASK, current = tool, onClick = { viewModel.changeTool(EditTool.MASK) })
                    StudioToolItem(toolType = EditTool.REMOVE, current = tool, onClick = { viewModel.changeTool(EditTool.REMOVE) })
                    StudioToolItem(toolType = EditTool.REPLACE, current = tool, onClick = { viewModel.changeTool(EditTool.REPLACE) })
                    StudioToolItem(toolType = EditTool.EXPAND, current = tool, onClick = { viewModel.changeTool(EditTool.EXPAND) })
                    StudioToolItem(toolType = EditTool.BACKGROUND, current = tool, onClick = { viewModel.changeTool(EditTool.BACKGROUND) })
                    StudioToolItem(toolType = EditTool.STYLE, current = tool, onClick = { viewModel.changeTool(EditTool.STYLE) })
                    StudioToolItem(toolType = EditTool.ENHANCE, current = tool, onClick = { viewModel.changeTool(EditTool.ENHANCE) })
                    StudioToolItem(toolType = EditTool.ADJUST, current = tool, onClick = { viewModel.changeTool(EditTool.ADJUST) })
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }

    // Advanced prompt settings Bottom Sheet
    if (showPromptSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPromptSettingsSheet = false },
            containerColor = ArtisticSurfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Advanced Creative Settings",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Creativity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Creativity", color = Color.Gray, fontSize = 13.sp)
                        Text(String.format("%.1f", settings.creativity), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.creativity,
                        onValueChange = { viewModel.updateSettings(settings.copy(creativity = it)) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF9E7AFF), activeTrackColor = Color(0xFF9E7AFF))
                    )
                }

                // Image influence strength
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Image Influence", color = Color.Gray, fontSize = 13.sp)
                        Text(String.format("%.1f", settings.imageStrength), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.imageStrength,
                        onValueChange = { viewModel.updateSettings(settings.copy(imageStrength = it)) },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF9E7AFF), activeTrackColor = Color(0xFF9E7AFF))
                    )
                }

                // Negative Prompt
                OutlinedTextField(
                    value = viewModel.negativePromptText,
                    onValueChange = { viewModel.negativePromptText = it },
                    label = { Text("Negative Prompt") },
                    placeholder = { Text("unwanted objects, bad lighting, blur...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF9E7AFF),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { showPromptSettingsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Settings")
                }
            }
        }
    }
}

@Composable
fun StudioToolItem(
    toolType: EditTool,
    current: EditTool,
    onClick: () -> Unit
) {
    val active = toolType == current
    val icon = when (toolType) {
        EditTool.PROMPT -> Icons.Default.ChatBubbleOutline
        EditTool.MASK -> Icons.Default.Brush
        EditTool.REMOVE -> Icons.Default.AutoFixNormal
        EditTool.REPLACE -> Icons.Default.SwapHoriz
        EditTool.EXPAND -> Icons.Default.CropFree
        EditTool.BACKGROUND -> Icons.Default.Wallpaper
        EditTool.STYLE -> Icons.Default.ColorLens
        EditTool.ENHANCE -> Icons.Default.AutoFixHigh
        EditTool.ADJUST -> Icons.Default.Tune
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) ArtisticViolet.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("tool_${toolType.name.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = toolType.displayName,
            tint = if (active) ArtisticViolet else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = toolType.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (active) ArtisticViolet else Color.Gray,
                fontSize = 11.sp
            )
        )
    }
}

// --- Specific sub panels for bottom items ---

@Composable
fun StandardPromptSubPanel(viewModel: SharedViewModel, onShowAdvanced: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Describe Your Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            IconButton(onClick = onShowAdvanced) {
                Icon(Icons.Default.Settings, "Advanced", tint = Color.Gray)
            }
        }

        OutlinedTextField(
            value = viewModel.promptText,
            onValueChange = { viewModel.promptText = it },
            placeholder = { Text("Change dress to red, cinematic sunset hotel backdrop...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ArtisticViolet,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = ArtisticSurface,
                unfocusedContainerColor = ArtisticSurface
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .testTag("prompt_text_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.enhancePrompt() },
                colors = ButtonDefaults.buttonColors(containerColor = ArtisticSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoAwesome, "Magic", tint = ArtisticViolet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Enhance Prompt")
            }

            Button(
                onClick = { viewModel.executeGeneration() },
                colors = ButtonDefaults.buttonColors(containerColor = ArtisticViolet),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("generate_button")
            ) {
                Icon(Icons.Default.FlashOn, "Generate")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate AI")
            }
        }
    }
}

@Composable
fun InpaintingSubPanel(onOpenEditor: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF15151A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Inpainting Area Select", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Draw a mask over any specific object (e.g. shirt, hair, background) to replace it with generative details.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenEditor,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Brush, "Draw")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Mask Canvas")
            }
        }
    }
}

@Composable
fun RemoveSubPanel(viewModel: SharedViewModel, onOpenEditor: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remove Object with AI", color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenEditor) {
                Icon(Icons.Default.Brush, "Mask", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit Mask", color = Color(0xFF9E7AFF))
            }
        }

        Text(
            "Draw a mask overlay over the target object, and tap Remove to erase it perfectly from the photo.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Button(
            onClick = {
                viewModel.promptText = "Erase and remove the masked object cleanly, inpaint background textures seamlessly"
                viewModel.executeGeneration()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoFixNormal, "Remove")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Erase Masked Object")
        }
    }
}

@Composable
fun ReplaceSubPanel(viewModel: SharedViewModel, onOpenEditor: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AI Generative Replacement", color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = onOpenEditor) {
                Icon(Icons.Default.Brush, "Mask", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Draw Mask", color = Color(0xFF9E7AFF))
            }
        }

        OutlinedTextField(
            value = viewModel.promptText,
            onValueChange = { viewModel.promptText = it },
            placeholder = { Text("What do you want to replace it with? E.g. black leather jacket") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF9E7AFF),
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF15151A),
                unfocusedContainerColor = Color(0xFF15151A)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.executeGeneration() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SwapHoriz, "Replace")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Replace Object")
        }
    }
}

@Composable
fun ExpandSubPanel(viewModel: SharedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Outpainting & Aspect Ratio Expansion", color = Color.White, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AspectRatio.values().forEach { r ->
                val active = viewModel.settings.aspectRatio == r
                FilterChip(
                    selected = active,
                    onClick = { viewModel.updateSettings(viewModel.settings.copy(aspectRatio = r)) },
                    label = { Text(r.ratio) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9E7AFF),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = {
                viewModel.promptText = "Seamlessly outpaint and expand the photograph coordinates to a wider format"
                viewModel.executeGeneration()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CropFree, "Expand")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Expand Canvas Layout")
        }
    }
}

@Composable
fun BackgroundSubPanel(viewModel: SharedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI Background Replacement", color = Color.White, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val presets = listOf("Luxury Hotel Lobby", "Cyberpunk Neon City backdrop", "Professional Studio Slate background")
            presets.forEach { preset ->
                val active = viewModel.promptText.contains(preset)
                FilterChip(
                    selected = active,
                    onClick = { viewModel.promptText = "Replace the background with: $preset" },
                    label = { Text(preset) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9E7AFF),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = { viewModel.executeGeneration() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Wallpaper, "Background")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Background")
        }
    }
}

@Composable
fun StyleSubPanel(viewModel: SharedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Creative Style Transformation", color = Color.White, fontWeight = FontWeight.Bold)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val styles = listOf("Watercolor Painting", "Oil Painting Brush Strokes", "Pencil Drawing Sketch", "Vibrant Anime Illustration", "Dramatic Cinematic Color Grade")
            styles.forEach { style ->
                val active = viewModel.promptText.contains(style)
                FilterChip(
                    selected = active,
                    onClick = { viewModel.promptText = "Transform into a masterfully detailed: $style" },
                    label = { Text(style.split(" ").first()) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9E7AFF),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Button(
            onClick = { viewModel.executeGeneration() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ColorLens, "Style")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply Style Filter")
        }
    }
}

@Composable
fun EnhanceSubPanel(viewModel: SharedViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("AI Detail Portrait Enhancer", color = Color.White, fontWeight = FontWeight.Bold)
        
        Text(
            "Automatically reduces digital noise, corrects color maps, increases pixel sharpness, and brightens eyes/skin naturally.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Button(
            onClick = {
                viewModel.promptText = "Enhance the details of this image, clean skin naturally, improve contrast and dynamic lighting"
                viewModel.executeGeneration()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E7AFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoFixHigh, "Enhance")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Apply AI HD Enhancement")
        }
    }
}

@Composable
fun AdjustSubPanel(viewModel: SharedViewModel) {
    val adj = viewModel.adjustment

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Mathematical Matrix Adjustments", color = Color.White, fontWeight = FontWeight.Bold)

        // 1. Brightness
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness", color = Color.Gray, fontSize = 12.sp)
                Text(String.format("%.2f", adj.brightness), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = adj.brightness,
                onValueChange = { viewModel.updateAdjustment(adj.copy(brightness = it)) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF9E7AFF), activeTrackColor = Color(0xFF9E7AFF)),
                modifier = Modifier.height(24.dp)
            )
        }

        // 2. Contrast
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contrast", color = Color.Gray, fontSize = 12.sp)
                Text(String.format("%.2f", adj.contrast), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = adj.contrast,
                onValueChange = { viewModel.updateAdjustment(adj.copy(contrast = it)) },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF9E7AFF), activeTrackColor = Color(0xFF9E7AFF)),
                modifier = Modifier.height(24.dp)
            )
        }

        // 3. Saturation
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saturation", color = Color.Gray, fontSize = 12.sp)
                Text(String.format("%.2f", adj.saturation), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = adj.saturation,
                onValueChange = { viewModel.updateAdjustment(adj.copy(saturation = it)) },
                valueRange = 0.0f..2.0f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF9E7AFF), activeTrackColor = Color(0xFF9E7AFF)),
                modifier = Modifier.height(24.dp)
            )
        }
    }
}
