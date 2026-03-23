package me.secure.vault.secureme.presentation.fileviewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewer(file: File, modifier: Modifier = Modifier) {
    var pageCount by remember { mutableIntStateOf(0) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    val mutex = remember { Mutex() }
    val scope = rememberCoroutineScope()
    
    // Viewing States
    var isHorizontal by remember { mutableStateOf(false) }
    var isDarkMode by remember { mutableStateOf(false) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val listState = rememberLazyListState()
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    // Load PDF Renderer
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val pdfRenderer = PdfRenderer(input)
                renderer = pdfRenderer
                pageCount = pdfRenderer.pageCount
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer?.close()
        }
    }

    if (renderer == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 3f
                                }
                            }
                        )
                    }
            ) {
                if (isHorizontal) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                                rotationZ = rotationAngle
                            )
                            .background(if (isDarkMode) Color.Black else Color.DarkGray),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(pageCount) { index ->
                            PdfPageItem(renderer!!, index, mutex, isDarkMode, rotationAngle)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y,
                                rotationZ = rotationAngle
                            )
                            .background(if (isDarkMode) Color.Black else Color.DarkGray),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageCount) { index ->
                            PdfPageItem(renderer!!, index, mutex, isDarkMode, rotationAngle)
                        }
                    }
                }
            }

            // --- UI Overlays ---

            // Page Indicator (Bottom Center)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.clickable { showJumpDialog = true }
                ) {
                    Text(
                        text = "$currentPage / $pageCount",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Controls Bar (Floating Top End)
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PdfControlButton(
                        icon = if (isHorizontal) Icons.Default.ViewStream else Icons.Default.ViewWeek,
                        contentDescription = "Toggle Orientation",
                        onClick = { isHorizontal = !isHorizontal }
                    )
                    PdfControlButton(
                        icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Dark Mode",
                        onClick = { isDarkMode = !isDarkMode }
                    )
                    PdfControlButton(
                        icon = Icons.Default.RotateRight,
                        contentDescription = "Rotate",
                        onClick = { rotationAngle = (rotationAngle + 90f) % 360f }
                    )
                    PdfControlButton(
                        icon = Icons.Default.FullscreenExit,
                        contentDescription = "Reset Zoom",
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var jumpPage by remember { mutableStateOf(currentPage.toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                OutlinedTextField(
                    value = jumpPage,
                    onValueChange = { if (it.all { char -> char.isDigit() }) jumpPage = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Page Number (1-$pageCount)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val page = jumpPage.toIntOrNull()?.minus(1)
                    if (page != null && page in 0 until pageCount) {
                        scope.launch { listState.scrollToItem(page) }
                        showJumpDialog = false
                    }
                }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PdfControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    mutex: Mutex,
    isDarkMode: Boolean,
    rotationAngle: Float
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.Default) {
            mutex.withLock {
                try {
                    val page = renderer.openPage(pageIndex)
                    val renderScale = 2.5f // Balanced quality and performance
                    val width = (page.width * renderScale).toInt()
                    val height = (page.height * renderScale).toInt()
                    
                    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(b)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap = b
                    page.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier
                    .widthIn(max = 800.dp) // Prevent oversized images on tablets
                    .aspectRatio(bitmap!!.width.toFloat() / bitmap!!.height.toFloat()),
                contentScale = ContentScale.Fit,
                colorFilter = if (isDarkMode) {
                    // Invert colors for Dark Mode: (1 - RGB)
                    ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )))
                } else null
            )
        } else {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        }
    }
}
