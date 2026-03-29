Write-Host "Stopping existing Java/Gradle instances..."
Stop-Process -Name java,javaw -Force -ErrorAction SilentlyContinue

Write-Host "Building Android APK..."
$gradleProcess = Start-Process -FilePath ".\gradlew" -ArgumentList ":mobile-app:assembleDebug --no-daemon --console=plain" -Wait -NoNewWindow -PassThru

if ($gradleProcess.ExitCode -eq 0) {
    Write-Host "Installing Android APK via native ADB (bypassing ddmlib)..."
    $apkPath = ".\mobile-app\build\outputs\apk\debug\mobile-app-debug.apk"
    $adbProcess = Start-Process -FilePath "C:\Users\RevFavour\android-sdk\platform-tools\adb.exe" -ArgumentList "install -r -d -t `"$apkPath`"" -Wait -NoNewWindow -PassThru
    
    if ($adbProcess.ExitCode -eq 0) {
        Write-Host "Launching Android App on device..."
        & "C:\Users\RevFavour\android-sdk\platform-tools\adb.exe" shell am start -n com.brytebee.ecomesh/.MainActivity

        Write-Host "Starting Desktop Environment..."
        .\gradlew :desktop-app:run
    } else {
        Write-Error "ADB native install failed. Please check your phone for any rogue MIUI security popups."
    }
} else {
    Write-Error "Android build failed. Aborting launch."
}
