package me.secure.vault.secureme.presentation.shares

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import me.secure.vault.secureme.core.utils.FileFormatter
import me.secure.vault.secureme.domain.model.ShareRecord
import me.secure.vault.secureme.domain.model.ShareStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentSharesScreen(
    navController: NavController,
    viewModel: SentSharesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is SentSharesUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                SentSharesUiEffect.ShareDeleted -> {
                    snackbarHostState.showSnackbar("Share record and cloud file deleted")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sent") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.shares.isEmpty()) {
                Text(
                    text = "No sent files",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.shares, key = { it.shareId }) { share ->
                        SentShareItemCard(
                            share = share,
                            onDelete = { viewModel.onIntent(SentSharesUiIntent.DeleteShare(share.shareId, share.fileId)) },
                            isDeleting = uiState.isDeleting
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SentShareItemCard(
    share: ShareRecord,
    onDelete: () -> Unit,
    isDeleting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = share.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recipient ID: ${share.recipientId.take(8)}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusBadge(status = share.status)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = FileFormatter.formatFileSize(share.fileSize),
                    style = MaterialTheme.typography.labelMedium
                )
                
                // Show delete button if the share is no longer pending
                if (share.status != ShareStatus.PENDING) {
                    IconButton(
                        onClick = onDelete,
                        enabled = !isDeleting
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Cleanup",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ShareStatus) {
    val color = when (status) {
        ShareStatus.PENDING -> Color.Gray
        ShareStatus.ACCEPTED -> Color(0xFF4CAF50)
        ShareStatus.REJECTED -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
