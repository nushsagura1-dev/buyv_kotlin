#!/bin/bash

echo "========================================"
echo "📦 BuyV Kotlin - Build Release APK"
echo "========================================"
echo ""

echo "🔨 Building Release APK..."
./gradlew :e-commerceAndroidApp:assembleRelease

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""
echo "📍 APK Location:"
echo "   e-commerceAndroidApp/build/outputs/apk/release/app-release.apk"
echo ""
echo "💡 Note: This APK is unsigned. For Play Store, use:"
echo "   ./gradlew :e-commerceAndroidApp:bundleRelease"
echo ""

