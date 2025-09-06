package com.streampad.bt.hid

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface IHidClient {
    val isReady: StateFlow<Boolean>
    val name: String

    fun start(context: Context)
    fun stop(context: Context)
    fun sendKeyPress(modifier: Byte, keyCode: Byte)
    fun isSupported(context: Context): Boolean
}

