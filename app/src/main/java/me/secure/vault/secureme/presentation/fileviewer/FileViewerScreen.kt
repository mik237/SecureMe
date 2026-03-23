@file:OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
package me.secure.vault.secureme.presentation.fileviewer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FileViewerScreen(
    fileId: String,
    navController: NavController,
    viewModel: FileViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(fileId) {
        viewModel.onIntent(FileViewerUiIntent.LoadFile(fileId))
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is FileViewerUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.fileEntry?.fileName ?: "Viewer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            val state = uiState
            when {
                state.isLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                state.errorMessage != null -> {
                    ErrorView(message = state.errorMessage)
                }
                state.decryptedFile != null -> {
                    MediaContent(
                        file = state.decryptedFile,
                        mimeType = state.fileEntry?.mimeType ?: ""
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaContent(file: File, mimeType: String) {
    when {
        mimeType.startsWith("image/") -> {
            AsyncImage(
                model = file,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        mimeType.startsWith("video/") -> {
            VideoPlayer(file = file)
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No preview available for this file type",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(file: File) {
    val context = LocalContext.current
    
    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(file.absolutePath))
                prepare()
                playWhenReady = true
            }
    }

    var volume by remember { mutableFloatStateOf(exoPlayer.volume) }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Volume control overlay at bottom right
        /*Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 24.dp)
                .height(350.dp) // Significantly taller as requested
                .width(56.dp),
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Percentage Text (Wrap content height)
                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Slider area (Weight 1, takes remaining height)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            exoPlayer.volume = it
                        },
                        modifier = Modifier
                            .rotate(-90f)
                            .width(maxHeight), // Match parent height (maxHeight of weight-allocated space)
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // IconButton (Wrap content height)
                IconButton(
                    onClick = {
                        volume = if (volume > 0f) 0f else 1f
                        exoPlayer.volume = volume
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when {
                            volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                            volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Toggle Mute",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }*/
    }
}

@Composable
private fun ErrorView(message: String) {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}
