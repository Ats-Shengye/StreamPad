# StreamPad Android - Specification

## Overview

古いAndroidタブレット（Nexus 7 2013等）をPCのBluetooth HIDキーボードとして再活用し、
カスタマイズ可能な5×7グリッドからワンタップでショートカットキーを送信する左手デバイス風アプリ。
プロファイル暗号化、キープアライブ機能、長押しリピート対応。

## Architecture

```
[PC (Bluetooth受信側)] <-- Bluetooth HID Profile --> [Android App]
                                                           |
                                         +------------------+------------------+
                                         |                  |                  |
                                   UI (Compose)    BluetoothHidService   ProfileManager
                                         |                  |                  |
                                   MainViewModel      HidClientManager    EncryptedFile
                                         |                  |              (AES-256-GCM)
                                         +--------+---------+
                                                  |
                                             SettingsManager
                                          (SharedPreferences)
```

- **フロントエンド**: Jetpack Compose (宣言的UI)
- **バックエンド**: Kotlin Coroutines + Flow
- **HID通信**: BluetoothHidDevice Profile (API 30+)
- **暗号化**: androidx.security.crypto (Tink)
- **シリアライゼーション**: Kotlinx Serialization

## Functional Requirements

### F1: ショートカットグリッド

- 5×7 = 35スロットのグリッド表示
- タップでショートカットキー送信 (Modifier + KeyCode)
- 長押しでキーリピート (10Hz間隔、1秒後発動)
- ボタン: ラベル、説明、カテゴリ色分け表示
- 空スロット: 視覚的に区別 (グレーアウト)

### F2: Bluetooth HID接続

- HIDキーボードとしてPC側に認識
- ペアリング後、自動的にHIDプロファイル登録
- キー送信: チャネルキューイング (順序保証)
- フォアグラウンド通知: 送信中のみ表示、送信完了で降格
- 接続状態表示: Ready/Not Ready

### F3: プロファイル管理

- プロファイルのCRUD (作成/読み込み/更新/削除)
- AES-256-GCM暗号化 (Android Keystore連携)
- インポート/エクスポート (JSON形式、暗号化前の平文)
- プロファイル一覧表示 (最終更新日時、ファイルサイズ)
- プロファイル切り替え (ドロップダウン)
- デフォルトプロファイル: 削除不可

### F4: プロファイル編集

- companion web tool: `tools/streampad_keymap_editor.html`
- ブラウザで開き、グリッドを視覚的に編集
- JSON出力 → アプリでインポート
- 既存プロファイルのエクスポート → Web編集 → インポート

### F5: キープアライブ機能

- 専用画面でScrollLockキーを5分間隔で送信
- スマホのスリープ抑制 (Wake Lock)
- 送信成功/失敗のログ表示
- BottomNavigationで通常画面と切り替え

### F6: 設定管理

- キー押下時の振動ON/OFF
- ページ変更時の振動ON/OFF
- 視覚フィードバックON/OFF (未実装、将来用)
- サイレントモード (振動・通知を一括無効)
- 接続モード: BLUETOOTH固定 (USB削除済み)

## Non-Functional Requirements

| #   | 項目         | 内容                                                   |
| --- | ------------ | ------------------------------------------------------ |
| NF1 | 動作環境     | Android 11+ (API 30+)、Bluetooth 4.0+                 |
| NF2 | セキュリティ | プロファイル暗号化、ProGuard有効化、allowBackup=false |
| NF3 | パフォーマンス | 低スペック端末対応 (Nexus 7 2013等)                  |
| NF4 | 応答性       | キー送信遅延 < 100ms                                   |
| NF5 | リソース     | バックグラウンド時のメモリフットプリント最小化         |

## UI Design

Material Design 3準拠のダークテーマ。

- **カラースキーム**: Material3 Dynamic Color
- **メイン画面**: 5×7グリッド + プロファイル選択ドロップダウン + 設定ボタン
- **キープアライブ画面**: BottomNavigationで切り替え、スクロールログ + 開始/停止ボタン
- **プロファイル管理**: ダイアログベース、一覧 + 削除/エクスポート/インポート
- **設定画面**: スイッチ + ラベル

## Security Considerations

### 暗号化

- プロファイルファイル: AES-256-GCM (HKDF-4KB)
- MasterKey: Android Keystore管理 (端末依存、抽出不可)
- ファイル拡張子: `.json.enc`
- エクスポート時: 平文JSON (ユーザー責任)

### データ保護

- `allowBackup="false"` (クラウドバックアップ無効)
- `android:exported="false"` (外部Activityからの起動拒否)
- ProGuard: リリースビルドでコード難読化、ログ削除

### 入力バリデーション

- プロファイルJSON: Kotlinx Serializationのデシリアライズエラーハンドリング
- ファイル名: 特殊文字・パストラバーサル対策 (英数字・ハイフン・アンダースコアのみ許可)

### 権限

- `BLUETOOTH_CONNECT` (Android 12+)
- `BLUETOOTH_ADVERTISE` (Android 12+)
- `POST_NOTIFICATIONS` (Android 13+、フォアグラウンドサービス用)
- 最小権限の原則: HID通信に必要な権限のみ

## File Structure

```
StreamPad/
├── SPEC.md                            # この仕様書
├── GLOSSARY.md                        # 用語集
├── README.md                          # セットアップ手順、使い方
├── LICENSE                            # Apache 2.0 License
├── settings.gradle.kts                # Gradle設定
├── build.gradle.kts                   # プロジェクトレベル
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties  # Gradle 8.7
├── app/
│   ├── build.gradle.kts               # アプリレベル設定
│   ├── proguard-rules.pro             # ProGuard設定
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml    # 権限・サービス・Activity定義
│           ├── res/
│           │   ├── values/            # strings.xml, themes.xml
│           │   ├── drawable/          # アイコン
│           │   └── mipmap/            # ランチャーアイコン
│           └── java/com/streampad/bt/
│               ├── MainActivity.kt               # メインエントリポイント
│               ├── service/
│               │   └── BluetoothHidService.kt   # HIDデバイス登録・キー送信
│               ├── hid/
│               │   ├── IHidClient.kt            # HID抽象化インターフェース
│               │   ├── BluetoothHidClient.kt    # Bluetooth HID実装
│               │   ├── DummyHidClient.kt        # モック実装
│               │   ├── HidClientManager.kt      # クライアント切り替え管理
│               │   └── HidResult.kt             # Result型 (Success/Failure)
│               ├── model/
│               │   ├── Shortcut.kt              # ショートカット定義
│               │   ├── Profile.kt               # プロファイル定義
│               │   └── Settings.kt              # 設定定義
│               ├── ui/
│               │   ├── MainScreen.kt            # メイングリッド画面
│               │   ├── KeepAliveScreen.kt       # キープアライブ画面
│               │   ├── ProfileManagementScreen.kt # プロファイル管理画面
│               │   ├── MainViewModel.kt         # 状態管理
│               │   └── theme/
│               │       ├── Theme.kt             # Material3テーマ
│               │       └── Typography.kt        # タイポグラフィ
│               └── utils/
│                   ├── ProfileManager.kt        # プロファイルCRUD・暗号化
│                   ├── ProfileManagerFactory.kt # DI用Factory
│                   └── SettingsManager.kt       # 設定永続化
└── tools/
    └── streampad_keymap_editor.html   # プロファイル編集Webツール
```

## Dependencies

### ランタイム依存

| ライブラリ                       | 用途                           | バージョン |
| -------------------------------- | ------------------------------ | ---------- |
| androidx.core:core-ktx           | Kotlin拡張                     | 1.12.0     |
| androidx.lifecycle:lifecycle-runtime-ktx | ライフサイクル管理         | 2.7.0      |
| androidx.activity:activity-compose | Compose統合                  | 1.8.2      |
| androidx.compose:compose-bom     | Compose依存管理                | 2024.02.00 |
| androidx.compose.material3       | Material3 UI                   | BOM管理    |
| androidx.lifecycle:lifecycle-viewmodel-compose | ViewModelとCompose統合 | 2.7.0 |
| androidx.navigation:navigation-compose | BottomNavigation         | 2.7.6      |
| kotlinx-coroutines-android       | 非同期処理                     | 1.8.1      |
| kotlinx-serialization-json       | JSONシリアライゼーション       | 1.6.2      |
| androidx.security:security-crypto | EncryptedFile (Tink)          | 1.1.0-alpha06 |

### 開発依存

| ライブラリ                     | 用途           | バージョン |
| ------------------------------ | -------------- | ---------- |
| androidx.compose.ui:ui-tooling | Composeプレビュー | BOM管理    |
| androidx.compose.ui:ui-test-manifest | テスト用マニフェスト | BOM管理 |

## Development

- **GitHub公開前提**: ポートフォリオ兼用。コメント・READMEは外部エンジニア可読レベル
- **テスト**: 未実装（将来的にJUnit + Espresso追加予定）
- **コード規約**: Coding.md準拠、Kotlin公式スタイルガイド
- **コメント**: フォーマル（外部エンジニア可読）、日本語

## Data Model

### Shortcut

```kotlin
data class Shortcut(
    val label: String,           // 表示ラベル ("Ctrl+C")
    val description: String,     // 説明 ("コピー")
    val modifier: Byte,          // Modifierキー (0x00 = なし、0x02 = Ctrl)
    val keyCode: Byte,           // HID Usage ID (0x06 = C)
    val category: ShortcutCategory, // COPY_PASTE/EDIT/NAVIGATION/CUSTOM
    val isEmpty: Boolean = false // 空スロットフラグ
)

enum class ShortcutCategory {
    COPY_PASTE,  // 青系
    EDIT,        // 緑系
    NAVIGATION,  // オレンジ系
    CUSTOM       // 紫系
}
```

### Profile

```kotlin
data class Profile(
    val name: String,                // プロファイル名 ("default")
    val shortcuts: List<Shortcut>    // 35個のショートカット
)
```

### Settings

```kotlin
data class Settings(
    val keyPressVibration: Boolean = true,
    val pageChangeVibration: Boolean = true,
    val visualFeedback: Boolean = false,  // 将来用
    val silentMode: Boolean = false,      // 振動・通知一括OFF
    val connectionMode: ConnectionMode = ConnectionMode.BLUETOOTH
)

enum class ConnectionMode {
    BLUETOOTH  // USB削除済み
}
```

### HidResult

```kotlin
sealed class HidResult {
    object Success : HidResult()
    sealed class Failure : HidResult() {
        data class Unsupported(val message: String) : Failure()
    }
}
```

## HID Specification

### HID Report Descriptor

```
Usage Page: Generic Desktop (0x01)
Usage: Keyboard (0x06)
Collection: Application
  Modifier Keys: 1 byte (Ctrl, Shift, Alt, GUI)
  Reserved: 1 byte
  Key Codes: 6 bytes (同時押し6キーまで)
```

合計8バイトのHIDレポート。

### Modifier Bitmap

| Modifier | Bit | 値   |
| -------- | --- | ---- |
| None     | -   | 0x00 |
| Ctrl     | 0   | 0x01 |
| Shift    | 1   | 0x02 |
| Alt      | 2   | 0x04 |
| GUI      | 3   | 0x08 |
| Ctrl+Shift | - | 0x03 |

### HID Usage ID (例)

| キー | Usage ID |
| ---- | -------- |
| A    | 0x04     |
| B    | 0x05     |
| C    | 0x06     |
| 1    | 0x1E     |
| Enter| 0x28     |
| Esc  | 0x29     |
| ScrollLock | 0x47 |

## Bluetooth HID Flow

```
1. [App] BluetoothHidService.onCreate()
2. [App] registerHidApplication()
3. [System] HIDアプリケーション登録完了コールバック
4. [PC] Bluetooth検索 → ペアリング
5. [System] onConnectionStateChanged(CONNECTED)
6. [App] isHidRegistered = true
7. [User] ボタンタップ
8. [App] sendKeyPress(modifier, keyCode)
9. [Service] keyChannel.send((modifier, keyCode))
10. [Service] sendKeySequence() → sendReport()
11. [PC] キー入力受信
```

## Foreground Service Strategy

- 通常時: バックグラウンド (通知なし)
- キー送信時: フォアグラウンド昇格 (通知表示)
- 送信完了5秒後: バックグラウンド降格 (通知削除)
- キープアライブ稼働中: フォアグラウンド維持

これにより、常時通知を避けつつ、システムキルを防ぐ。

## Build Configuration

### Debug Build

- minifyEnabled: false
- shrinkResources: false
- proguardFiles: なし
- Log.d() 有効

### Release Build

- minifyEnabled: true
- shrinkResources: true
- proguardFiles: proguard-android-optimize.txt + proguard-rules.pro
- Log.d() 削除 (ProGuard設定)

## Toolchain

| ツール                | バージョン |
| --------------------- | ---------- |
| JDK                   | 17         |
| Gradle Wrapper        | 8.7        |
| Android Gradle Plugin | 8.6.1      |
| Kotlin                | 1.9.22     |
| Compose Compiler      | 1.5.8      |
| Target SDK            | 34         |
| Min SDK               | 30         |

## Tier

- **Tier**: C（小規模完結型）
- **開発端末**: PC (Android Studio) + 実機デバッグ (タブレット)
- **テスト対象**: Nexus 7 2013 (Android 11カスタムROM)
- **行数**: ~2000 lines (推定)
