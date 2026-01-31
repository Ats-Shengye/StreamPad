# Glossary

本プロジェクトの全モジュール・クラス・関数・定数・データモデルの一覧。
コードリーディングの補助資料として使用。

updated: 2026-01-31

## モジュール・クラス

| 名前                     | 種別       | ファイル                                | 役割                                                         |
| ------------------------ | ---------- | --------------------------------------- | ------------------------------------------------------------ |
| `BluetoothHidService`    | Service    | service/BluetoothHidService.kt          | Bluetooth HIDデバイス登録とキー送信を管理するAndroidサービス |
| `BluetoothHidClient`     | クラス     | hid/BluetoothHidClient.kt               | IHidClient実装、BluetoothHidServiceへのバインディング管理    |
| `HidClientManager`       | クラス     | hid/HidClientManager.kt                 | HIDクライアントの切り替え（Bluetooth/Dummy）と生存期間管理   |
| `DummyHidClient`         | クラス     | hid/DummyHidClient.kt                   | IHidClient実装、ログのみ出力するモック                       |
| `ProfileManager`         | クラス     | utils/ProfileManager.kt                 | プロファイルの暗号化保存・復号化読み込み・インポート・エクスポート |
| `SettingsManager`        | クラス     | utils/SettingsManager.kt                | アプリ設定（振動・視覚フィードバック・接続モード）の永続化   |
| `MainViewModel`          | ViewModel  | ui/MainViewModel.kt                     | ショートカットグリッドの状態管理とプロファイル操作           |
| `MainActivity`           | Activity   | MainActivity.kt                         | メインアクティビティ、権限管理・Bluetooth有効化・HID初期化   |
| `IHidClient`             | interface  | hid/IHidClient.kt                       | HID実装の契約（start/stop/sendKeyPress/isSupported/isReady） |

## BluetoothHidService メソッド

| 名前                      | 種別           | 役割                                                     |
| ------------------------- | -------------- | -------------------------------------------------------- |
| `onCreate()`              | override fun   | サービス初期化、keyChannelの処理ループ開始               |
| `onBind(intent)`          | override fun   | LocalBinderを返却、Activity/ViewModelからのバインド受付  |
| `onDestroy()`             | override fun   | HIDアプリケーション登録解除、Bluetoothプロキシクローズ   |
| `initializeBluetooth()`   | private fun    | BluetoothManagerとAdapterの初期化、HID Deviceプロキシ取得 |
| `registerHidApplication()`| private fun    | HIDキーボードディスクリプタ登録、SDP設定、QoS設定        |
| `sendKeyPress(modifier, keyCode)` | public fun | キー入力をkeyChannelにエンキュー                         |
| `sendKeySequence(modifier, keyCode)` | private suspend fun | キーダウン（100ms保持）→リリース（50ms間隔）のシーケンス実行 |
| `ensureForeground()`      | private fun    | フォアグラウンドサービスへ昇格（通知表示）               |
| `scheduleDemote(timeoutMs)` | private fun  | 指定時間後にバックグラウンドへ降格（通知削除）           |
| `createNotificationChannel()` | private fun | Android O以降の通知チャネル作成                          |
| `createNotification()`    | private fun    | フォアグラウンド通知の生成                               |

## BluetoothHidClient メソッド

| 名前                          | 種別         | 役割                                         |
| ----------------------------- | ------------ | -------------------------------------------- |
| `start(context)`              | override fun | BluetoothHidServiceの起動とバインド          |
| `stop(context)`               | override fun | サービスのアンバインドと停止                 |
| `sendKeyPress(modifier, keyCode)` | override fun | サービス経由でキー送信                       |
| `isSupported(context)`        | override fun | Bluetooth HIDサポート確認（BluetoothAdapter存在チェック） |

## HidClientManager メソッド

| 名前                          | 種別     | 役割                                                 |
| ----------------------------- | -------- | ---------------------------------------------------- |
| `switchTo(mode)`              | fun      | 接続モード切り替え（BLUETOOTH/DUMMY）、HidResult返却 |
| `stop()`                      | fun      | 現在のクライアント停止、DummyClientへ切り替え        |
| `sendKeyPress(modifier, keyCode)` | fun  | 現在のクライアントへキー送信を委譲                   |
| `isBluetoothSupported()`      | fun      | Bluetooth HIDサポート確認                            |
| `isUsbSupported()`            | fun      | USB HIDサポート確認（常にfalse）                     |
| `isUsbConfigFsAvailable()`    | fun      | USB ConfigFS利用可否（常にfalse）                    |

## ProfileManager メソッド

| 名前                                  | 種別     | 役割                                                 |
| ------------------------------------- | -------- | ---------------------------------------------------- |
| `saveProfile(name, shortcuts)`        | fun      | プロファイルをAES-256-GCM暗号化して保存              |
| `loadProfile(name)`                   | fun      | プロファイルを復号化して読み込み（List<Shortcut>返却） |
| `deleteProfile(name)`                 | fun      | プロファイル削除（defaultは削除不可）                |
| `importProfile(jsonContent, name)`    | fun      | JSON形式でプロファイルをインポート                   |
| `exportProfile(name)`                 | fun      | プロファイルをJSON形式でエクスポート                 |
| `renameProfile(oldName, newName)`     | fun      | プロファイルのリネーム                               |
| `duplicateProfile(source, target)`    | fun      | プロファイルの複製                                   |
| `validateProfile(jsonContent)`        | fun      | プロファイルJSONのバリデーション                     |
| `mergeProfile(target, additional)`    | fun      | プロファイルのマージ（既存に追加）                   |
| `getProfileStats(name)`               | fun      | プロファイルの統計情報取得                           |
| `loadAvailableProfiles()`             | fun      | 利用可能なプロファイル一覧をディレクトリから読み込み |
| `setCurrentProfile(name)`             | fun      | 現在のプロファイルを設定                             |
| `getAllProfileNames()`                | fun      | すべてのプロファイル名を取得                         |
| `profileExists(name)`                 | fun      | プロファイルの存在チェック                           |
| `clearAllProfiles()`                  | fun      | すべてのプロファイルを削除（初期化）                 |
| `getProfilesDirectory()`              | fun      | プロファイルディレクトリのパスを取得                 |
| `getProfileCount()`                   | fun      | プロファイルの総数を取得                             |
| `getProfileLastModified(name)`        | fun      | プロファイルの最終更新日時を取得                     |
| `getProfileFileSize(name)`            | fun      | プロファイルのファイルサイズを取得（バイト）         |
| `initializeDefaultProfile(shortcuts)` | fun      | デフォルトプロファイルの初期化                       |
| `testEncryption()`                    | fun      | 暗号化の健全性チェック（テスト用）                   |
| `isMasterKeyAvailable()`              | fun      | MasterKeyの存在確認                                  |
| `migrateLegacyProfiles()`             | fun      | 非暗号化プロファイル→暗号化への移行処理              |

## SettingsManager メソッド

| 名前                              | 種別         | 役割                                     |
| --------------------------------- | ------------ | ---------------------------------------- |
| `loadSettings()`                  | private fun  | SharedPreferencesから設定を読み込み      |
| `updateKeyPressVibration(enabled)` | fun         | キー押下時の振動設定を更新               |
| `updatePageChangeVibration(enabled)` | fun       | ページ変更時の振動設定を更新             |
| `updateVisualFeedback(enabled)`   | fun          | 視覚フィードバック設定を更新             |
| `updateSilentMode(enabled)`       | fun          | サイレントモード設定を更新               |
| `updateConnectionMode(mode)`      | fun          | 接続モード設定を更新                     |
| `saveAndEmit(settings)`           | private fun  | 設定をSharedPreferencesに保存してStateFlowへ反映 |

## MainViewModel メソッド

| 名前                                      | 種別         | 役割                                             |
| ----------------------------------------- | ------------ | ------------------------------------------------ |
| `loadProfile(profileName)`                | fun          | プロファイルを読み込み、35スロットに調整         |
| `saveCurrentAsProfile(profileName)`       | fun          | 現在のショートカット状態をプロファイルとして保存 |
| `getAvailableProfiles()`                  | fun          | 利用可能なプロファイル一覧を取得                 |
| `getCurrentProfile()`                     | fun          | 現在のプロファイル名を取得                       |
| `deleteProfile(profileName)`              | fun          | プロファイル削除                                 |
| `importProfile(jsonContent, profileName)` | fun          | プロファイルのインポート                         |
| `getProfileShortcuts(profileName)`        | fun          | 指定プロファイルのショートカット一覧を取得       |
| `saveProfileWithShortcuts(name, shortcuts)` | fun        | プロファイルをショートカットリストで保存         |
| `getDefaultShortcuts()`                   | private fun  | デフォルトのショートカットグリッドを生成         |

## MainActivity 主要メソッド

| 名前                                  | 種別         | 役割                                         |
| ------------------------------------- | ------------ | -------------------------------------------- |
| `onCreate(savedInstanceState)`        | override fun | アクティビティ初期化、Compose UI構築         |
| `checkPermissionsAndStartBtService()` | private fun  | Bluetooth権限確認とサービス起動              |
| `enableBluetooth()`                   | private fun  | Bluetoothの有効化                            |
| `switchToBluetooth()`                 | private fun  | HidClientManagerをBluetoothモードに切り替え  |
| `sendShortcut(modifier, keyCode)`     | private fun  | ショートカットキーの送信                     |
| `startContinuousInput(modifier, keyCode)` | private fun | 長押し時のキーリピートループ開始             |
| `stopContinuousInput()`               | private fun  | キーリピートループ停止                       |
| `vibrate()`                           | private fun  | タップフィードバック振動を実行               |
| `onDestroy()`                         | override fun | HidClientManagerの停止                       |
| `isUsbHidAvailable()`                 | fun          | USB HID利用可否（常にfalse）                 |
| `requestNotificationPermissionIfNeeded()` | private fun | 通知権限のリクエスト（Android 13以降）       |

## 定数

| 名前                          | 値           | 役割                                           |
| ----------------------------- | ------------ | ---------------------------------------------- |
| `VIBRATION_DURATION_MS`       | 50L          | タップフィードバック持続時間（ミリ秒）         |
| `LONG_PRESS_DELAY_MS`         | 1000L        | 長押し判定しきい値（ミリ秒）                   |
| `CONTINUOUS_INPUT_INTERVAL_MS`| 100L         | リピート間隔（10Hz）                           |
| `SCROLL_LOCK_KEY_CODE`        | 0x47         | キープアライブ用キー（ScrollLock）             |
| `PROFILE_FILE_EXTENSION`      | "json.enc"   | 暗号化プロファイル拡張子                       |
| `TAG`                         | "BluetoothHidService" | ログタグ                               |
| `CHANNEL_ID`                  | "StreamPadService" | 通知チャネルID                           |
| `NOTIFICATION_ID`             | 1            | フォアグラウンド通知ID                         |
| `HID_REPORT_DESCRIPTOR`       | ByteArray    | HIDキーボードディスクリプタ（USBデバイスとしての仕様定義） |

## データモデル

| 名前                   | 種別           | ファイル               | 役割                                                         |
| ---------------------- | -------------- | ---------------------- | ------------------------------------------------------------ |
| `Profile`              | data class     | model/Profile.kt       | プロファイル名 + List<Shortcut>                              |
| `Shortcut`             | data class     | model/Shortcut.kt      | ショートカットキーの定義（label/description/modifier/keyCode/category/isEmpty） |
| `ShortcutCategory`     | enum           | model/Shortcut.kt      | COPY_PASTE/EDIT/NAVIGATION/CUSTOM                            |
| `Settings`             | data class     | model/Settings.kt      | 振動・視覚フィードバック・接続モード設定                     |
| `ConnectionMode`       | enum           | model/Settings.kt      | BLUETOOTHのみ（USB削除済み）                                 |
| `HidResult`            | sealed class   | hid/HidResult.kt       | Success or Failure.Unsupported(message)                      |
| `SerializableShortcut` | data class     | utils/ProfileManager.kt| JSONシリアライゼーション用のショートカットラッパー           |
| `SerializableProfile`  | data class     | utils/ProfileManager.kt| JSONシリアライゼーション用のプロファイルラッパー             |
| `ProfileStats`         | data class     | utils/ProfileManager.kt| プロファイルのメタデータスナップショット（shortcutCount/categoryCounts/emptyShortcuts/lastModified/fileSize） |

## 主要State・変数

| 名前                 | 種別                            | スコープ               | 役割                                       |
| -------------------- | ------------------------------- | ---------------------- | ------------------------------------------ |
| `_shortcuts`         | MutableStateFlow<List<Shortcut>>| MainViewModel          | 現在の35スロットグリッド（5×7）の編集可能な状態 |
| `shortcuts`          | StateFlow<List<Shortcut>>       | MainViewModel          | 読み取り専用のショートカットグリッド       |
| `connectedDevice`    | BluetoothDevice?                | BluetoothHidService    | 接続中のペアリング先デバイス               |
| `isHidRegistered`    | Boolean                         | BluetoothHidService    | HIDアプリケーション登録状態（true = PC側に認識済み） |
| `keyChannel`         | Channel<Pair<Byte, Byte>>       | BluetoothHidService    | キー入力キュー（modifier, keyCode のペア） |
| `current`            | IHidClient                      | HidClientManager       | 現在のHID戦略（BluetoothHidClient or DummyHidClient） |
| `_isReady`           | MutableStateFlow<Boolean>       | HidClientManager       | HIDサービス準備状態                        |
| `masterKey`          | MasterKey                       | ProfileManager         | AES-256-GCM暗号化キー（Android Keystore連携） |
| `_availableProfiles` | MutableStateFlow<List<String>>  | ProfileManager         | 保存済みプロファイル名リスト               |
| `_currentProfile`    | MutableStateFlow<String>        | ProfileManager         | 現在読み込み中のプロファイル名             |
| `_settings`          | MutableStateFlow<Settings>      | SettingsManager        | アプリ設定キャッシュ                       |
| `continuousInputJob` | Job?                            | MainActivity           | 長押しリピートのコルーチンジョブ           |

## 暗号化仕様

| 項目               | 内容                                                   |
| ------------------ | ------------------------------------------------------ |
| 暗号化アルゴリズム | AES-256-GCM（HKDF-4KB）                                |
| 鍵管理             | Android Keystore（MasterKey）                          |
| ライブラリ         | androidx.security.crypto（Tink）                       |
| ファイル拡張子     | .json.enc                                              |
| 対象データ         | プロファイル（SerializableProfile as JSON）            |
| 鍵スキーム         | MasterKey.KeyScheme.AES256_GCM                         |
| ファイルスキーム   | EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB |
