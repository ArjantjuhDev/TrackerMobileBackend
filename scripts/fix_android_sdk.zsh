#!/bin/zsh
# Fix Android SDK & Build Tools (zsh)

SDK_PATH="$HOME/Android/Sdk"
if [ -d "/mnt/c/Users/arjan/AppData/Local/Android/Sdk" ]; then
  SDK_PATH="/mnt/c/Users/arjan/AppData/Local/Android/Sdk"
fi

BUILD_TOOLS_VERSION="35.0.0"

echo "Verwijder corrupte build-tools ($BUILD_TOOLS_VERSION) ..."
rm -rf "$SDK_PATH/build-tools/$BUILD_TOOLS_VERSION"

echo "Installeer build-tools opnieuw ..."
"$SDK_PATH/cmdline-tools/latest/bin/sdkmanager" "build-tools;$BUILD_TOOLS_VERSION"

if [[ $? -eq 0 ]]; then
  echo "Build-tools $BUILD_TOOLS_VERSION succesvol opnieuw geïnstalleerd."
else
  echo "Fout bij installeren build-tools. Controleer je SDK Manager en internetverbinding."
fi

# Controleer local.properties
if [[ -f "local.properties" ]]; then
  echo "Controleer sdk.dir in local.properties:"
  grep sdk.dir local.properties
else
  echo "local.properties niet gevonden."
fi
