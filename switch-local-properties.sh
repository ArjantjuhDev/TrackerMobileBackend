#!/bin/bash
# Usage: ./switch-local-properties.sh wsl|win

if [ "$1" = "wsl" ]; then
  cp local.properties.wsl local.properties
  echo "Switched to WSL SDK path."
elif [ "$1" = "win" ]; then
  cp local.properties.win local.properties
  echo "Switched to Windows SDK path."
else
  echo "Usage: $0 wsl|win"
fi
