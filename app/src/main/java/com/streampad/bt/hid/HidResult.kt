package com.streampad.bt.hid

/**
 * HidResult - HID操作の結果を表す型
 *
 * # 目的
 * - HID接続の成功/失敗を型安全に表現
 * - エラー詳細をUI層に伝える
 *
 * # 使用例
 * ```
 * when (val result = hidManager.switchTo(ConnectionMode.BLUETOOTH)) {
 *     is HidResult.Success -> { /* 成功処理 */ }
 *     is HidResult.Failure.Unsupported -> { Toast.show(result.message) }
 * }
 * ```
 */
sealed class HidResult {
    /**
     * 操作が成功
     */
    data object Success : HidResult()

    /**
     * 操作が失敗
     */
    sealed class Failure : HidResult() {
        /**
         * サポートされていない環境
         */
        data class Unsupported(val message: String) : Failure()
    }
}
