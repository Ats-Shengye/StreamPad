# Streampad Android
左手デバイス風アプリ。
Nexus 7 (2013) での利用を想定。
Android (Kotlin) アプリ。GitHub 公開用にビルド成果物と個人環境ファイルを除外済み。

## Build
- 推奨: Android Studio (stable) で開く → Sync → Run
- CLI: `./gradlew assembleDebug`

## Tools
- Keymap Generator: `tools/streampad_keymap_editor.html`
  - ブラウザで開いて編集 → エクスポート
  - 生成したマップのインポート先や形式は `app/` 実装に合わせて各自で調整（現状は汎用 JSON/テキスト想定）。

## Toolchain
- JDK: 17
- Gradle Wrapper: 8.7 (wrapper で自動取得)
- Android Gradle Plugin: 8.6.1
- Kotlin: 1.9.22 / Compose Compiler: 1.5.8

## Requirements
- Android SDK: `local.properties` は除外済み（各自 `sdk.dir` を設定）
- CLI 派は `ANDROID_SDK_ROOT` か `ANDROID_HOME` を環境変数で指定可

## Notes
- keystore や `google-services.json` はリポジトリに含まず
- Issue/PR で構成変更がある場合は `.gitignore` を更新

## 用意してほしいもの（ユーザー側）
- JDK 17: インストールは各自。Android Studio 同梱の JDK でも可。
- Android SDK: Android Studio を入れれば自動。CLI ビルド派は環境変数で指定。
  - 例: `export ANDROID_SDK_ROOT="$HOME/Android/Sdk"`
- `local.properties`: SDK パスのみのローカル専用ファイル。
  - 例: `sdk.dir=/home/{USER}/Android/Sdk`
- リリース署名用のキーストア（リリースビルドを作る場合のみ必要）
  - keystore 本体（`.jks`/`.keystore`）、パスワード、`keyAlias`
  - これらはコミットしない。`signingConfigs` は各自のローカルで設定。
- Firebase/Google サービスを使う場合のみ
  - `app/google-services.json` を各自取得して配置
  - 使うときは `app/build.gradle.kts` の `plugins` ブロックに `id("com.google.gms.google-services")` を追加

現状のコードでは外部 API キー類は不要。追加する場合は `BuildConfig` などに注入、値は環境変数や未コミットの設定ファイルから読み込み。
