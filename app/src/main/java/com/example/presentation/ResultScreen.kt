package com.example.presentation

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.GenerationInfo
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: SharedViewModel,
    onNavigateBack: () -> Unit,
    onEditAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val original = viewModel.originalImage ?: return
    val generated = viewModel.generatedImage ?: return
    val info = viewModel.lastGeneratedInfo

    var sliderProgress by remember { mutableStateOf(0.5f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Save success callback trigger
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Studio Output", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
        ) {
            
            // Slider instructions
            Text(
                text = "SWIPE IMAGE TO COMPARE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                textAlign = TextAlign.Center
            )

            // Before After Slider Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ArtisticSurface)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(24.dp))
                    .clipToBounds()
                    .onSizeChanged { containerSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (containerSize.width > 0) {
                                sliderProgress = (change.position.x / containerSize.width).coerceIn(0f, 1f)
                            }
                        }
                    }
                    .testTag("before_after_slider")
            ) {
                val origImageBitmap = remember(original) { original.asImageBitmap() }
                val genImageBitmap = remember(generated) { generated.asImageBitmap() }

                // Lower Layer: Generated Image (Modified)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val viewW = size.width
                    val viewH = size.height
                    val imgW = generated.width.toFloat()
                    val imgH = generated.height.toFloat()

                    val scaleToFit = Math.min(viewW / imgW, viewH / imgH)
                    val fitW = imgW * scaleToFit
                    val fitH = imgH * scaleToFit
                    val left = (viewW - fitW) / 2f
                    val top = (viewH - fitH) / 2f

                    drawImage(
                        image = genImageBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(fitW.toInt(), fitH.toInt())
                    )
                }

                // Upper Layer: Original Image, clipped dynamically based on slider progress
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val viewW = size.width
                    val viewH = size.height
                    val imgW = original.width.toFloat()
                    val imgH = original.height.toFloat()

                    val scaleToFit = Math.min(viewW / imgW, viewH / imgH)
                    val fitW = imgW * scaleToFit
                    val fitH = imgH * scaleToFit
                    val left = (viewW - fitW) / 2f
                    val top = (viewH - fitH) / 2f

                    // Clip bounds to crop on sliderProgress x line
                    val clipWidth = viewW * sliderProgress
                    
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = clipWidth,
                        bottom = viewH
                    ) {
                        drawImage(
                            image = origImageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(fitW.toInt(), fitH.toInt())
                        )
                    }
                }

                // Dynamic Split Line Indicator
                if (containerSize.width > 0) {
                    val splitX = containerSize.width * sliderProgress
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(2.dp)
                            .offset(x = (splitX / (context.resources.displayMetrics.density)).dp)
                            .background(Color.White)
                    ) {
                        // Slider handle badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .background(Color.White, CircleShape)
                                .offset(x = (-17).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Comparison Handle",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Small Tag badges
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Original", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ArtisticViolet.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("AI Mod", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Export Actions Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save HD Button
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.saveToGallery(
                            onSuccess = { msg ->
                                isSaving = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            },
                            onFailure = { err ->
                                isSaving = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArtisticViolet
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(54.dp)
                        .testTag("save_hd_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.SaveAlt, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save HD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Share Button
                IconButton(
                    onClick = {
                        shareImage(context, generated)
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(ArtisticSurfaceVariant, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                }

                // Edit Again / Refine
                IconButton(
                    onClick = onEditAgain,
                    modifier = Modifier
                        .size(54.dp)
                        .background(ArtisticSurface, RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = "Refine", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Style variations & prompt quick-tweaks
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ArtisticSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RECREATION PLATFORM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ArtisticViolet
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Want to iterate further on this output? Tap Regenerate or modify the creative parameters in the editor Workspace.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray, lineHeight = 20.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.executeGeneration()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regenerate", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.enhancePrompt()
                                viewModel.executeGeneration()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ArtisticSurfaceVariant),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Variation", modifier = Modifier.size(16.dp), tint = ArtisticViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Variation", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Metadata info sheet
            if (info != null) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ArtisticSurface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GENERATION METRICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        MetricRow(label = "AI Model Engine", value = info.model)
                        MetricRow(label = "Dimensions Resolution", value = info.resolution)
                        MetricRow(label = "Generation Time", value = "${String.format("%.2f", info.durationMs / 1000f)} seconds")
                        MetricRow(label = "Mathematical Seed", value = info.seed.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
    }
}

// Global Intent to Share image natively with standard systems
private fun shareImage(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "ai_output.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.flush()
        fileOutputStream.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Image Output"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
