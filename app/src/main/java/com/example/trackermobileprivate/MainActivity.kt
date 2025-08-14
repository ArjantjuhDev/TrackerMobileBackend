package com.example.trackermobileprivate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.example.trackermobileprivate.MainScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    fun startPeriodicUpload() {
        val context = this
        handler.post(object : Runnable {
            override fun run() {
                try {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                            if (location != null) {
                                locationText.value = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                                uploadLocation(location.latitude, location.longitude)
                            }
                        }
                    }
                } catch (e: Exception) {
                    fatalError.value = "Fout bij het uploaden van locatie: ${e.message}"
                }
                handler.postDelayed(this, uploadIntervalMs)
            }
        })
    }
    private val locationPermissionGranted = mutableStateOf(false)
    private val cameraPermissionGranted = mutableStateOf(false)
    private val permissionGranted = mutableStateOf(false) // true if all required
    private val fatalError = mutableStateOf("")
    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.CAMERA
    )
    private val locationText = mutableStateOf("")
    var fusedLocationClient: FusedLocationProviderClient? = null
    private val handler = Handler(Looper.getMainLooper())
    private val uploadIntervalMs = 60_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            fatalError.value = "Fout bij initialisatie locatie: ${e.message}"
        }
        setContent {
            MaterialTheme {
                MainScreen(
                    locationText = locationText,
                    fatalError = fatalError,
                    permissionGranted = permissionGranted,
                    locationPermissionGranted = locationPermissionGranted,
                    cameraPermissionGranted = cameraPermissionGranted,
                    downloadAndInstallApk = ::downloadAndInstallApk
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Always clear pairing state on start
        val prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE)
        prefs.edit().remove("paired").apply()
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        locationPermissionGranted.value = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        cameraPermissionGranted.value = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        permissionGranted.value = notGranted.isEmpty()
        if (notGranted.isNotEmpty()) {
            requestAllPermissions()
        }
        // Do NOT start background tasks until pairing is complete
    }

    // QR-code validation removed

    private fun downloadAndInstallApk(apkUrl: String) {
        val context = this
        Thread {
            try {
                val url = URL(apkUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val inputStream = conn.inputStream
                val file = java.io.File(context.getExternalFilesDir(null), "update.apk")
                file.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(android.net.Uri.fromFile(file), "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    fatalError.value = "Update is mislukt: ${e.message}"
                }
            }
        }.start()
    }

    private fun requestAllPermissions() {
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            permissionGranted.value = allGranted
            locationPermissionGranted.value = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            cameraPermissionGranted.value = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            // Only set permission state here. Do not start background tasks until pairing is complete.
            // Background tasks are started after pairing in ComposeScreens.kt
        }
        val notGranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            launcher.launch(notGranted.toTypedArray())
        }
    }

    fun uploadLocation(lat: Double, lon: Double) {
        Thread {
            val deviceId = getTrackerDeviceId()
            val timestamp = System.currentTimeMillis()
            val json = "{" +
                    "\"device_id\":\"$deviceId\"," +
                    "\"lat\":$lat," +
                    "\"lon\":$lon," +
                    "\"timestamp\":\"$timestamp\"}"
            android.util.Log.i("MainActivity", "Upload locatie: device_id=$deviceId, lat=$lat, lon=$lon, timestamp=$timestamp")
            try {
                val url = URL("https://tracker-mobile-private.vercel.app/api/location")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-API-Key", "jouw_geheime_api_key")
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(json) }
                val responseCode = conn.responseCode
                android.util.Log.i("MainActivity", "Upload locatie response: $responseCode")
                conn.inputStream.close()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Upload is mislukt: ${e.message}")
            }
        }.start()
    }

    private fun getTrackerDeviceId(): String {
        val prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE)
        val paired = prefs.getBoolean("paired", false)
        val id = prefs.getString("device_id", null)
        if (!paired || id == null) {
            android.util.Log.w("MainActivity", "Device not paired, blocking location upload.")
            return ""
        }
        android.util.Log.i("MainActivity", "Using device_id for upload: $id")
        return id
    }
}
