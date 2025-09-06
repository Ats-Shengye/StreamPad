package com.streampad.bt.utils

import android.content.Context
import com.streampad.bt.model.Profile
import com.streampad.bt.model.Shortcut
import com.streampad.bt.model.ShortcutCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class SerializableShortcut(
    val label: String,
    val description: String,
    val modifier: Byte,
    val keyCode: Byte,
    val category: String,
    val isEmpty: Boolean
)

@Serializable
data class SerializableProfile(
    val name: String,
    val shortcuts: List<SerializableShortcut>
)

class ProfileManager(private val context: Context) {
    
    private val profilesDir = File(context.filesDir, "profiles")
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val _availableProfiles = MutableStateFlow<List<String>>(emptyList())
    val availableProfiles: StateFlow<List<String>> = _availableProfiles
    
    private val _currentProfile = MutableStateFlow<String>("default")
    val currentProfile: StateFlow<String> = _currentProfile
    
    init {
        if (!profilesDir.exists()) {
            profilesDir.mkdirs()
        }
        loadAvailableProfiles()
    }
    
    fun loadAvailableProfiles() {
        val profiles = profilesDir.listFiles()?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension } ?: emptyList()
        _availableProfiles.value = profiles
    }
    
    fun saveProfile(name: String, shortcuts: List<Shortcut>) {
        val serializableShortcuts = shortcuts.map { shortcut ->
            SerializableShortcut(
                label = shortcut.label,
                description = shortcut.description,
                modifier = shortcut.modifier,
                keyCode = shortcut.keyCode,
                category = shortcut.category.name,
                isEmpty = shortcut.isEmpty
            )
        }
        
        val profile = SerializableProfile(name, serializableShortcuts)
        val profileFile = File(profilesDir, "$name.json")
        
        try {
            profileFile.writeText(json.encodeToString(profile))
            loadAvailableProfiles()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun loadProfile(name: String): List<Shortcut>? {
        val profileFile = File(profilesDir, "$name.json")
        if (!profileFile.exists()) return null
        
        return try {
            val profileContent = profileFile.readText()
            val profile = json.decodeFromString<SerializableProfile>(profileContent)
            
            profile.shortcuts.map { serializable ->
                Shortcut(
                    label = serializable.label,
                    description = serializable.description,
                    modifier = serializable.modifier,
                    keyCode = serializable.keyCode,
                    category = ShortcutCategory.valueOf(serializable.category),
                    isEmpty = serializable.isEmpty
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun deleteProfile(name: String) {
        if (name == "default") return // Prevent deleting default profile
        
        val profileFile = File(profilesDir, "$name.json")
        if (profileFile.exists()) {
            profileFile.delete()
            loadAvailableProfiles()
        }
    }
    
    fun importProfile(jsonContent: String, profileName: String): Boolean {
        return try {
            val profile = json.decodeFromString<SerializableProfile>(jsonContent)
            val updatedProfile = profile.copy(name = profileName)
            
            val profileFile = File(profilesDir, "$profileName.json")
            profileFile.writeText(json.encodeToString(updatedProfile))
            loadAvailableProfiles()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun setCurrentProfile(name: String) {
        _currentProfile.value = name
    }
}