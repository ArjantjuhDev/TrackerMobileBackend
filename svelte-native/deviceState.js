import { writable } from 'svelte/store';

export const deviceLocked = writable(false);
export const deviceTakenOver = writable(false);
export const lockCodeError = writable('');

const BACKEND_URL = 'https://tracker-mobile-backend.onrender.com';

export async function validateLockCode(deviceId, lockCode) {
  try {
    const response = await fetch(`${BACKEND_URL}/api/validate_code`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ device_id: deviceId, code: lockCode })
    });
    const json = await response.json();
    if (json.valid) {
      deviceLocked.set(false);
      lockCodeError.set('');
    } else {
      lockCodeError.set('Code is ongeldig of verlopen.');
    }
  } catch (e) {
    lockCodeError.set('Fout bij verbinden: ' + e.message);
  }
}

export async function pollDeviceState(deviceId) {
  try {
    const response = await fetch(`${BACKEND_URL}/api/device_state?device_id=${deviceId}`);
    const json = await response.json();
    deviceLocked.set(json.state?.locked || false);
    deviceTakenOver.set(json.state?.taken_over || false);
  } catch (e) {
    // Optionally handle error
  }
}

export function startWipeLockPolling(deviceId) {
  setInterval(() => {
    pollDeviceState(deviceId);
  }, 15000); // Poll every 15 seconds
}
