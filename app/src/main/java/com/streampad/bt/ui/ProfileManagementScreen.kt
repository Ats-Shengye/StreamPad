package com.streampad.bt.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.streampad.bt.model.Shortcut
import com.streampad.bt.model.ShortcutCategory
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.streampad.bt.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val availableProfiles by viewModel.getAvailableProfiles().collectAsStateWithLifecycle()
    val currentProfile by viewModel.getCurrentProfile().collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf<String?>(null) }
    
    // プレビューモード用の状態
    var previewProfile by remember { mutableStateOf<String?>(null) }
    var previewShortcuts by remember { mutableStateOf<List<Shortcut>>(emptyList()) }
    var editingShortcuts by remember { mutableStateOf<List<Shortcut>>(emptyList()) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    
    val context = LocalContext.current
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonContent = inputStream?.bufferedReader()?.use { it.readText() }
                
                if (jsonContent != null) {
                    // ファイル名から拡張子を除いたプロファイル名を生成
                    val fileName = getFileName(context, uri) ?: "インポートプロファイル"
                    val profileName = fileName.removeSuffix(".json")
                    
                    val success = viewModel.importProfile(jsonContent, profileName)
                    importMessage = if (success) {
                        "プロファイル「$profileName」をインポートしました"
                    } else {
                        "インポートに失敗しました。ファイル形式を確認してください"
                    }
                } else {
                    importMessage = "ファイルの読み取りに失敗しました"
                }
            } catch (e: Exception) {
                importMessage = "エラー: ${e.message}"
            }
        }
    }
    
    // インポートメッセージの自動消去
    LaunchedEffect(importMessage) {
        importMessage?.let {
            kotlinx.coroutines.delay(3000)
            importMessage = null
        }
    }
    
    // プレビューモード時のレイアウト
    if (previewProfile != null) {
        ProfilePreviewScreen(
            profileName = previewProfile!!,
            shortcuts = editingShortcuts,
            onShortcutMove = { fromIndex, toIndex ->
                val newShortcuts = editingShortcuts.toMutableList()
                if (fromIndex < newShortcuts.size && toIndex < newShortcuts.size) {
                    val item = newShortcuts.removeAt(fromIndex)
                    newShortcuts.add(toIndex, item)
                    editingShortcuts = newShortcuts
                }
            },
            onSave = {
                viewModel.saveProfileWithShortcuts(previewProfile!!, editingShortcuts)
                previewProfile = null
            },
            onCancel = {
                previewProfile = null
            }
        )
    } else {
        // 通常のプロファイル管理画面
        ProfileManagementContent(
            availableProfiles = availableProfiles,
            currentProfile = currentProfile,
            importMessage = importMessage,
            onProfileLongPress = { profileName ->
                previewProfile = profileName
                val shortcuts = viewModel.getProfileShortcuts(profileName)
                previewShortcuts = shortcuts
                editingShortcuts = shortcuts
            },
            onApplyProfile = { profileName ->
                viewModel.loadProfile(profileName)
                onNavigateBack()
            },
            onDeleteProfile = { profileName ->
                viewModel.deleteProfile(profileName)
            },
            onImportClick = { filePickerLauncher.launch("application/json") },
            onSaveClick = { showSaveDialog = true },
            onNavigateBack = onNavigateBack
        )
    }
    
    // Save Dialog
    if (showSaveDialog) {
        ProfileSaveDialog(
            newProfileName = newProfileName,
            onNameChange = { newProfileName = it },
            onSave = {
                if (newProfileName.isNotBlank()) {
                    viewModel.saveCurrentAsProfile(newProfileName)
                    showSaveDialog = false
                    newProfileName = ""
                }
            },
            onDismiss = { 
                showSaveDialog = false 
                newProfileName = ""
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementContent(
    availableProfiles: List<String>,
    currentProfile: String,
    importMessage: String?,
    onProfileLongPress: (String) -> Unit,
    onApplyProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onImportClick: () -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Header
        TopAppBar(
            title = { 
                Text(
                    stringResource(id = R.string.profile_management),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0F0F1E)
            ),
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = Color.White
                    )
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Profile Info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.current_profile),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentProfile,
                        color = Color(0xFF64B5F6),
                        fontSize = 14.sp
                    )
                }
            }
            
            // Profile List
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.available_profiles),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableProfiles) { profileName ->
                            ProfileItem(
                                profileName = profileName,
                                isCurrentProfile = profileName == currentProfile,
                                onApply = { onApplyProfile(profileName) },
                                onDelete = { onDeleteProfile(profileName) },
                                onLongPress = { onProfileLongPress(profileName) }
                            )
                        }
                    }
                }
            }
            
            // Import Message
            importMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("失敗") || message.contains("エラー")) 
                            Color(0xFF4A2A2E) else Color(0xFF2A4A3E)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF64B5F6)
                    )
                ) {
                    Text(stringResource(id = R.string.pick_file), color = Color.White)
                }
                
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text(stringResource(id = R.string.save_current_settings), color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ProfileItem(
    profileName: String,
    isCurrentProfile: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentProfile) 
                Color(0xFF2A4A3E) else Color(0xFF2A2A3E)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profileName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrentProfile) FontWeight.Bold else FontWeight.Normal
                )
                if (isCurrentProfile) {
                    Text(
                        text = stringResource(id = R.string.in_use_marker),
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCurrentProfile) {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF64B5F6)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(id = R.string.apply), fontSize = 12.sp)
                    }
                }
                
                if (profileName != "default") {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete),
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePreviewScreen(
    profileName: String,
    shortcuts: List<Shortcut>,
    onShortcutMove: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val columns = 5
    val rows = 7
    val totalSlots = columns * rows
    
    // ショートカットを35個に調整
    val paddedShortcuts = remember(shortcuts) {
        shortcuts + List(maxOf(0, totalSlots - shortcuts.size)) { Shortcut.empty() }
    }
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Header
        TopAppBar(
            title = { 
                Text(
                    stringResource(id = R.string.profile_edit_title, profileName),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0F0F1E)
            ),
            actions = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(id = R.string.cancel), color = Color.White)
                    }
                    TextButton(onClick = onSave) {
                        Text(stringResource(id = R.string.save), color = Color(0xFF4CAF50))
                    }
                }
            }
        )
        
        // Instructions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (selectedIndex == null) {
                        stringResource(id = R.string.profile_edit_hint)
                    } else {
                        stringResource(id = R.string.profile_edit_selecting, selectedIndex!! + 1)
                    },
                    color = Color.White,
                    fontSize = 14.sp
                )
                if (selectedIndex != null) {
                    Text(
                        text = stringResource(id = R.string.profile_edit_cancel_hint),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(paddedShortcuts) { index, shortcut ->
                SelectableShortcutButton(
                    shortcut = shortcut,
                    index = index,
                    isSelected = selectedIndex == index,
                    onLongPress = { 
                        selectedIndex = if (selectedIndex == index) null else index
                    },
                    onClick = { 
                        selectedIndex?.let { fromIndex ->
                            if (fromIndex != index) {
                                onShortcutMove(fromIndex, index)
                                selectedIndex = null
                            } else {
                                selectedIndex = null
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SelectableShortcutButton(
    shortcut: Shortcut,
    index: Int,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = if (shortcut.isEmpty) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1A1A2E), Color(0xFF1A1A2E))
                    )
                } else {
                    when (shortcut.category) {
                        ShortcutCategory.COPY_PASTE -> Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A5F), Color(0xFF16304D))
                        )
                        ShortcutCategory.EDIT -> Brush.linearGradient(
                            colors = listOf(Color(0xFF3D2F5B), Color(0xFF312449))
                        )
                        ShortcutCategory.NAVIGATION -> Brush.linearGradient(
                            colors = listOf(Color(0xFF2A4A3E), Color(0xFF1E3A2E))
                        )
                        ShortcutCategory.CUSTOM -> Brush.linearGradient(
                            colors = listOf(Color(0xFF2A2A3E), Color(0xFF23232E))
                        )
                    }
                }
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFF4CAF50) else 
                       if (shortcut.isEmpty) Color(0xFF2A2A3E).copy(alpha = 0.3f) 
                       else Color(0xFF2A2A3E).copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(shortcut, index) {
                detectTapGestures(
                    onLongPress = {
                        // 空のスロットも選択可能
                        onLongPress()
                    },
                    onTap = {
                        onClick()
                    }
                )
            }
            .zIndex(if (isSelected) 1f else 0f),
        contentAlignment = Alignment.Center
    ) {
        if (!shortcut.isEmpty) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = shortcut.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (shortcut.description.isNotEmpty()) {
                    Text(
                        text = shortcut.description,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 8.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else if (isSelected) {
            // 空のスロットが選択された場合の表示
            Text(
                text = stringResource(id = R.string.empty_slot),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        
        // Position indicator
        Text(
            text = "${index + 1}",
            color = Color(0xFF666666),
            fontSize = 8.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
        )
        
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(
                        Color(0xFF4CAF50),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.check_mark),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ProfileSaveDialog(
    newProfileName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.profile_save_title)) },
        text = {
            Column {
                Text(stringResource(id = R.string.profile_name_prompt))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = onNameChange,
                    placeholder = { Text(stringResource(id = R.string.profile_name_placeholder)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(stringResource(id = R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

// ファイル名を取得する関数
fun getFileName(context: Context, uri: Uri): String? {
    return when (uri.scheme) {
        "content" -> {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) cursor.getString(nameIndex) else null
                } else null
            }
        }
        "file" -> {
            uri.lastPathSegment
        }
        else -> null
    }
}
