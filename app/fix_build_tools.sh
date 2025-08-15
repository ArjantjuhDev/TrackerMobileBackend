#!/bin/bash
# Remove corrupted Build Tools 35.0.0 and reinstall

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ -z "$ANDROID_HOME" ]; then
  echo "ANDROID_HOME is not set. Please set it to your Android SDK path."
  exit 1
fi

echo "Removing Build Tools 35.0.0..."
"$SDKMANAGER" --uninstall "build-tools;35.0.0"

echo "Reinstalling Build Tools 35.0.0..."
"$SDKMANAGER" --install "build-tools;35.0.0"

echo "Done. Please verify installation in SDK Manager or re-run your build."