package com.streampad.bt.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streampad.bt.model.Shortcut
import com.streampad.bt.model.ShortcutCategory
import com.streampad.bt.utils.ProfileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    context: Context,
    private val profileManager: ProfileManager
) : ViewModel() {
    private val _shortcuts = MutableStateFlow(getDefaultShortcuts())
    val shortcuts: StateFlow<List<Shortcut>> = _shortcuts
    
    init {
        // デフォルトプロファイルを作成
        saveCurrentAsProfile("default")
    }
    
    fun loadProfile(profileName: String) {
        viewModelScope.launch {
            profileManager.loadProfile(profileName)?.let { shortcuts ->
                // 35個のスロットに合わせて調整（5x7）
                val adjustedShortcuts = shortcuts.take(35) + 
                    List(maxOf(0, 35 - shortcuts.size)) { Shortcut.empty() }
                _shortcuts.value = adjustedShortcuts
                profileManager.setCurrentProfile(profileName)
            }
        }
    }
    
    fun saveCurrentAsProfile(profileName: String) {
        viewModelScope.launch {
            profileManager.saveProfile(profileName, _shortcuts.value)
        }
    }
    
    fun getAvailableProfiles(): StateFlow<List<String>> = profileManager.availableProfiles
    fun getCurrentProfile(): StateFlow<String> = profileManager.currentProfile
    
    fun deleteProfile(profileName: String) {
        viewModelScope.launch {
            profileManager.deleteProfile(profileName)
        }
    }
    
    fun importProfile(jsonContent: String, profileName: String): Boolean {
        return profileManager.importProfile(jsonContent, profileName)
    }
    
    fun getProfileShortcuts(profileName: String): List<Shortcut> {
        return if (profileName == getCurrentProfile().value) {
            // 現在のプロファイルなら現在の状態を返す
            _shortcuts.value
        } else {
            // 他のプロファイルはファイルから読み込み
            profileManager.loadProfile(profileName) ?: emptyList()
        }
    }
    
    fun saveProfileWithShortcuts(profileName: String, shortcuts: List<Shortcut>) {
        viewModelScope.launch {
            profileManager.saveProfile(profileName, shortcuts)
            // 現在のプロファイルの場合は状態も更新
            if (profileName == getCurrentProfile().value) {
                _shortcuts.value = shortcuts
            }
        }
    }
    
    private fun getDefaultShortcuts(): List<Shortcut> {
        val baseShortcuts = listOf(
            // Row 1 - Reserved/Empty
            Shortcut.empty(),
            Shortcut.empty(),
            Shortcut.empty(),
            Shortcut.empty(),
            Shortcut.empty(),
            
            // Row 2 - Edit operations
            Shortcut(
                label = "Ctrl+Z",
                description = "元に戻す",
                modifier = Shortcut.MOD_CTRL,
                keyCode = Shortcut.KEY_Z,
                category = ShortcutCategory.EDIT
            ),
            Shortcut(
                label = "Ctrl+Y",
                description = "やり直し",
                modifier = Shortcut.MOD_CTRL,
                keyCode = Shortcut.KEY_Y,
                category = ShortcutCategory.EDIT
            ),
            Shortcut(
                label = "F2",
                description = "編集",
                modifier = Shortcut.MOD_NONE,
                keyCode = Shortcut.KEY_F2,
                category = ShortcutCategory.EDIT
            ),
            Shortcut.empty(),
            Shortcut.empty(),
            
            // Row 3 - Copy/Paste operations
            Shortcut(
                label = "Ctrl+C",
                description = "コピー",
                modifier = Shortcut.MOD_CTRL,
                keyCode = Shortcut.KEY_C,
                category = ShortcutCategory.COPY_PASTE
            ),
            Shortcut(
                label = "Ctrl+V",
                description = "貼付",
                modifier = Shortcut.MOD_CTRL,
                keyCode = Shortcut.KEY_V,
                category = ShortcutCategory.COPY_PASTE
            ),
            Shortcut(
                label = "Ctrl+X",
                description = "切取",
                modifier = Shortcut.MOD_CTRL,
                keyCode = Shortcut.KEY_X,
                category = ShortcutCategory.COPY_PASTE
            ),
            Shortcut(
                label = "Win+V",
                description = "履歴",
                modifier = Shortcut.MOD_WIN,
                keyCode = Shortcut.KEY_V,
                category = ShortcutCategory.COPY_PASTE
            ),
            Shortcut.empty(),
            
            // Row 4 - Navigation
            Shortcut(
                label = "←",
                description = "左",
                modifier = Shortcut.MOD_NONE,
                keyCode = Shortcut.KEY_LEFT,
                category = ShortcutCategory.NAVIGATION
            ),
            Shortcut(
                label = "↑",
                description = "上",
                modifier = Shortcut.MOD_NONE,
                keyCode = Shortcut.KEY_UP,
                category = ShortcutCategory.NAVIGATION
            ),
            Shortcut(
                label = "↓",
                description = "下",
                modifier = Shortcut.MOD_NONE,
                keyCode = Shortcut.KEY_DOWN,
                category = ShortcutCategory.NAVIGATION
            ),
            Shortcut(
                label = "→",
                description = "右",
                modifier = Shortcut.MOD_NONE,
                keyCode = Shortcut.KEY_RIGHT,
                category = ShortcutCategory.NAVIGATION
            ),
            Shortcut.empty()
        )
        
        // 35個になるまで空のスロットで埋める（5x7）
        return baseShortcuts + List(35 - baseShortcuts.size) { Shortcut.empty() }
    }
}