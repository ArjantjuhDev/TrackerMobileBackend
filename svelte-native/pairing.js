import { writable } from 'svelte/store';

export const verificationState = writable(false);
export const verificationError = writable('');

const BACKEND_URL = 'https://tracker-mobile-backend.onrender.com';

export async function startVerificationPolling(appCode, deviceName, onSuccess) {
  let verified = false;
  let attempts = 0;
  while (!verified && attempts < 90) {
    try {
      const response = await fetch(`${BACKEND_URL}/api/verify_app_code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ app_code: appCode })
      });
      const json = await response.json();
      if (json.verified) {
        verificationState.set(true);
        verified = true;
        // Register device after successful verification
        try {
          const regResponse = await fetch(`${BACKEND_URL}/api/register_device`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ device_id: appCode, device_name: deviceName })
          });
          const regJson = await regResponse.json();
          console.log('Register device response:', regJson);
        } catch (e) {
          console.error('Register device failed:', e.message);
        }
        if (onSuccess) onSuccess(appCode);
        break;
      } else {
        verificationError.set(json.reason || 'Verificatie mislukt.');
      }
    } catch (e) {
      verificationError.set('Fout bij verificatie: ' + e.message);
    }
    attempts++;
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  if (!verified) {
    verificationError.set('Verificatie is niet gelukt binnen de tijdslimiet. Probeer opnieuw.');
  }
}
