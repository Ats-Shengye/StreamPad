# StreamPad Android

使わなくなったAndroidタブレットを、PCのショートカットキーパッドとして再活用するアプリ。
Bluetooth接続でPCにキーボードとして認識させ、カスタマイズ可能な5×7グリッドからワンタップでショートカットキーを送信する。
古いタブレット（Nexus 7 2013等）に最適化した左手デバイス風キーボードアプリ。

## 技術スタック

- 言語: Kotlin
- フレームワーク: Jetpack Compose
- 主要技術: Bluetooth HID Device Profile
- セキュリティ: AES-256-GCM暗号化（Android Keystore連携）
- シリアライゼーション: Kotlinx Serialization

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

### 使い方

1. アプリを起動
2. Bluetooth権限を許可
3. PC側でBluetoothデバイス検索 → ペアリング
4. 接続後、グリッドのボタンをタップでキー送信
5. 長押しでキーリピート（10Hz間隔）

キーマップ編集: `tools/streampad_keymap_editor.html` をブラウザで開いてプロファイルを作成・編集。

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

## 関連ドキュメント

- [GLOSSARY.md](./GLOSSARY.md) - コード内の主要な変数・関数・型の役割一覧
