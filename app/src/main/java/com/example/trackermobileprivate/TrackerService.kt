package com.example.trackermobileprivate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

class TrackerService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val handler = Handler()
    private val uploadIntervalMs = 60_000L // elke minuut
    private val wipeCheckIntervalMs = 15_000L // elke 15 seconden
    private var isLocked = false
    private val API_KEY = "jouw_geheime_api_key" // Zet hier je API key
    private val SERVER_URL = "http://10.0.2.2:5000" // Gebruik 10.0.2.2 voor emulator

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
        startPeriodicUpload()
        startWipeCheck()
    }

    private fun startForegroundService() {
        val channelId = "tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Tracker Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracker actief")
            .setContentText("Locatie wordt bijgehouden")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notification)
    }

    private fun uploadLocation(lat: Double, lon: Double) {
        Thread {
            try {
                val url = URL("$SERVER_URL/api/location")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-API-Key", API_KEY)
                conn.doOutput = true
                val json = "{\"lat\":$lat,\"lon\":$lon,\"timestamp\":\"${System.currentTimeMillis()}\"}"
                OutputStreamWriter(conn.outputStream).use { it.write(json) }
                conn.inputStream.close()
            } catch (_: Exception) {}
        }.start()
    }

    private fun startPeriodicUpload() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    val permission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@TrackerService,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    if (permission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                uploadLocation(location.latitude, location.longitude)
                            }
                        }
                    }
                } catch (_: Exception) {}
                handler.postDelayed(this, uploadIntervalMs)
            }
        })
    }

    private fun startWipeCheck() {
        handler.post(object : Runnable {
            override fun run() {
                Thread {
                    try {
                        val url = URL("$SERVER_URL/api/location")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "GET"
                        conn.setRequestProperty("X-API-Key", API_KEY)
                        val response = conn.inputStream.bufferedReader().readText()
                        if (response.contains("\"wipe\":true") && !isLocked) {
                            isLocked = true
                            // TODO: Trigger lock UI/logic (bijv. via BroadcastReceiver)
                        }
                    } catch (_: Exception) {}
                }.start()
                handler.postDelayed(this, wipeCheckIntervalMs)
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
