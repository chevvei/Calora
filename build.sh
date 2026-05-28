#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="$PROJECT_DIR/.dev/jdk-17.0.12"
export GRADLE_USER_HOME="$PROJECT_DIR/.dev/gradle-home"
export ANDROID_SDK_ROOT="$PROJECT_DIR/../myApp/.dev/android-sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_PREFS_ROOT="$PROJECT_DIR/.dev/dot-android"

cd "$PROJECT_DIR"

case "${1:-build}" in
  build)
    ./gradlew assembleDebug --no-daemon
    echo "APK: $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    ;;
  install)
    ./gradlew assembleDebug --no-daemon
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ;;
  release)
    ./gradlew assembleRelease --no-daemon
    echo "APK: $PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
    ;;
  clean)
    ./gradlew clean --no-daemon
    ;;
  *)
    echo "Usage: $0 {build|install|release|clean}"
    exit 1
    ;;
esac
