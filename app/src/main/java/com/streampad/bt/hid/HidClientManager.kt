package com.streampad.bt.hid

import android.content.Context
import com.streampad.bt.model.ConnectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * HidClientManager - HIDクライアントの管理クラス
 *
 * # 役割
 * - 接続モード (Bluetooth) の切り替え
 * - 各クライアントの生存期間管理
 * - キー送信の委譲
 *
 * # 実装方針
 * - USB HIDは削除 (将来的に再実装の可能性あり)
 * - switchTo()はHidResultを返却
 * - ConnectionMode.BLUETOOTHのみサポート
 */
class HidClientManager(private val appContext: Context) {
    private val btClient = BluetoothHidClient()
    private val dummyClient = DummyHidClient()

    private var current: IHidClient = dummyClient
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    /**
     * 指定されたモードに切り替える
     *
     * @param mode 接続モード (BLUETOOTH のみ対応)
     * @return 切り替え結果 (HidResult)
     */
    fun switchTo(mode: ConnectionMode): HidResult {
        return when (mode) {
            ConnectionMode.BLUETOOTH -> {
                if (!btClient.isSupported(appContext)) {
                    HidResult.Failure.Unsupported("Bluetooth HID is not supported on this device")
                } else {
                    if (current !== btClient) {
                        current.stop(appContext)
                        current = btClient
                        current.start(appContext)
                        _isReady.value = current.isReady.value
                    }
                    HidResult.Success
                }
            }
        }
    }

    /**
     * 現在のクライアントを停止
     */
    fun stop() {
        current.stop(appContext)
        _isReady.value = false
        current = dummyClient
    }

    /**
     * キー送信 (現在のクライアントに委譲)
     */
    fun sendKeyPress(modifier: Byte, keyCode: Byte) {
        current.sendKeyPress(modifier, keyCode)
    }

    /**
     * Bluetooth HIDがサポートされているか
     */
    fun isBluetoothSupported(): Boolean = btClient.isSupported(appContext)

    /**
     * USB HIDがサポートされているか (常にfalse)
     */
    fun isUsbSupported(): Boolean = false

    /**
     * USB ConfigFSが利用可能か (常にfalse)
     */
    fun isUsbConfigFsAvailable(): Boolean = false
}
