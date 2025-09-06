package com.streampad.bt.hid

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DummyHidClient : IHidClient {
    override val name: String = "Dummy"
    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady

    override fun start(context: Context) { /* no-op */ }
    override fun stop(context: Context) { /* no-op */ }

    override fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        Log.d("DummyHidClient", "sendKeyPress m=$modifier k=$keyCode")
    }

    override fun isSupported(context: Context): Boolean = true
}

