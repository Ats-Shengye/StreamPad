package com.streampad.bt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.streampad.bt.ui.MainScreen
import com.streampad.bt.ui.MainViewModel
import com.streampad.bt.ui.ProfileManagementScreen
import com.streampad.bt.ui.KeepAliveScreen
import com.streampad.bt.ui.theme.StreamPadTheme
import com.streampad.bt.utils.SettingsManager
import com.streampad.bt.utils.ProfileManagerFactory
import com.streampad.bt.model.ConnectionMode
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.streampad.bt.hid.HidClientManager
import com.streampad.bt.hid.HidResult

// Magic Number を定数化
private const val VIBRATION_DURATION_MS = 50L
private const val LONG_PRESS_DELAY_MS = 1000L
private const val CONTINUOUS_INPUT_INTERVAL_MS = 100L
private const val SCROLL_LOCK_KEY_CODE: Byte = 0x47

class MainActivity : ComponentActivity() {

    private lateinit var hidManager: HidClientManager
    private var isServiceReady by mutableStateOf(false)
    private lateinit var vibrator: Vibrator
    private lateinit var settingsManager: SettingsManager

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            enableBluetooth()
        } else {
            Toast.makeText(this, "Bluetooth権限が必要です", Toast.LENGTH_LONG).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            switchToBluetooth()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored: service can still run, but notifications may be hidden */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        settingsManager = SettingsManager(this)
        hidManager = HidClientManager(applicationContext)
        lifecycleScope.launch {
            hidManager.isReady.collect { isServiceReady = it }
        }

        setContent {
            StreamPadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = viewModel {
                        MainViewModel(
                            applicationContext,
                            ProfileManagerFactory.create(applicationContext)
                        )
                    }
                    val settings by settingsManager.settings.collectAsStateWithLifecycle()

                    // Keep an imperative mirror of connectionMode
                    LaunchedEffect(settings.connectionMode) {
                        currentConnectionMode = settings.connectionMode
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                isServiceConnected = isServiceReady,
                                settings = settings,
                                onConnectionModeChange = { mode ->
                                    when (mode) {
                                        ConnectionMode.BLUETOOTH -> {
                                            checkPermissionsAndStartBtService()
                                            settingsManager.updateConnectionMode(mode)
                                        }
                                    }
                                },
                                onShortcutClick = { shortcut ->
                                    sendShortcut(shortcut.modifier, shortcut.keyCode)
                                    if (settings.keyPressVibration && !settings.silentMode) {
                                        vibrate()
                                    }
                                },
                                onShortcutLongPress = { shortcut ->
                                    startContinuousInput(shortcut.modifier, shortcut.keyCode)
                                },
                                onShortcutRelease = {
                                    stopContinuousInput()
                                },
                                onKeyPressVibrationChange = { enabled ->
                                    settingsManager.updateKeyPressVibration(enabled)
                                },
                                onPageChangeVibrationChange = { enabled ->
                                    settingsManager.updatePageChangeVibration(enabled)
                                },
                                onVisualFeedbackChange = { enabled ->
                                    settingsManager.updateVisualFeedback(enabled)
                                },
                                onSilentModeChange = { enabled ->
                                    settingsManager.updateSilentMode(enabled)
                                },
                                onNavigateToProfileManagement = {
                                    navController.navigate("profile_management")
                                },
                                onNavigateToKeepAlive = {
                                    navController.navigate("keep_alive")
                                },
                                bluetoothSupported = hidManager.isBluetoothSupported(),
                                usbSupported = hidManager.isUsbSupported(),
                                usbConfigfsAvailable = hidManager.isUsbConfigFsAvailable()
                            )
                        }

                        composable("profile_management") {
                            ProfileManagementScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable("keep_alive") {
                            KeepAliveScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSendKeepAlive = {
                                    // ScrollLockキーを送信 (HID Usage ID: 0x47)
                                    sendShortcut(0, SCROLL_LOCK_KEY_CODE)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Start an initial service based on saved setting
        val initialMode = settingsManager.settings.value.connectionMode
        if (initialMode == ConnectionMode.BLUETOOTH) {
            checkPermissionsAndStartBtService()
        }
    }

    private fun checkPermissionsAndStartBtService() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

        if (permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }) {
            enableBluetooth()
        } else {
            requestBluetoothPermissions.launch(permissions)
        }
    }

    private fun enableBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            switchToBluetooth()
        }
    }

    private fun switchToBluetooth() {
        requestNotificationPermissionIfNeeded()
        val result = hidManager.switchTo(ConnectionMode.BLUETOOTH)
        when (result) {
            is HidResult.Success -> {
                // Success, no action needed
            }
            is HidResult.Failure.Unsupported -> {
                Toast.makeText(applicationContext, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendShortcut(modifier: Byte, keyCode: Byte) {
        hidManager.sendKeyPress(modifier, keyCode)
    }

    private var continuousInputJob: Job? = null

    private fun startContinuousInput(modifier: Byte, keyCode: Byte) {
        stopContinuousInput() // Cancel any existing continuous input
        continuousInputJob = lifecycleScope.launch {
            // Wait before starting continuous input (prevent accidental repeat)
            delay(LONG_PRESS_DELAY_MS)
            if (isActive) {
                while (isActive) {
                    sendShortcut(modifier, keyCode)
                    delay(CONTINUOUS_INPUT_INTERVAL_MS)
                }
            }
        }
    }

    private fun stopContinuousInput() {
        continuousInputJob?.cancel()
        continuousInputJob = null
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    VIBRATION_DURATION_MS,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION_DURATION_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hidManager.stop()
    }

    // Connection mode mirror used for routing
    private var currentConnectionMode: ConnectionMode = ConnectionMode.BLUETOOTH

    fun isUsbHidAvailable(): Boolean = hidManager.isUsbSupported()

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
