import { writable } from 'svelte/store';
import { Application, android as androidApp } from '@nativescript/core';

export const permissionGranted = writable(false);
export const locationPermissionGranted = writable(false);
export const cameraPermissionGranted = writable(false);
export const fatalError = writable('');
export const locationText = writable('');

// Request permissions
export async function requestAllPermissions() {
  try {
    const permissions = [
      'android.permission.ACCESS_FINE_LOCATION',
      'android.permission.ACCESS_COARSE_LOCATION',
      'android.permission.INTERNET',
      'android.permission.ACCESS_BACKGROUND_LOCATION',
      'android.permission.CAMERA'
    ];
    for (const perm of permissions) {
      await androidApp.context.requestPermissions([perm], 0);
    }
    // Check permissions
    locationPermissionGranted.set(true); // TODO: check actual permission
    cameraPermissionGranted.set(true);   // TODO: check actual permission
    permissionGranted.set(true);        // TODO: check all
  } catch (e) {
    fatalError.set('Fout bij permissies: ' + e.message);
  }
}

// Location upload
export async function uploadLocation(lat, lon) {
  try {
    const deviceId = 'TODO_DEVICE_ID'; // Replace with actual device ID logic
    const timestamp = Date.now();
    const json = {
      device_id: deviceId,
      lat,
      lon,
      timestamp
    };
    const response = await fetch('https://tracker-mobile-backend.onrender.com/api/location', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': 'jouw_geheime_api_key'
      },
      body: JSON.stringify(json)
    });
    locationText.set(`Lat: ${lat}, Lon: ${lon}`);
  } catch (e) {
    fatalError.set('Fout bij het uploaden van locatie: ' + e.message);
  }
}
