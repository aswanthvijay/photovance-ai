#!/bin/bash
set -e

echo "======================================================================"
echo "    PHOTOVANCE AI - APPLE IOS NATIVE BUILD & EXPORT SCRIPT"
echo "======================================================================"
echo ""

cd "$(dirname "$0")"

echo "[1/3] Cleaning previous builds..."
rm -rf PhotovanceAI.xcarchive Payload PhotovanceAI-Release.ipa

echo "[2/3] Building Photovance AI Xcode Archive..."
xcodebuild archive \
    -project PhotovanceAI.xcodeproj \
    -scheme PhotovanceAI \
    -configuration Release \
    -sdk iphoneos \
    -archivePath PhotovanceAI.xcarchive \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGN_IDENTITY=""

echo "[3/3] Packaging PhotovanceAI-Release.ipa..."
mkdir -p Payload
cp -r PhotovanceAI.xcarchive/Products/Applications/PhotovanceAI.app Payload/
zip -r PhotovanceAI-Release.ipa Payload
rm -rf Payload

echo ""
echo "======================================================================"
echo "Build Successful!"
echo "Release IPA Location: $(pwd)/PhotovanceAI-Release.ipa"
echo "======================================================================"
