@echo off
echo ========================================
echo 🚀 BuyV Kotlin - Build and Run Script
echo ========================================
echo.

echo [1/3] 🔨 Building app...
call gradlew :e-commerceAndroidApp:assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)
echo ✅ Build successful!
echo.

echo [2/3] 📱 Installing app on device...
call gradlew :e-commerceAndroidApp:installDebug
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Installation failed!
    echo 💡 Make sure a device is connected: adb devices
    pause
    exit /b 1
)
echo ✅ Installation successful!
echo.

echo [3/3] 🎬 Launching app...
adb shell am start -n com.project.e_commerce.android/.MainActivity
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Launch failed!
    pause
    exit /b 1
)
echo ✅ App launched!
echo.

echo ========================================
echo ✅ Done! App is running on your device
echo ========================================
echo.
echo 💡 To view logs: adb logcat ^| findstr "ECOMMERCE_APP"
echo.

pause

