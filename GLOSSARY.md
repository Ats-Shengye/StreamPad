# Glossary
updated: 2026-01-31

## 変数・State
| 名前 | 種別 | 役割 |
|------|------|------|
| `_shortcuts` | MutableStateFlow<List<Shortcut>> | 現在の35スロットグリッド（5×7）の編集可能な状態 |
| `shortcuts` | StateFlow<List<Shortcut>> | 読み取り専用のショートカットグリッド |
| `connectedDevice` | BluetoothDevice? | 接続中のペアリング先デバイス |
| `isHidRegistered` | Boolean | HIDアプリケーション登録状態（true = PC側に認識済み） |
| `keyChannel` | Channel<Pair<Byte, Byte>> | キー入力キュー（modifier, keyCode のペア） |
| `current` | IHidClient | 現在のHID戦略（BluetoothHidClient or DummyHidClient） |
| `_isReady` | MutableStateFlow<Boolean> | HIDサービス準備状態 |
| `masterKey` | MasterKey | AES-256-GCM暗号化キー（Android Keystore連携） |
| `_availableProfiles` | MutableStateFlow<List<String>> | 保存済みプロファイル名リスト |
| `_currentProfile` | MutableStateFlow<String> | 現在読み込み中のプロファイル名 |
| `_settings` | MutableStateFlow<Settings> | アプリ設定キャッシュ |
| `continuousInputJob` | Job? | 長押しリピートのコルーチンジョブ |

## 関数・Hooks
| 名前 | 種別 | 役割 |
|------|------|------|
| `registerHidApplication()` | private fun | HIDキーボードディスクリプタ登録、SDP設定、QoS設定 |
| `sendKeyPress(modifier, keyCode)` | public fun | キー入力をkeyChannelにエンキューする |
| `sendKeySequence(modifier, keyCode)` | private suspend fun | キーダウン（100ms保持）→リリース（50ms間隔）のシーケンスを実行 |
| `switchTo(mode)` | fun | HidClientManagerの戦略切替（BLUETOOTH/DUMMY） |
| `startContinuousInput(modifier, keyCode)` | private fun | 長押し時のキーリピートループ開始 |
| `saveProfile(name, shortcuts)` | suspend fun | プロファイルをAES-256-GCM暗号化して保存 |
| `loadProfile(name)` | suspend fun | プロファイルを復号化して読み込み |
| `deleteProfile(name)` | suspend fun | プロファイル削除 |
| `importProfile(jsonContent, name)` | fun | JSON形式でプロファイルをインポート |
| `exportProfile(name)` | fun | プロファイルをJSON形式でエクスポート |
| `migrateLegacyProfiles()` | fun | 非暗号化プロファイル→暗号化への移行処理 |
| `ensureForeground()` | private fun | フォアグラウンドサービスへ昇格（通知表示） |
| `scheduleDemote(timeoutMs)` | private fun | 指定時間後にバックグラウンドへ降格（通知削除） |

## 型・Interface
| 名前 | 役割 |
|------|------|
| `IHidClient` | HID実装の契約（start/stop/sendKeyPress/isSupported/isReady） |
| `BluetoothHidClient` | IHidClient実装、BluetoothHidServiceにバインド |
| `DummyHidClient` | IHidClient実装、ログのみ出力するモック |
| `HidResult` | sealed class、Success or Failure.Unsupported(message) |
| `Profile` | data class、プロファイル名 + List<Shortcut> |
| `Shortcut` | data class、ショートカットキーの定義（label/description/modifier/keyCode/category/isEmpty） |
| `ShortcutCategory` | enum、COPY_PASTE/EDIT/NAVIGATION/CUSTOM |
| `Settings` | data class、振動・視覚フィードバック・接続モード設定 |
| `ConnectionMode` | enum、BLUETOOTHのみ（USB削除済み） |
| `SerializableShortcut` | data class、JSONシリアライゼーション用のラッパー |
| `SerializableProfile` | data class、JSONシリアライゼーション用のプロファイルラッパー |
| `ProfileStats` | data class、プロファイルのメタデータスナップショット（shortcutCount/sizeBytes/lastModified/isEncrypted） |

## 定数
| 名前 | 値 | 役割 |
|------|-----|------|
| `VIBRATION_DURATION_MS` | 50L | タップフィードバック持続時間（ミリ秒） |
| `LONG_PRESS_DELAY_MS` | 1000L | 長押し判定しきい値（ミリ秒） |
| `CONTINUOUS_INPUT_INTERVAL_MS` | 100L | リピート間隔（10Hz） |
| `SCROLL_LOCK_KEY_CODE` | 0x47 | キープアライブ用キー（ScrollLock） |
| `PROFILE_FILE_EXTENSION` | "json.enc" | 暗号化プロファイル拡張子 |
| `HID_REPORT_DESCRIPTOR` | ByteArray | HIDキーボードディスクリプタ（USBデバイスとしての仕様定義） |
