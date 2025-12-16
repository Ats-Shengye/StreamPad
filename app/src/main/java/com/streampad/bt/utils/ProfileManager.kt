package com.streampad.bt.utils

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.streampad.bt.model.Profile
import com.streampad.bt.model.Shortcut
import com.streampad.bt.model.ShortcutCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "ProfileManager"
private const val PROFILE_FILE_EXTENSION = "json.enc"

/**
 * SerializableShortcut - Kotlinx.serializationで扱える形式のショートカットデータ
 */
@Serializable
data class SerializableShortcut(
    val label: String,
    val description: String,
    val modifier: Byte,
    val keyCode: Byte,
    val category: String,
    val isEmpty: Boolean
)

/**
 * SerializableProfile - Kotlinx.serializationで扱える形式のプロファイルデータ
 */
@Serializable
data class SerializableProfile(
    val name: String,
    val shortcuts: List<SerializableShortcut>
)

/**
 * ProfileManager - プロファイル管理クラス (暗号化対応版)
 *
 * # 暗号化仕様
 * - MasterKey: AES-256-GCM (Android Keystore)
 * - EncryptedFile: androidx.security.crypto (Tink)
 * - ファイル拡張子: .json.enc
 *
 * # 実装方針
 * - 全てのプロファイルファイルは暗号化して保存
 * - MasterKeyは1回だけ生成、以降は再利用
 * - エラーハンドリングは呼び出し側に委譲
 */
class ProfileManager(private val context: Context) {

    private val profilesDir = File(context.filesDir, "profiles")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // MasterKey (遅延初期化)
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
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

    /**
     * 利用可能なプロファイル一覧をディレクトリから読み込む
     */
    fun loadAvailableProfiles() {
        val profiles = profilesDir.listFiles()?.filter { it.extension == "enc" }
            ?.map { it.nameWithoutExtension.removeSuffix(".json") } ?: emptyList()
        _availableProfiles.value = profiles
    }

    /**
     * プロファイルを保存 (暗号化)
     */
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
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")

        try {
            val jsonContent = json.encodeToString(profile)
            writeEncryptedText(profileFile, jsonContent)
            loadAvailableProfiles()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save profile: $name", e)
            throw e
        }
    }

    /**
     * プロファイルを読み込む (復号化)
     */
    fun loadProfile(name: String): List<Shortcut>? {
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        if (!profileFile.exists()) {
            Log.e(TAG, "Profile file not found: $name")
            return null
        }

        return try {
            val jsonContent = readEncryptedText(profileFile)
            val profile = json.decodeFromString<SerializableProfile>(jsonContent)

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
            Log.e(TAG, "Failed to load profile: $name", e)
            null
        }
    }

    /**
     * プロファイルを削除
     */
    fun deleteProfile(name: String) {
        if (name == "default") {
            Log.e(TAG, "Cannot delete default profile")
            return
        }

        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        if (profileFile.exists()) {
            try {
                profileFile.delete()
                loadAvailableProfiles()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete profile: $name", e)
                throw e
            }
        }
    }

    /**
     * プロファイルをインポート (JSON文字列から)
     */
    fun importProfile(jsonContent: String, profileName: String): Boolean {
        return try {
            val profile = json.decodeFromString<SerializableProfile>(jsonContent)
            val updatedProfile = profile.copy(name = profileName)

            val profileFile = File(profilesDir, "$profileName.$PROFILE_FILE_EXTENSION")
            val finalJsonContent = json.encodeToString(updatedProfile)
            writeEncryptedText(profileFile, finalJsonContent)
            loadAvailableProfiles()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import profile: $profileName", e)
            false
        }
    }

    /**
     * 現在のプロファイルを設定
     */
    fun setCurrentProfile(name: String) {
        _currentProfile.value = name
    }

    /**
     * プロファイルをエクスポート (JSON文字列として)
     */
    fun exportProfile(name: String): String? {
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        if (!profileFile.exists()) {
            Log.e(TAG, "Profile file not found for export: $name")
            return null
        }

        return try {
            readEncryptedText(profileFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export profile: $name", e)
            null
        }
    }

    /**
     * すべてのプロファイルの名前を取得
     */
    fun getAllProfileNames(): List<String> {
        return _availableProfiles.value
    }

    /**
     * プロファイルの存在チェック
     */
    fun profileExists(name: String): Boolean {
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        return profileFile.exists()
    }

    /**
     * プロファイルのリネーム
     */
    fun renameProfile(oldName: String, newName: String): Boolean {
        if (oldName == "default") {
            Log.e(TAG, "Cannot rename default profile")
            return false
        }

        val oldFile = File(profilesDir, "$oldName.$PROFILE_FILE_EXTENSION")
        val newFile = File(profilesDir, "$newName.$PROFILE_FILE_EXTENSION")

        if (!oldFile.exists()) {
            Log.e(TAG, "Profile not found for rename: $oldName")
            return false
        }

        if (newFile.exists()) {
            Log.e(TAG, "Target profile already exists: $newName")
            return false
        }

        return try {
            // 既存データを読み込み、名前を変更して保存
            val shortcuts = loadProfile(oldName) ?: return false
            saveProfile(newName, shortcuts)
            deleteProfile(oldName)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename profile: $oldName -> $newName", e)
            false
        }
    }

    /**
     * プロファイルのバリデーション
     */
    fun validateProfile(jsonContent: String): Boolean {
        return try {
            json.decodeFromString<SerializableProfile>(jsonContent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Invalid profile JSON", e)
            false
        }
    }

    /**
     * プロファイルの複製
     */
    fun duplicateProfile(sourceName: String, targetName: String): Boolean {
        val shortcuts = loadProfile(sourceName) ?: return false

        return try {
            saveProfile(targetName, shortcuts)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to duplicate profile: $sourceName -> $targetName", e)
            false
        }
    }

    /**
     * すべてのプロファイルを削除 (初期化)
     */
    fun clearAllProfiles() {
        profilesDir.listFiles()?.forEach { file ->
            if (file.extension == "enc") {
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete file: ${file.name}", e)
                }
            }
        }
        loadAvailableProfiles()
    }

    /**
     * プロファイルディレクトリのパスを取得
     */
    fun getProfilesDirectory(): File = profilesDir

    /**
     * プロファイルの総数を取得
     */
    fun getProfileCount(): Int = _availableProfiles.value.size

    /**
     * プロファイルの最終更新日時を取得
     */
    fun getProfileLastModified(name: String): Long? {
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        return if (profileFile.exists()) {
            profileFile.lastModified()
        } else {
            null
        }
    }

    /**
     * プロファイルのファイルサイズを取得 (バイト)
     */
    fun getProfileFileSize(name: String): Long? {
        val profileFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")
        return if (profileFile.exists()) {
            profileFile.length()
        } else {
            null
        }
    }

    /**
     * デフォルトプロファイルの初期化
     */
    fun initializeDefaultProfile(shortcuts: List<Shortcut>) {
        if (!profileExists("default")) {
            saveProfile("default", shortcuts)
        }
    }

    /**
     * プロファイルのマージ (既存のショートカットに追加)
     */
    fun mergeProfile(targetName: String, additionalShortcuts: List<Shortcut>): Boolean {
        val existingShortcuts = loadProfile(targetName) ?: emptyList()
        val mergedShortcuts = existingShortcuts + additionalShortcuts

        return try {
            saveProfile(targetName, mergedShortcuts)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge profile: $targetName", e)
            false
        }
    }

    /**
     * プロファイルの統計情報を取得
     */
    data class ProfileStats(
        val name: String,
        val shortcutCount: Int,
        val categoryCounts: Map<ShortcutCategory, Int>,
        val emptyShortcuts: Int,
        val lastModified: Long,
        val fileSize: Long
    )

    fun getProfileStats(name: String): ProfileStats? {
        val shortcuts = loadProfile(name) ?: return null
        val lastModified = getProfileLastModified(name) ?: return null
        val fileSize = getProfileFileSize(name) ?: return null

        val categoryCounts = shortcuts.groupBy { it.category }.mapValues { it.value.size }
        val emptyShortcuts = shortcuts.count { it.isEmpty }

        return ProfileStats(
            name = name,
            shortcutCount = shortcuts.size,
            categoryCounts = categoryCounts,
            emptyShortcuts = emptyShortcuts,
            lastModified = lastModified,
            fileSize = fileSize
        )
    }

    // ========================================
    // 暗号化関連の内部メソッド
    // ========================================

    /**
     * EncryptedFileインスタンスを生成
     */
    private fun createEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    /**
     * 暗号化してテキストを書き込む
     */
    private fun writeEncryptedText(file: File, text: String) {
        val encryptedFile = createEncryptedFile(file)
        encryptedFile.openFileOutput().use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * 復号化してテキストを読み込む
     */
    private fun readEncryptedText(file: File): String {
        val encryptedFile = createEncryptedFile(file)
        val outputStream = ByteArrayOutputStream()
        encryptedFile.openFileInput().use { input ->
            input.copyTo(outputStream)
        }
        return outputStream.toString(Charsets.UTF_8.name())
    }

    /**
     * 暗号化の健全性チェック (テスト用)
     */
    fun testEncryption(): Boolean {
        val testFile = File(profilesDir, "__test__.enc")
        return try {
            val testData = "Test encryption data"
            writeEncryptedText(testFile, testData)
            val readData = readEncryptedText(testFile)
            testFile.delete()
            testData == readData
        } catch (e: Exception) {
            Log.e(TAG, "Encryption test failed", e)
            false
        }
    }

    /**
     * MasterKeyの存在確認
     */
    fun isMasterKeyAvailable(): Boolean {
        return try {
            masterKey != null
        } catch (e: Exception) {
            Log.e(TAG, "MasterKey not available", e)
            false
        }
    }

    /**
     * 暗号化されていない古いプロファイルを暗号化形式に移行
     */
    fun migrateLegacyProfiles(): Int {
        var migratedCount = 0
        profilesDir.listFiles()?.forEach { file ->
            if (file.extension == "json" && !file.name.startsWith("__")) {
                try {
                    val jsonContent = file.readText()
                    val name = file.nameWithoutExtension
                    val newFile = File(profilesDir, "$name.$PROFILE_FILE_EXTENSION")

                    writeEncryptedText(newFile, jsonContent)
                    file.delete()
                    migratedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate legacy profile: ${file.name}", e)
                }
            }
        }
        if (migratedCount > 0) {
            loadAvailableProfiles()
        }
        return migratedCount
    }
}
