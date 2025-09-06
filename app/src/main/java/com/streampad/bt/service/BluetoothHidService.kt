package com.streampad.bt.service

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.streampad.bt.MainActivity
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BluetoothHidService : Service() {
    
    companion object {
        private const val TAG = "BluetoothHidService"
        private const val CHANNEL_ID = "StreamPadService"
        private const val NOTIFICATION_ID = 1
        
        // HID Report Descriptor for keyboard
        private val HID_REPORT_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(),  // Usage (Keyboard)
            0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
            
            // Modifier keys
            0x05.toByte(), 0x07.toByte(),  // Usage Page (Key Codes)
            0x19.toByte(), 0xE0.toByte(),  // Usage Minimum (224)
            0x29.toByte(), 0xE7.toByte(),  // Usage Maximum (231)
            0x15.toByte(), 0x00.toByte(),  // Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(),  // Logical Maximum (1)
            0x75.toByte(), 0x01.toByte(),  // Report Size (1)
            0x95.toByte(), 0x08.toByte(),  // Report Count (8)
            0x81.toByte(), 0x02.toByte(),  // Input (Data, Variable, Absolute)
            
            // Reserved byte
            0x95.toByte(), 0x01.toByte(),  // Report Count (1)
            0x75.toByte(), 0x08.toByte(),  // Report Size (8)
            0x81.toByte(), 0x01.toByte(),  // Input (Constant)
            
            // Key array (6 keys)
            0x95.toByte(), 0x06.toByte(),  // Report Count (6)
            0x75.toByte(), 0x08.toByte(),  // Report Size (8)
            0x15.toByte(), 0x00.toByte(),  // Logical Minimum (0)
            0x25.toByte(), 0x65.toByte(),  // Logical Maximum (101)
            0x05.toByte(), 0x07.toByte(),  // Usage Page (Key Codes)
            0x19.toByte(), 0x00.toByte(),  // Usage Minimum (0)
            0x29.toByte(), 0x65.toByte(),  // Usage Maximum (101)
            0x81.toByte(), 0x00.toByte(),  // Input (Data, Array)
            
            0xC0.toByte()  // End Collection
        )
    }
    
    private val binder = LocalBinder()
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothHidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isHidRegistered = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val keyChannel: Channel<Pair<Byte, Byte>> = Channel(Channel.BUFFERED)
    private var isInForeground = false
    private var demoteJob: kotlinx.coroutines.Job? = null
    
    private val executor = Executor { command -> command.run() }
    
    private val hidDeviceCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.d(TAG, "App status changed: registered=$registered")
            isHidRegistered = registered
            if (registered && pluggedDevice != null) {
                connectedDevice = pluggedDevice
                Log.d(TAG, "Connected to device: ${pluggedDevice.name}")
            }
        }
        
        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            Log.d(TAG, "Connection state changed: state=$state")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDevice = device
                    Log.d(TAG, "Device connected: ${device?.name}")
                    ensureForeground()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedDevice = null
                    Log.d(TAG, "Device disconnected")
                    scheduleDemote()
                }
            }
        }
    }
    
    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = proxy as BluetoothHidDevice
                registerHidApplication()
            }
        }
        
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null
                isHidRegistered = false
            }
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothHidService = this@BluetoothHidService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeBluetooth()
        scope.launch {
            for ((modifier, keyCode) in keyChannel) {
                try {
                    ensureForeground()
                    sendKeySequence(modifier, keyCode)
                    scheduleDemote()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in key sequence", e)
                }
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StreamPad Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "StreamPad Bluetooth HID Service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StreamPad")
            .setContentText("Bluetooth HID Service Active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        
        if (bluetoothAdapter?.isEnabled == true) {
            bluetoothAdapter?.getProfileProxy(this, serviceListener, BluetoothProfile.HID_DEVICE)
        }
    }
    
    private fun registerHidApplication() {
        val sdpSettings = BluetoothHidDeviceAppSdpSettings(
            "StreamPad",
            "Bluetooth Shortcut Pad",
            "StreamPad",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            HID_REPORT_DESCRIPTOR
        )
        
        val qosOut = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
        
        bluetoothHidDevice?.registerApp(
            sdpSettings,
            null,
            qosOut,
            executor,
            hidDeviceCallback
        )
    }
    
    fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        if (!isHidRegistered || connectedDevice == null || bluetoothHidDevice == null) {
            Log.w(TAG, "Cannot queue key: not connected")
            return
        }
        keyChannel.trySend(modifier to keyCode)
    }

    private suspend fun sendKeySequence(modifier: Byte, keyCode: Byte) {
        val device = connectedDevice ?: return
        val hid = bluetoothHidDevice ?: return
        // Key down
        val pressReport = byteArrayOf(
            modifier,
            0x00,
            keyCode,
            0x00, 0x00, 0x00, 0x00, 0x00
        )
        hid.sendReport(device, 0, pressReport)
        // Delay to ensure host registers press
        delay(100)
        // Key up
        val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        hid.sendReport(device, 0, releaseReport)
        // Throttle next key
        delay(50)
        Log.d(TAG, "Sent key: m=$modifier k=$keyCode")
    }

    private fun ensureForeground() {
        if (!isInForeground) {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            isInForeground = true
        }
        demoteJob?.cancel()
    }

    private fun scheduleDemote(timeoutMs: Long = 2000L) {
        demoteJob?.cancel()
        demoteJob = scope.launch {
            delay(timeoutMs)
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) { }
            isInForeground = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isHidRegistered) {
            bluetoothHidDevice?.unregisterApp()
        }
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, bluetoothHidDevice)
        scope.cancel()
    }
}
