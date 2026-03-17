Write-Host "Stopping existing Java/Gradle instances..."
Stop-Process -Name java -Force -ErrorAction SilentlyContinue

Write-Host "Building and installing Android APK..."
$gradleProcess = Start-Process -FilePath ".\gradlew" -ArgumentList ":mobile-app:installDebug --no-daemon --console=plain" -Wait -NoNewWindow -PassThru

if ($gradleProcess.ExitCode -eq 0) {
    Write-Host "Launching Android App on device..."
    & "C:\Users\RevFavour\android-sdk\platform-tools\adb.exe" shell am start -n com.brytebee.ecomesh/.MainActivity

    Write-Host "Starting Desktop Environment..."
    .\gradlew :desktop-app:run
} else {
    Write-Error "Android build failed. Aborting launch."
}
