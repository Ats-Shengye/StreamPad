package com.streampad.bt.hid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.streampad.bt.service.UsbHidService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

class UsbHidClient : IHidClient {
    override val name: String = "USB"

    private var service: UsbHidService? = null
    private var bound = false
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val local = binder as? UsbHidService.LocalBinder
            service = local?.getService()
            bound = service != null
            _isReady.value = bound
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
            _isReady.value = false
        }
    }

    override fun start(context: Context) {
        val intent = Intent(context, UsbHidService::class.java)
        // Start as background started service (app is in foreground when called)
        context.startService(intent)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun stop(context: Context) {
        if (bound) {
            context.unbindService(connection)
            bound = false
            _isReady.value = false
        }
        context.stopService(Intent(context, UsbHidService::class.java))
    }

    override fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        service?.sendKeyPress(modifier, keyCode)
    }

    override fun isSupported(context: Context): Boolean {
        // Quick and conservative root check; avoids launching UI if definitely not available
        return isRootAvailable()
    }

    private fun isRootAvailable(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            proc.waitFor()
            proc.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }
}
