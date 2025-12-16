# StreamPad Android

Bluetooth HID対応の左手デバイス風キーボードアプリ。古いタブレット（Nexus 7 2013等）を外部キーパッドとして再活用。

## 機能

- Bluetooth HIDプロファイルでPCに接続（キーボードとして認識）
- カスタマイズ可能なショートカットグリッド（Compact/Expanded）
- プロファイル管理（保存・切替・インポート/エクスポート）
- 長押しでキーリピート
- 触覚フィードバック（バイブレーション）

## 技術スタック

- Kotlin
- Jetpack Compose
- Bluetooth HID Device Profile
- EncryptedFile（プロファイル暗号化）
- Kotlinx Serialization

## ファイル構成

```
app/src/main/java/com/streampad/bt/
├── MainActivity.kt           # エントリーポイント
├── hid/
│   ├── BluetoothHidClient.kt # Bluetooth HID実装
│   ├── HidClientManager.kt   # HIDクライアント管理
│   └── HidResult.kt          # 操作結果型
├── model/
│   ├── Profile.kt            # プロファイルデータ
│   ├── Settings.kt           # 設定
│   └── Shortcut.kt           # ショートカット定義
├── service/
│   └── BluetoothHidService.kt # Bluetoothサービス
├── ui/
│   ├── MainScreen.kt         # メイン画面
│   ├── MainViewModel.kt      # ViewModel
│   └── ProfileManagementScreen.kt
└── utils/
    ├── ProfileManager.kt     # プロファイル永続化（暗号化）
    └── SettingsManager.kt    # 設定管理

tools/
└── streampad_keymap_editor.html  # キーマップ編集ツール
```

## セットアップ

### 必要なもの

- JDK 17
- Android SDK（Android Studio推奨）
- Bluetooth HID対応のAndroid端末（API 30+）

### ビルド

```bash
# Android Studioで開く → Sync → Run
# または CLI:
./gradlew assembleDebug

# リリースビルド（署名設定が必要）
./gradlew assembleRelease
```

### local.properties

```properties
sdk.dir=/path/to/Android/Sdk
```

## 使い方

1. アプリを起動
2. Bluetooth権限を許可
3. PC側でBluetoothデバイス検索 → ペアリング
4. 接続後、グリッドのボタンをタップでキー送信

### キーマップ編集

`tools/streampad_keymap_editor.html` をブラウザで開いてプロファイルを作成・編集。

## セキュリティ

- プロファイルデータはAES-256-GCMで暗号化（Android Keystore連携）
- リリースビルドはProGuardでコード難読化・ログ削除
- `allowBackup=false`（クラウドバックアップ無効）

## Toolchain

| ツール | バージョン |
|--------|-----------|
| JDK | 17 |
| Gradle Wrapper | 8.7 |
| Android Gradle Plugin | 8.6.1 |
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.8 |
| Target SDK | 34 |
| Min SDK | 30 |

## 更新履歴

### v1.1
- USB HID機能削除（root不要化）
- プロファイル暗号化（EncryptedFile）
- ProGuard有効化
- コード品質改善（Result型、DI対応）

### v1.0
- 初回リリース
