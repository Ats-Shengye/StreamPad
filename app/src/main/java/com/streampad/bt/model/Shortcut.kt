package com.streampad.bt.model

data class Shortcut(
    val label: String,
    val description: String = "",
    val modifier: Byte = 0,
    val keyCode: Byte = 0,
    val category: ShortcutCategory = ShortcutCategory.CUSTOM,
    val isEmpty: Boolean = false
) {
    companion object {
        fun empty() = Shortcut("", "", isEmpty = true)
        
        // Modifier keys
        const val MOD_NONE: Byte = 0x00
        const val MOD_CTRL: Byte = 0x01
        const val MOD_SHIFT: Byte = 0x02
        const val MOD_ALT: Byte = 0x04
        const val MOD_WIN: Byte = 0x08
        
        // Common key codes (HID Usage IDs)
        const val KEY_A: Byte = 0x04
        const val KEY_B: Byte = 0x05
        const val KEY_C: Byte = 0x06
        const val KEY_D: Byte = 0x07
        const val KEY_E: Byte = 0x08
        const val KEY_F: Byte = 0x09
        const val KEY_G: Byte = 0x0A
        const val KEY_H: Byte = 0x0B
        const val KEY_I: Byte = 0x0C
        const val KEY_J: Byte = 0x0D
        const val KEY_K: Byte = 0x0E
        const val KEY_L: Byte = 0x0F
        const val KEY_M: Byte = 0x10
        const val KEY_N: Byte = 0x11
        const val KEY_O: Byte = 0x12
        const val KEY_P: Byte = 0x13
        const val KEY_Q: Byte = 0x14
        const val KEY_R: Byte = 0x15
        const val KEY_S: Byte = 0x16
        const val KEY_T: Byte = 0x17
        const val KEY_U: Byte = 0x18
        const val KEY_V: Byte = 0x19
        const val KEY_W: Byte = 0x1A
        const val KEY_X: Byte = 0x1B
        const val KEY_Y: Byte = 0x1C
        const val KEY_Z: Byte = 0x1D
        
        const val KEY_ENTER: Byte = 0x28
        const val KEY_ESC: Byte = 0x29
        const val KEY_BACKSPACE: Byte = 0x2A
        const val KEY_TAB: Byte = 0x2B
        const val KEY_SPACE: Byte = 0x2C
        const val KEY_DELETE: Byte = 0x4C
        
        const val KEY_F1: Byte = 0x3A
        const val KEY_F2: Byte = 0x3B
        const val KEY_F3: Byte = 0x3C
        const val KEY_F4: Byte = 0x3D
        const val KEY_F5: Byte = 0x3E
        const val KEY_F6: Byte = 0x3F
        const val KEY_F7: Byte = 0x40
        const val KEY_F8: Byte = 0x41
        const val KEY_F9: Byte = 0x42
        const val KEY_F10: Byte = 0x43
        const val KEY_F11: Byte = 0x44
        const val KEY_F12: Byte = 0x45
        
        const val KEY_RIGHT: Byte = 0x4F
        const val KEY_LEFT: Byte = 0x50
        const val KEY_DOWN: Byte = 0x51
        const val KEY_UP: Byte = 0x52
        
        const val KEY_HOME: Byte = 0x4A
        const val KEY_END: Byte = 0x4D
        const val KEY_PAGE_UP: Byte = 0x4B
        const val KEY_PAGE_DOWN: Byte = 0x4E
    }
}

enum class ShortcutCategory {
    COPY_PASTE,
    EDIT,
    NAVIGATION,
    CUSTOM
}