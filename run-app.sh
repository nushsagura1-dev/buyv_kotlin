#!/bin/bash

echo "========================================"
echo "🚀 BuyV Kotlin - Build and Run Script"
echo "========================================"
echo ""

echo "[1/3] 🔨 Building app..."
./gradlew :e-commerceAndroidApp:assembleDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi
echo "✅ Build successful!"
echo ""

echo "[2/3] 📱 Installing app on device..."
./gradlew :e-commerceAndroidApp:installDebug

if [ $? -ne 0 ]; then
    echo "❌ Installation failed!"
    echo "💡 Make sure a device is connected: adb devices"
    exit 1
fi
echo "✅ Installation successful!"
echo ""

echo "[3/3] 🎬 Launching app..."
adb shell am start -n com.project.e_commerce.android/.MainActivity

if [ $? -ne 0 ]; then
    echo "❌ Launch failed!"
    exit 1
fi
echo "✅ App launched!"
echo ""

echo "========================================"
echo "✅ Done! App is running on your device"
echo "========================================"
echo ""
echo "💡 To view logs: adb logcat | grep ECOMMERCE_APP"
echo ""

