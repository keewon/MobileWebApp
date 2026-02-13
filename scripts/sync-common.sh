#!/bin/bash
# sync-common.sh - Sync common/ web content to iOS and Android projects
#
# Usage: ./scripts/sync-common.sh
#
# iOS: Copies to ios/MobileWebApp/common/ (add as folder reference in Xcode)
# Android: Copies to android/app/src/main/assets/common/

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
COMMON_DIR="$ROOT_DIR/common"

if [ ! -d "$COMMON_DIR" ]; then
    echo "Error: common/ directory not found at $COMMON_DIR"
    exit 1
fi

# Sync to iOS
IOS_DEST="$ROOT_DIR/ios/MobileWebApp/common"
echo "Syncing to iOS: $IOS_DEST"
rm -rf "$IOS_DEST"
cp -R "$COMMON_DIR" "$IOS_DEST"
echo "  Done."

# Sync to Android
ANDROID_DEST="$ROOT_DIR/android/app/src/main/assets/common"
echo "Syncing to Android: $ANDROID_DEST"
rm -rf "$ANDROID_DEST"
mkdir -p "$(dirname "$ANDROID_DEST")"
cp -R "$COMMON_DIR" "$ANDROID_DEST"
echo "  Done."

echo ""
echo "Sync complete. Files synced:"
find "$IOS_DEST" -type f | sed "s|$ROOT_DIR/||" | sort
echo "---"
find "$ANDROID_DEST" -type f | sed "s|$ROOT_DIR/||" | sort
