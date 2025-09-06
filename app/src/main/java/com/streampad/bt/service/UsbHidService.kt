package com.streampad.bt.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.streampad.bt.MainActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UsbHidService : Service() {

    private val binder = LocalBinder()
    private var isUsbHidInitialized = false
    private var hidDevicePath: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val keyChannel: Channel<Pair<Byte, Byte>> = Channel(Channel.BUFFERED)
    private var isInForeground = false
    private var demoteJob: kotlinx.coroutines.Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): UsbHidService = this@UsbHidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeUsbHid()
        scope.launch {
            for ((modifier, keyCode) in keyChannel) {
                try {
                    ensureForeground()
                    sendHidSequence(modifier, keyCode)
                    scheduleDemote()
                } catch (e: Exception) {
                    Log.e(TAG, "USB key sequence error", e)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        if (!isUsbHidInitialized) {
            Log.w(TAG, "USB HID not initialized, attempting fallback")
            return
        }
        keyChannel.trySend(modifier to keyCode)
    }
    
    private fun initializeUsbHid() {
        try {
            // Try to initialize USB HID gadget (requires root)
            if (setupUsbGadget()) {
                isUsbHidInitialized = true
                Log.i(TAG, "USB HID initialized successfully")
            } else {
                Log.w(TAG, "USB HID initialization failed - device may not support HID gadget or lacks root access")
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB HID initialization error: ${e.message}")
        }
    }
    
    private fun setupUsbGadget(): Boolean {
        val gadgetPaths = listOf(
            "/config/usb_gadget/g1",
            "/sys/kernel/config/usb_gadget/g1"
        )
        
        for (basePath in gadgetPaths) {
            val gadgetDir = File(basePath)
            if (!gadgetDir.exists()) continue
            
            try {
                // Create HID function
                val hidDir = File("$basePath/functions/hid.usb0")
                if (!hidDir.exists()) {
                    executeRootCommand("mkdir -p ${hidDir.absolutePath}")
                }
                
                // Set HID descriptor (standard keyboard)
                val reportDesc = getKeyboardHidDescriptor()
                writeHidDescriptor("$basePath/functions/hid.usb0/report_desc", reportDesc)
                writeToFile("$basePath/functions/hid.usb0/report_length", "8")
                writeToFile("$basePath/functions/hid.usb0/protocol", "1")
                writeToFile("$basePath/functions/hid.usb0/subclass", "1")
                
                // Link function to configuration
                val configDir = "$basePath/configs/b.1"
                executeRootCommand("mkdir -p $configDir")
                executeRootCommand("ln -sf $basePath/functions/hid.usb0 $configDir/")
                
                // Enable gadget
                val udcFile = File("$basePath/UDC")
                if (udcFile.exists()) {
                    val udcController = getUdcController()
                    if (udcController != null) {
                        writeToFile("$basePath/UDC", udcController)
                        hidDevicePath = "/dev/hidg0"
                        return File(hidDevicePath!!).exists()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to setup USB gadget at $basePath: ${e.message}")
                continue
            }
        }
        return false
    }
    
    private fun sendHidReport(modifier: Byte, keyCode: Byte) {
        hidDevicePath?.let { path ->
            val report = byteArrayOf(
                modifier, // Modifier keys
                0,        // Reserved
                keyCode,  // Key code
                0, 0, 0, 0, 0 // Additional keys (unused)
            )
            
            try {
                FileOutputStream(path).use { fos ->
                    fos.write(report)
                    fos.flush()
                }
                
                // Send key release immediately
                val releaseReport = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                FileOutputStream(path).use { fos ->
                    fos.write(releaseReport)
                    fos.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write HID report: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun sendHidSequence(modifier: Byte, keyCode: Byte) {
        // Press
        sendHidReport(modifier, keyCode)
        delay(100)
        // Release
        sendHidReport(0, 0)
        delay(50)
        Log.d(TAG, "USB key: m=$modifier k=$keyCode")
    }

    private fun ensureForeground() {
        if (!isInForeground) {
            startForeground(NOTIFICATION_ID, createNotification())
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
    
    private fun getKeyboardHidDescriptor(): ByteArray {
        // Standard USB HID keyboard descriptor
        return byteArrayOf(
            0x05, 0x01,          // Usage Page (Generic Desktop)
            0x09, 0x06,          // Usage (Keyboard)
            0xa1.toByte(), 0x01, // Collection (Application)
            0x05, 0x07,          // Usage Page (Keyboard/Keypad)
            0x19, 0xe0.toByte(), // Usage Minimum (224)
            0x29, 0xe7.toByte(), // Usage Maximum (231)
            0x15, 0x00,          // Logical Minimum (0)
            0x25, 0x01,          // Logical Maximum (1)
            0x75, 0x01,          // Report Size (1)
            0x95.toByte(), 0x08, // Report Count (8)
            0x81.toByte(), 0x02, // Input (Data, Variable, Absolute)
            0x95.toByte(), 0x01, // Report Count (1)
            0x75, 0x08,          // Report Size (8)
            0x81.toByte(), 0x03, // Input (Constant, Variable, Absolute)
            0x95.toByte(), 0x06, // Report Count (6)
            0x75, 0x08,          // Report Size (8)
            0x15, 0x00,          // Logical Minimum (0)
            0x25, 0x65,          // Logical Maximum (101)
            0x05, 0x07,          // Usage Page (Keyboard/Keypad)
            0x19, 0x00,          // Usage Minimum (0)
            0x29, 0x65,          // Usage Maximum (101)
            0x81.toByte(), 0x00, // Input (Data, Array, Absolute)
            0xc0.toByte()        // End Collection
        )
    }
    
    private fun executeRootCommand(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Root command failed: $command - ${e.message}")
            false
        }
    }
    
    private fun writeToFile(filePath: String, content: String): Boolean {
        return try {
            executeRootCommand("echo '$content' > $filePath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to file $filePath: ${e.message}")
            false
        }
    }
    
    private fun writeHidDescriptor(filePath: String, descriptor: ByteArray): Boolean {
        return try {
            File(filePath).outputStream().use { output ->
                output.write(descriptor)
                output.flush()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write HID descriptor to $filePath: ${e.message}")
            // Fallback: try with root command
            try {
                val tempFile = File.createTempFile("hid_desc", ".bin")
                tempFile.outputStream().use { output ->
                    output.write(descriptor)
                }
                executeRootCommand("cp ${tempFile.absolutePath} $filePath")
                tempFile.delete()
                true
            } catch (fallbackException: Exception) {
                Log.e(TAG, "Fallback write also failed: ${fallbackException.message}")
                false
            }
        }
    }
    
    private fun getUdcController(): String? {
        return try {
            val udcDir = File("/sys/class/udc")
            if (udcDir.exists()) {
                udcDir.listFiles()?.firstOrNull()?.name
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get UDC controller: ${e.message}")
            null
        }
    }
    
    fun isUsbHidAvailable(): Boolean = isUsbHidInitialized

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StreamPad USB Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "StreamPad USB Service" }
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
            .setContentText("USB Service Active (stub)")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val TAG = "UsbHidService"
        private const val CHANNEL_ID = "StreamPadUsbServiceChannel"
        private const val NOTIFICATION_ID = 2002
    }
}
