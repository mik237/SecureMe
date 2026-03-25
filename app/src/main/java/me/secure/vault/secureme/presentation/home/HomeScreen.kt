package me.secure.vault.secureme.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import me.secure.vault.secureme.core.utils.FileFormatter
import me.secure.vault.secureme.domain.model.HomeTab
import me.secure.vault.secureme.domain.model.TrustedContact
import me.secure.vault.secureme.domain.model.VaultFileEntry
import me.secure.vault.secureme.presentation.navigation.NavigationRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onIntent(HomeUiIntent.ImportFile(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is HomeUiEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeUiEffect.OpenFileViewer -> {
                    navController.navigate("${NavigationRoutes.FILE_VIEWER}/${effect.fileId}")
                }
                is HomeUiEffect.FileSharedSuccessfully -> {
                    snackbarHostState.showSnackbar("File shared successfully")
                }
                is HomeUiEffect.NavigateToUnlock -> {
                    navController.navigate(NavigationRoutes.ONBOARDING) {
                        popUpTo(NavigationRoutes.HOME) { inclusive = true }
                    }
                }
                is HomeUiEffect.NavigateToLogin -> {
                    navController.navigate(NavigationRoutes.LOGIN) {
                        popUpTo(NavigationRoutes.HOME) { inclusive = true }
                    }
                }
            }
        }
    }

    // Deletion Confirmation Dialog
    uiState.fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(HomeUiIntent.DismissDeleteDialog) },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to permanently delete '${file.fileName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(HomeUiIntent.DeleteFile) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(HomeUiIntent.DismissDeleteDialog) }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(
                        "WARNING: This action is permanent and irreversible.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Deleting your account will result in:")
                    BulletPoint("Complete loss of all files in your local vault.")
                    BulletPoint("Removal of all keys and metadata from our servers.")
                    BulletPoint("Deletion of all cloud backups.")
                    BulletPoint("Termination of all active file shares.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you absolutely sure you want to proceed?")
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteAccountDialog = false
                    viewModel.onIntent(HomeUiIntent.DeleteAccount) 
                }) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    // Sharing Dialog
    uiState.fileToShare?.let { file ->
        AlertDialog(
            onDismissRequest = { if (!uiState.isSharing) viewModel.onIntent(HomeUiIntent.DismissShareDialog) },
            title = { Text("Share File") },
            text = {
                Column {
                    Text("Select a trusted contact or enter an email to share '${file.fileName}' securely.")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = uiState.shareRecipientEmail,
                        onValueChange = { viewModel.onIntent(HomeUiIntent.OnShareRecipientChange(it)) },
                        label = { Text("Recipient Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !uiState.isSharing,
                        isError = uiState.shareError != null,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    uiState.shareError?.let { error ->
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (uiState.trustedContacts.isNotEmpty()) {
                        Text(
                            "Trusted Contacts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.trustedContacts) { contact ->
                                ContactPickerItem(
                                    contact = contact,
                                    isSelected = uiState.shareRecipientEmail == contact.email,
                                    onSelect = { viewModel.onIntent(HomeUiIntent.SelectContactForSharing(contact.email)) }
                                )
                            }
                        }
                    } else {
                        Text(
                            "No trusted contacts found. Add them in the Settings tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    if (uiState.isSharing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onIntent(HomeUiIntent.ShareFile) },
                    enabled = uiState.shareRecipientEmail.isNotBlank() && !uiState.isSharing
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onIntent(HomeUiIntent.DismissShareDialog) },
                    enabled = !uiState.isSharing
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SecureMe Vault",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            HomeBottomNavigation(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.onIntent(HomeUiIntent.OnTabSelected(it)) }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab != HomeTab.SETTINGS) {
                FloatingActionButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import File")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.selectedTab == HomeTab.SETTINGS) {
                SettingsTabContent(
                    navController = navController,
                    onLockVault = { viewModel.onIntent(HomeUiIntent.LockVault) },
                    onLogout = { viewModel.onIntent(HomeUiIntent.Logout) },
                    onDeleteAccount = { showDeleteAccountDialog = true }
                )
            } else {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.files.isEmpty()) {
                    EmptyStateView(tab = uiState.selectedTab)
                } else {
                    FileGrid(
                        files = uiState.files,
                        onFileClick = { viewModel.onIntent(HomeUiIntent.OpenFile(it)) },
                        onFileLongClick = { viewModel.onIntent(HomeUiIntent.ConfirmDeleteFile(it)) },
                        onShareClick = { viewModel.onIntent(HomeUiIntent.OnShareFileClick(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
        Text("• ", fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsTabContent(
    navController: NavController,
    onLockVault: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SettingsCategoryHeader("Account & Profile")
        }
        item {
            SettingsItem(
                title = "My Profile",
                icon = Icons.Default.Person,
                onClick = { navController.navigate(NavigationRoutes.PROFILE) }
            )
        }
        item {
            SettingsItem(
                title = "Contacts",
                icon = Icons.Default.People,
                onClick = { navController.navigate(NavigationRoutes.CONTACTS) }
            )
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { SettingsCategoryHeader("File Sharing") }
        
        item {
            SettingsItem(
                title = "Received Files",
                icon = Icons.Default.Inbox,
                onClick = { navController.navigate(NavigationRoutes.SHARED_WITH_ME) }
            )
        }
        item {
            SettingsItem(
                title = "Sent Files",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = { navController.navigate(NavigationRoutes.SENT_SHARES) }
            )
        }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { SettingsCategoryHeader("Security") }
        
        item {
            SettingsItem(
                title = "Lock Vault",
                icon = Icons.Default.Lock,
                onClick = onLockVault
            )
        }
        item {
            SettingsItem(
                title = "Logout",
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onLogout
            )
        }
        item {
            SettingsItem(
                title = "Delete Account",
                icon = Icons.Default.DeleteForever,
                iconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error,
                onClick = onDeleteAccount
            )
        }
    }
}

@Composable
fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ContactPickerItem(
    contact: TrustedContact,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    contact.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    contact.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (contact.isTrusted) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Trusted",
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FileGrid(
    files: List<VaultFileEntry>,
    onFileClick: (VaultFileEntry) -> Unit,
    onFileLongClick: (VaultFileEntry) -> Unit,
    onShareClick: (VaultFileEntry) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.id }) { file ->
            FileCard(
                file = file, 
                onClick = { onFileClick(file) },
                onLongClick = { onFileLongClick(file) },
                onShareClick = { onShareClick(file) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCard(
    file: VaultFileEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getFileIcon(file.mimeType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = FileFormatter.formatFileSize(file.fileSize),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(tab: HomeTab) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = getTabIcon(tab),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No ${tab.name.lowercase()} found",
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap the + button to add files to your vault.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun HomeBottomNavigation(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { 
                    Text(
                        text = tab.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp
                    ) 
                },
                icon = {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = tab.name
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

private fun getTabIcon(tab: HomeTab): ImageVector {
    return when (tab) {
        HomeTab.IMAGES -> Icons.Default.Image
        HomeTab.VIDEOS -> Icons.Default.VideoLibrary
        HomeTab.DOCUMENTS -> Icons.AutoMirrored.Filled.InsertDriveFile
        HomeTab.SETTINGS -> Icons.Default.Settings
    }
}

private fun getFileIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.startsWith("video/") -> Icons.Default.VideoLibrary
        mimeType.startsWith("text/") || mimeType.contains("pdf") || mimeType.contains("word") -> Icons.AutoMirrored.Filled.InsertDriveFile
        else -> Icons.Default.Description
    }
}
