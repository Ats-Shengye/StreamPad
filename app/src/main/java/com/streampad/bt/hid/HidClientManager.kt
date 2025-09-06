package com.streampad.bt.hid

import android.content.Context
import android.widget.Toast
import com.streampad.bt.model.ConnectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HidClientManager(private val appContext: Context) {
    private val btClient = BluetoothHidClient()
    private val usbClient = UsbHidClient()
    private val dummyClient = DummyHidClient()

    private var current: IHidClient = dummyClient
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    fun switchTo(mode: ConnectionMode): Boolean {
        val target = when (mode) {
            ConnectionMode.BLUETOOTH -> btClient
            ConnectionMode.USB -> usbClient
        }
        if (!target.isSupported(appContext)) {
            Toast.makeText(appContext, unsupportedMessage(mode), Toast.LENGTH_LONG).show()
            return false
        }
        if (current !== target) {
            current.stop(appContext)
            current = target
            current.start(appContext)
            _isReady.value = current.isReady.value
        }
        return true
    }

    fun stop() {
        current.stop(appContext)
        _isReady.value = false
        current = dummyClient
    }

    fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        current.sendKeyPress(modifier, keyCode)
    }

    fun isBluetoothSupported(): Boolean = btClient.isSupported(appContext)
    fun isUsbSupported(): Boolean = usbClient.isSupported(appContext)
    fun isUsbConfigFsAvailable(): Boolean {
        val paths = listOf("/config/usb_gadget", "/sys/kernel/config/usb_gadget")
        return paths.any { java.io.File(it).exists() }
    }

    private fun unsupportedMessage(mode: ConnectionMode): String = when (mode) {
        ConnectionMode.BLUETOOTH -> "この端末ではBluetooth HIDが利用できない可能性があるよ"
        ConnectionMode.USB -> "USB HIDはroot端末のみ対応だよ"
    }
}
