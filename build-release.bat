@echo off
echo ========================================
echo 📦 BuyV Kotlin - Build Release APK
echo ========================================
echo.

echo 🔨 Building Release APK...
call gradlew :e-commerceAndroidApp:assembleRelease

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Build failed!
    pause
    exit /b 1
)

echo.
echo ✅ Build successful!
echo.
echo 📍 APK Location:
echo    e-commerceAndroidApp\build\outputs\apk\release\app-release.apk
echo.
echo 💡 Note: This APK is unsigned. For Play Store, use:
echo    .\gradlew :e-commerceAndroidApp:bundleRelease
echo.

pause

