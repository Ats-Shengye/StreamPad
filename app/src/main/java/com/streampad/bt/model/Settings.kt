package com.streampad.bt.model

data class Settings(
    val keyPressVibration: Boolean = true,
    val pageChangeVibration: Boolean = true,
    val visualFeedback: Boolean = true,
    val silentMode: Boolean = false,
    val connectionMode: ConnectionMode = ConnectionMode.BLUETOOTH
)

enum class ConnectionMode {
    BLUETOOTH
}
