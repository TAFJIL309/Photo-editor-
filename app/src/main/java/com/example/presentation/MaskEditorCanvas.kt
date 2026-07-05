package com.example.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EditTool
import java.util.Stack

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MaskEditorCanvas(
    viewModel: SharedViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val original = viewModel.originalImage ?: return
    val mask = viewModel.maskImage ?: return

    var isEraseMode by remember { mutableStateOf(false) }
    var brushSize by remember { mutableStateOf(40f) }
    var brushOpacity by remember { mutableStateOf(0.7f) }
    var showOverlay by remember { mutableStateOf(true) }

    // Navigation and pan zoom variables
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isPanZoomMode by remember { mutableStateOf(false) }

    // Canvas sizing
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Native paint objects
    val drawPaint = remember(brushSize, brushOpacity, isEraseMode) {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = brushSize
            if (isEraseMode) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = Color.WHITE
                alpha = (brushOpacity * 255).toInt().coerceIn(0, 255)
            }
        }
    }

    // Undo / Redo Stacks
    val undoStack = remember { Stack<Bitmap>() }
    val redoStack = remember { Stack<Bitmap>() }

    fun saveHistory() {
        val backup = Bitmap.createBitmap(mask)
        undoStack.push(backup)
        redoStack.clear()
        if (undoStack.size > 20) {
            undoStack.removeAt(0)
        }
    }

    fun handleUndo() {
        if (undoStack.isNotEmpty()) {
            val currentBackup = Bitmap.createBitmap(mask)
            redoStack.push(currentBackup)
            
            val previous = undoStack.pop()
            // Clear current mask and copy previous onto it
            val tempCanvas = Canvas(mask)
            tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            tempCanvas.drawBitmap(previous, 0f, 0f, null)
            
            // Trigger Compose recomposition
            viewModel.updateMaskBitmap(Bitmap.createBitmap(mask))
        }
    }

    fun handleRedo() {
        if (redoStack.isNotEmpty()) {
            val currentBackup = Bitmap.createBitmap(mask)
            undoStack.push(currentBackup)
            
            val next = redoStack.pop()
            val tempCanvas = Canvas(mask)
            tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            tempCanvas.drawBitmap(next, 0f, 0f, null)
            
            viewModel.updateMaskBitmap(Bitmap.createBitmap(mask))
        }
    }

    // Interactive rendering
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFF070709))
    ) {
        // Workspace header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(androidx.compose.ui.graphics.Color(0xFF1E1E24), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = androidx.compose.ui.graphics.Color.White)
                }
                Text(
                    text = "Inpainting Mask Editor",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Undo / Redo controls
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { handleUndo() },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.background(
                        if (undoStack.isNotEmpty()) androidx.compose.ui.graphics.Color(0xFF1E1E24) else androidx.compose.ui.graphics.Color(0xFF111115),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.DarkGray
                    )
                }
                IconButton(
                    onClick = { handleRedo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.background(
                        if (redoStack.isNotEmpty()) androidx.compose.ui.graphics.Color(0xFF1E1E24) else androidx.compose.ui.graphics.Color(0xFF111115),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.DarkGray
                    )
                }
            }
        }

        // Active drawing canvas box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 150.dp)
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(isPanZoomMode) {
                    if (isPanZoomMode) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.8f, 5f)
                            offset += pan
                        }
                    }
                }
        ) {
            var currentPath by remember { mutableStateOf<Path?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInteropFilter { motionEvent ->
                        if (isPanZoomMode) return@pointerInteropFilter false

                        // Canvas scale calculations to coordinate touch down to underlying Bitmap bounds
                        val viewW = canvasSize.width.toFloat()
                        val viewH = canvasSize.height.toFloat()
                        val imgW = original.width.toFloat()
                        val imgH = original.height.toFloat()

                        if (viewW == 0f || viewH == 0f || imgW == 0f || imgH == 0f) return@pointerInteropFilter false

                        // Scale inside letterbox
                        val scaleToFit = Math.min(viewW / imgW, viewH / imgH)
                        val fitWidth = imgW * scaleToFit
                        val fitHeight = imgH * scaleToFit
                        val leftOffset = (viewW - fitWidth) / 2f
                        val topOffset = (viewH - fitHeight) / 2f

                        // Correcting for pan/zoom scale offset
                        val relativeX = (motionEvent.x - offset.x) / scale
                        val relativeY = (motionEvent.y - offset.y) / scale

                        // Calculate pixel indices on the original image bounds
                        val bitmapTouchX = ((relativeX - leftOffset) / scaleToFit).coerceIn(0f, imgW)
                        val bitmapTouchY = ((relativeY - topOffset) / scaleToFit).coerceIn(0f, imgH)

                        val nativeCanvas = Canvas(mask)

                        when (motionEvent.action) {
                            MotionEvent.ACTION_DOWN -> {
                                saveHistory()
                                currentPath = Path().apply {
                                    moveTo(bitmapTouchX, bitmapTouchY)
                                }
                                nativeCanvas.drawCircle(bitmapTouchX, bitmapTouchY, brushSize / 2f, drawPaint)
                                viewModel.updateMaskBitmap(Bitmap.createBitmap(mask))
                            }
                            MotionEvent.ACTION_MOVE -> {
                                currentPath?.let { path ->
                                    path.lineTo(bitmapTouchX, bitmapTouchY)
                                    nativeCanvas.drawPath(path, drawPaint)
                                    viewModel.updateMaskBitmap(Bitmap.createBitmap(mask))
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                currentPath = null
                            }
                        }
                        true
                    }
            ) {
                // Determine drawing bounds (Letterbox)
                val viewW = size.width
                val viewH = size.height
                val imgW = original.width.toFloat()
                val imgH = original.height.toFloat()

                val scaleToFit = Math.min(viewW / imgW, viewH / imgH)
                val fitW = imgW * scaleToFit
                val fitH = imgH * scaleToFit
                val left = (viewW - fitW) / 2f
                val top = (viewH - fitH) / 2f

                val destRect = androidx.compose.ui.geometry.Rect(left, top, left + fitW, top + fitH)

                // 1. Draw source photo
                drawImage(
                    image = original.asImageBitmap(),
                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(fitW.toInt(), fitH.toInt())
                )

                // 2. Overlay generated semi-transparent red inpainting layer
                if (showOverlay) {
                    drawImage(
                        image = mask.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(fitW.toInt(), fitH.toInt()),
                        alpha = 0.55f,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                            androidx.compose.ui.graphics.Color.Red,
                            androidx.compose.ui.graphics.BlendMode.SrcAtop
                        )
                    )
                }
            }
        }

        // Bottom Editor panel
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF15151A)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brush attributes: size and opacity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Size",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(50.dp)
                    )
                    Slider(
                        value = brushSize,
                        onValueChange = { brushSize = it },
                        valueRange = 10f..150f,
                        colors = SliderDefaults.colors(
                            thumbColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF),
                            activeTrackColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${brushSize.toInt()}px",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(45.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alpha",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(50.dp)
                    )
                    Slider(
                        value = brushOpacity,
                        onValueChange = { brushOpacity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF),
                            activeTrackColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(brushOpacity * 100).toInt()}%",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(45.dp)
                    )
                }

                Divider(color = androidx.compose.ui.graphics.Color.DarkGray)

                // Brush buttons & action bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Drawing brush selection
                        FilterChip(
                            selected = !isEraseMode && !isPanZoomMode,
                            onClick = {
                                isEraseMode = false
                                isPanZoomMode = false
                            },
                            label = { Text("Brush") },
                            leadingIcon = { Icon(Icons.Default.Brush, "Brush") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF),
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                        
                        // Erasing brush selection
                        FilterChip(
                            selected = isEraseMode && !isPanZoomMode,
                            onClick = {
                                isEraseMode = true
                                isPanZoomMode = false
                            },
                            label = { Text("Eraser") },
                            leadingIcon = { Icon(Icons.Default.Clear, "Eraser") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF),
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White
                            )
                        )

                        // Navigate/pan/zoom selector
                        FilterChip(
                            selected = isPanZoomMode,
                            onClick = { isPanZoomMode = true },
                            label = { Text("Pan/Zoom") },
                            leadingIcon = { Icon(Icons.Default.ZoomIn, "Pan Zoom") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = androidx.compose.ui.graphics.Color(0xFF9E7AFF),
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }

                    // Mask toggles
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { showOverlay = !showOverlay },
                            modifier = Modifier.background(
                                if (showOverlay) androidx.compose.ui.graphics.Color(0xFF9E7AFF).copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                                CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = if (showOverlay) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Overlay",
                                tint = if (showOverlay) androidx.compose.ui.graphics.Color(0xFF9E7AFF) else androidx.compose.ui.graphics.Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.invertMask() }) {
                            Icon(Icons.Default.InvertColors, "Invert Mask", tint = androidx.compose.ui.graphics.Color.White)
                        }
                        IconButton(onClick = { viewModel.clearMask() }) {
                            Icon(Icons.Default.LayersClear, "Clear Mask", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }
        }
    }
}
