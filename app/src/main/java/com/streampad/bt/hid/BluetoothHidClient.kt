package com.streampad.bt.hid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import com.streampad.bt.service.BluetoothHidService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BluetoothHidClient : IHidClient {
    override val name: String = "Bluetooth"

    private var service: BluetoothHidService? = null
    private var bound = false
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val local = binder as? BluetoothHidService.LocalBinder
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
        val intent = Intent(context, BluetoothHidService::class.java)
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
        context.stopService(Intent(context, BluetoothHidService::class.java))
    }

    override fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        service?.sendKeyPress(modifier, keyCode)
    }

    override fun isSupported(context: Context): Boolean {
        val mgr = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        val adapter: BluetoothAdapter? = mgr?.adapter
        return adapter != null
    }
}
