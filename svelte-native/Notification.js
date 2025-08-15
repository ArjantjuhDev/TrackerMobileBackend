import { Application, android as androidApp } from '@nativescript/core';

export function showNotification(title, text) {
  if (androidApp) {
    const context = androidApp.context;
    const NotificationManager = androidApp.getNativeApplication().getSystemService(androidApp.context.NOTIFICATION_SERVICE);
    const NotificationChannel = androidApp.context.getClass().forName('android.app.NotificationChannel');
    const NotificationCompat = androidApp.context.getClass().forName('androidx.core.app.NotificationCompat$Builder');
    const channelId = 'tracker_channel';
    // Create channel if needed (API 26+)
    // ...existing code...
    // Show notification
    // ...existing code...
  }
}
