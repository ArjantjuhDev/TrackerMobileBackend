package com.tracker.admin

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private var locationHandler: android.os.Handler? = null
    private var heartbeatHandler: android.os.Handler? = null
    private lateinit var connectionStatusText: TextView
    private var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient? = null
    private val locationIntervalMs = 30000L // elke 30 seconden
    private lateinit var mSocket: Socket
    private lateinit var componentName: ComponentName
    // Remove deprecated request code
    private var screenshotPermissionResult: Intent? = null
    private val screenshotLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            // Start MediaProjection en maak screenshot
            // Stuur screenshot via Socket.io (bijvoorbeeld als base64 string)
            val screenshotData = "base64string" // Vul in met echte data
            mSocket.emit("screen-data", screenshotData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        componentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        activateDeviceAdmin()
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)
        locationHandler = android.os.Handler(mainLooper)
        heartbeatHandler = android.os.Handler(mainLooper)

        val statusText = findViewById<TextView>(R.id.statusText)
        val permissionsText = findViewById<TextView>(R.id.permissionsText)
        val websiteActionsText = findViewById<TextView>(R.id.websiteActionsText)
        val lockBtn = findViewById<Button>(R.id.lockBtn)
        val wipeBtn = findViewById<Button>(R.id.wipeBtn)
        val screenshotBtn = findViewById<Button>(R.id.screenshotBtn)
        val feedbackText = findViewById<TextView>(R.id.feedbackText)

        // Show permissions status
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            permissionsText.text = "Alle rechten geaccepteerd."
            permissionsText.setTextColor(android.graphics.Color.parseColor("#388e3c"))
        } else {
            permissionsText.text = "Ontbrekende rechten: " + notGranted.joinToString(", ")
            permissionsText.setTextColor(android.graphics.Color.parseColor("#d32f2f"))
        }

        websiteActionsText.text = "Wat kan de website doen:\n- Vergrendelen\n- Ontgrendelen\n- Wis toestel\n- Screenshot maken\n- Locatie opvragen\n- Rechten opvragen"

        // Socket.IO logic
        try {
            mSocket = IO.socket("http://10.0.2.2:4000") // Gebruik je serveradres
            mSocket.connect()
            val prefs = getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
            val pairCode = prefs.getString("pair_code", null)
            if (pairCode != null) {
                mSocket.emit("join-room", pairCode)
            }
            mSocket.on(Socket.EVENT_CONNECT) { runOnUiThread {
                statusText.text = "Verbonden met server"
            } }
            mSocket.on(Socket.EVENT_DISCONNECT) { runOnUiThread {
                statusText.text = "Verbinding verbroken. Probeer opnieuw..."
            } }
            mSocket.on("lock-device") { args -> runOnUiThread {
                val unlockCode = if (args.isNotEmpty()) args[0] as? String else null
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("unlock_code", unlockCode)
                startActivity(intent)
                feedbackText.text = "Toestel vergrendeld via website."
            } }
            mSocket.on("unlock-device") { args -> runOnUiThread {
                val code = if (args.isNotEmpty()) args[0] as? String else null
                if (code != null) {
                    LockActivity.unlockCode = code
                    LockActivity.unlockRequested = true
                    feedbackText.text = "Ontgrendelverzoek ontvangen van website."
                }
            } }
            mSocket.on("paired") { runOnUiThread {
                feedbackText.text = "Toestel succesvol gekoppeld!"
                requestAllPermissions()
            } }
            mSocket.on("heartbeat") { runOnUiThread {
                statusText.text = "Live verbinding met server"
            } }
            mSocket.on("request-permissions") { runOnUiThread {
                requestAllPermissions()
                feedbackText.text = "Website vraagt om rechten."
            } }
            mSocket.on("wipe-device") { runOnUiThread {
                wipeDevice()
                feedbackText.text = "Wis-verzoek ontvangen van website."
            } }
            mSocket.on("request-screenshot") { runOnUiThread {
                requestScreenshotPermission()
                feedbackText.text = "Screenshot-verzoek ontvangen van website."
            } }
        } catch (e: Exception) {
            e.printStackTrace()
            feedbackText.text = "Socket.IO fout: ${e.message}"
        }
        startLocationUpdates()
        startHeartbeat()

        // Button logic for manual testing
        lockBtn.setOnClickListener {
            lockDevice()
            feedbackText.text = "Toestel handmatig vergrendeld."
        }
        wipeBtn.setOnClickListener {
            wipeDevice()
            feedbackText.text = "Toestel handmatig gewist."
        }
        screenshotBtn.setOnClickListener {
            requestScreenshotPermission()
            feedbackText.text = "Screenshot handmatig aangevraagd."
        }
    }
    private fun startHeartbeat() {
        heartbeatHandler?.post(object : Runnable {
            override fun run() {
                try {
                    mSocket.emit("heartbeat", JSONObject())
                } catch (_: Exception) {}
                heartbeatHandler?.postDelayed(this, 30000L)
            }
        })
    }

    private fun startLocationUpdates() {
        locationHandler?.post(object : Runnable {
            override fun run() {
                try {
                    fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                        if (location != null) {
                            val locObj = org.json.JSONObject()
                            locObj.put("lat", location.latitude)
                            locObj.put("lon", location.longitude)
                            mSocket.emit("location-update", locObj)
                        }
                    }
                } catch (_: Exception) {}
                locationHandler?.postDelayed(this, locationIntervalMs)
            }
        })
    }

    private fun activateDeviceAdmin() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isAdminActive(componentName)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Nodig voor remote control")
            startActivity(intent)
        }
    }

    private fun requestAllPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            Toast.makeText(this, "Alle rechten al geaccepteerd!", Toast.LENGTH_SHORT).show()
            mSocket.emit("permissions-accepted")
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            mSocket.emit("permissions-accepted")
            Toast.makeText(this, "Alle rechten geaccepteerd! App gaat naar achtergrond.", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true)
            finish()
        } else {
            Toast.makeText(this, "Niet alle rechten geaccepteerd!", Toast.LENGTH_LONG).show()
        }
    }

    private fun onPermissionsAccepted() {
        Toast.makeText(this, "Remote control geactiveerd!", Toast.LENGTH_SHORT).show()
    }

    private fun lockDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(componentName)) {
            dpm.lockNow()
            Toast.makeText(this, "Toestel vergrendeld!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Device Admin niet actief!", Toast.LENGTH_LONG).show()
        }
    }

    private fun wipeDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (dpm.isAdminActive(componentName)) {
            dpm.wipeData(0)
            Toast.makeText(this, "Toestel gewist!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Device Admin niet actief!", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestScreenshotPermission() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        screenshotLauncher.launch(intent)
    }

    // Removed deprecated onActivityResult for screenshot
}
