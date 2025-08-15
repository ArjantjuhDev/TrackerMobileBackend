# Tracker Mobile (Svelte NativeScript)

## Features
- Device pairing and registration
- Permissions (location, camera, internet)
- Location upload
- Error handling
- Backend API integration

## Getting Started

1. Install NativeScript CLI:
   ```bash
   npm install -g nativescript
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Run the app on Android:
   ```bash
   ns run android
   ```

## Next Steps
- Migrate device state, lock/unlock, wipe/lock polling, notifications, multi-device support, admin/member split, TOTP 2FA, etc.
- Remove legacy Android code when migration is complete.
