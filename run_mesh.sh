#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Stopping existing Java/Gradle instances..."
# Safely kill java processes associated with gradle daemon or debug
killall java 2>/dev/null || true

echo "Building Android APK..."
./gradlew :mobile-app:assembleDebug --no-daemon --console=plain

# Locate Android SDK
if [ -n "$ANDROID_HOME" ]; then
    ADB="$ANDROID_HOME/platform-tools/adb"
elif [ -d "$HOME/Library/Android/sdk" ]; then
    ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
    ADB="adb"
fi

echo "Using ADB at: $ADB"

# Check if ADB is executable
if ! command -v "$ADB" &> /dev/null && [ ! -f "$ADB" ]; then
    echo "Warning: adb not found. Skipping Android installation."
    echo "Starting Desktop Environment directly..."
    ./gradlew :desktop-app:run
    exit 0
fi

echo "Installing Android APK via native ADB..."
APK_PATH="./mobile-app/build/outputs/apk/debug/mobile-app-debug.apk"

if [ -f "$APK_PATH" ]; then
    "$ADB" install -r -d -t "$APK_PATH"
    
    echo "Launching Android App on device..."
    "$ADB" shell am start -n com.brytebee.ecomesh/.MainActivity
    
    echo "Starting Desktop Environment..."
    ./gradlew :desktop-app:run
else
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi
