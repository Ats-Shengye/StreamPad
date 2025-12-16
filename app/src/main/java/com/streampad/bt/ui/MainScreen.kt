package com.streampad.bt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.streampad.bt.R
import com.streampad.bt.model.ConnectionMode
import com.streampad.bt.model.Settings
import com.streampad.bt.model.Shortcut
import com.streampad.bt.model.ShortcutCategory
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isServiceConnected: Boolean,
    settings: Settings,
    onConnectionModeChange: (ConnectionMode) -> Unit,
    onShortcutClick: (Shortcut) -> Unit,
    onShortcutLongPress: (Shortcut) -> Unit,
    onShortcutRelease: () -> Unit,
    onKeyPressVibrationChange: (Boolean) -> Unit,
    onPageChangeVibrationChange: (Boolean) -> Unit,
    onVisualFeedbackChange: (Boolean) -> Unit,
    onSilentModeChange: (Boolean) -> Unit,
    onNavigateToProfileManagement: () -> Unit,
    onNavigateToKeepAlive: () -> Unit,
    bluetoothSupported: Boolean = true,
    usbSupported: Boolean = true,
    usbConfigfsAvailable: Boolean = false
) {
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    
    // Nexus 7用に5x7固定配列
    val columns = 5
    val rows = 7
    val totalSlots = rows * columns  // 35個のスロット
    
    // ショートカットを35個に調整
    val paddedShortcuts = remember(shortcuts) {
        shortcuts + List(max(0, totalSlots - shortcuts.size)) { Shortcut.empty() }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Column {
            // Header
            TopAppBar(
                title = { 
                    Text(
                        stringResource(id = R.string.app_name),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F1E)
                ),
                navigationIcon = {
                    IconButton(
                        onClick = { showSettings = !showSettings }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.settings),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection status indicator
                        val statusDescription = if (isServiceConnected) {
                            stringResource(id = R.string.status_connected)
                        } else {
                            stringResource(id = R.string.status_disconnected)
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .semantics {
                                    contentDescription = statusDescription
                                }
                                .background(
                                    color = if (isServiceConnected) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceConnected) stringResource(id = R.string.status_connected) else stringResource(id = R.string.status_disconnected),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            )
            
            // Shortcut Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(paddedShortcuts) { shortcut ->
                    ShortcutButton(
                        shortcut = shortcut,
                        visualFeedback = settings.visualFeedback,
                        onClick = { onShortcutClick(shortcut) },
                        onLongPress = { onShortcutLongPress(shortcut) },
                        onRelease = onShortcutRelease
                    )
                }
            }
        }
        
        // Settings overlay
        if (showSettings) {
            SettingsMenu(
                settings = settings,
                connectionMode = settings.connectionMode,
                onDismiss = { showSettings = false },
                onConnectionModeChange = onConnectionModeChange,
                onKeyPressVibrationChange = onKeyPressVibrationChange,
                onPageChangeVibrationChange = onPageChangeVibrationChange,
                onVisualFeedbackChange = onVisualFeedbackChange,
                onSilentModeChange = onSilentModeChange,
                bluetoothSupported = bluetoothSupported,
                usbSupported = usbSupported,
                usbConfigfsAvailable = usbConfigfsAvailable,
                onNavigateToProfileManagement = {
                    showSettings = false
                    onNavigateToProfileManagement()
                },
                onNavigateToKeepAlive = {
                    showSettings = false
                    onNavigateToKeepAlive()
                }
            )
        }
    }
}

@Composable
fun SettingsMenu(
    settings: Settings,
    connectionMode: ConnectionMode,
    onDismiss: () -> Unit,
    onConnectionModeChange: (ConnectionMode) -> Unit,
    onKeyPressVibrationChange: (Boolean) -> Unit,
    onPageChangeVibrationChange: (Boolean) -> Unit,
    onVisualFeedbackChange: (Boolean) -> Unit,
    onSilentModeChange: (Boolean) -> Unit,
    bluetoothSupported: Boolean,
    usbSupported: Boolean,
    usbConfigfsAvailable: Boolean,
    onNavigateToProfileManagement: () -> Unit,
    onNavigateToKeepAlive: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() },
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Card(
                modifier = Modifier.width(250.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.settings),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Connection mode selector
                    Text(
                        text = stringResource(id = R.string.connection_mode),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = connectionMode == ConnectionMode.BLUETOOTH,
                            onClick = { onConnectionModeChange(ConnectionMode.BLUETOOTH) },
                            enabled = bluetoothSupported,
                            label = { Text(stringResource(id = R.string.mode_bluetooth)) }
                        )
                    }

                    SettingItem(
                        title = stringResource(id = R.string.keypress_vibration),
                        isEnabled = settings.keyPressVibration,
                        onToggle = { onKeyPressVibrationChange(!settings.keyPressVibration) }
                    )
                    
                    SettingItem(
                        title = stringResource(id = R.string.pagechange_vibration),
                        isEnabled = settings.pageChangeVibration,
                        onToggle = { onPageChangeVibrationChange(!settings.pageChangeVibration) }
                    )
                    
                    SettingItem(
                        title = stringResource(id = R.string.visual_feedback),
                        isEnabled = settings.visualFeedback,
                        onToggle = { onVisualFeedbackChange(!settings.visualFeedback) }
                    )
                    
                    SettingItem(
                        title = stringResource(id = R.string.silent_mode),
                        isEnabled = settings.silentMode,
                        onToggle = { onSilentModeChange(!settings.silentMode) }
                    )
                    
                    Divider(
                        color = Color(0xFF2A2A3E),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Compatibility
                    Text(
                        text = stringResource(id = R.string.compatibility),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (bluetoothSupported) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(
                                text = if (bluetoothSupported) stringResource(id = R.string.bt_available) else stringResource(id = R.string.bt_unavailable),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (usbSupported) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(
                                text = if (usbSupported) stringResource(id = R.string.usb_root_ok) else stringResource(id = R.string.usb_root_ng),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (usbConfigfsAvailable) Color(0xFF4CAF50) else Color(0xFFFFC107),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Text(
                                text = if (usbConfigfsAvailable) stringResource(id = R.string.usb_configfs_ok) else stringResource(id = R.string.usb_configfs_unknown),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.compatibility_notes),
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    
                    Divider(
                        color = Color(0xFF2A2A3E),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Profile Management Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProfileManagement() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.profile_management),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = stringResource(id = R.string.profile_management),
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Divider(
                        color = Color(0xFF2A2A3E),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    // Keep Alive Mode Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToKeepAlive() }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.keep_alive_mode),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = stringResource(id = R.string.keep_alive_desc),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp, 24.dp)
                                .background(
                                    Color(0xFF4CAF50),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.start),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp
        )
        
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF64B5F6),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF2A2A3E)
            )
        )
    }
}

@Composable
fun ShortcutButton(
    shortcut: Shortcut,
    visualFeedback: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit
) {
    if (shortcut.isEmpty) {
        // Empty slot
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    color = Color(0xFF2A2A3E).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Color(0xFF1A1A2E).copy(alpha = 0.5f))
        )
    } else {
        var isPressed by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = when (shortcut.category) {
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
                )
                .border(
                    width = 1.dp,
                    color = if (isPressed) Color(0xFF64B5F6) else Color(0xFF2A2A3E).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            // Send single tap immediately
                            onClick()
                            // Start long press timer
                            onLongPress()
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                                onRelease()
                            }
                        }
                    )
                }
                .semantics {
                    contentDescription = buildString {
                        append(shortcut.label)
                        if (shortcut.description.isNotEmpty()) {
                            append(" ")
                            append(shortcut.description)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = shortcut.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (shortcut.description.isNotEmpty()) {
                    Text(
                        text = shortcut.description,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
