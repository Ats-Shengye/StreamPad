package com.streampad.bt.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.streampad.bt.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepAliveScreen(
    onNavigateBack: () -> Unit,
    onSendKeepAlive: () -> Unit
) {
    var currentTime by remember { mutableStateOf(getCurrentTime()) }
    var lastSentTime by remember { mutableStateOf<String?>(null) }
    var nextSendTime by remember { mutableStateOf(getNextSendTime()) }
    val intervalMinutes = 5 // 5分間隔
    
    // 点滅アニメーション
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // 時計更新
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = getCurrentTime()
        }
    }
    
    // Keep Alive送信
    LaunchedEffect(Unit) {
        while (true) {
            delay(intervalMinutes * 60 * 1000L)
            onSendKeepAlive()
            lastSentTime = getCurrentTime()
            nextSendTime = getNextSendTime(intervalMinutes)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1E))
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    stringResource(id = R.string.keep_alive_mode),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1A1A2E)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Status Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Color(0xFF4CAF50).copy(alpha = alpha),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.active),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = stringResource(id = R.string.seat_on),
                    color = Color(0xFF4CAF50),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Time Display
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E2E)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Current Time
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.current_time),
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = currentTime,
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Divider(color = Color(0xFF2A2A3E))
                    
                    // Last Sent Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.last_send),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = lastSentTime ?: stringResource(id = R.string.no_send),
                                color = Color(0xFF64B5F6),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.next),
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Text(
                                text = nextSendTime,
                                color = Color(0xFFFF9800),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A3E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Text(
                            text = stringResource(id = R.string.mode_auto),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF64B5F6), CircleShape)
                        )
                        Text(
                            text = stringResource(id = R.string.interval_minutes, intervalMinutes),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF9800), CircleShape)
                        )
                        Text(
                            text = stringResource(id = R.string.send_keys_scrolllock),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Instruction Text
            Text(
                text = stringResource(id = R.string.keep_alive_activate),
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

private fun getCurrentTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

private fun getNextSendTime(intervalMinutes: Int = 5): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, intervalMinutes)
    return sdf.format(calendar.time)
}
