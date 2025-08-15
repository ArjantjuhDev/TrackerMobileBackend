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
    private var mediaProjection: android.media.projection.MediaProjection? = null
    private var screenshotHandler: android.os.Handler? = null
    private var screenshotIntervalMs = 2000L // elke 2 seconden
    private val screenshotLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            screenshotPermissionResult = data
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(result.resultCode, data!!)
            startPeriodicScreenshots()
        }
    }

    private fun startPeriodicScreenshots() {
        if (mediaProjection == null) return
        screenshotHandler = android.os.Handler(mainLooper)
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val dpi = resources.displayMetrics.densityDpi
        val imageReader = android.media.ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        val virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
            width, height, dpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, screenshotHandler
        )
        screenshotHandler?.post(object : Runnable {
            override fun run() {
                val image = imageReader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width
                    val bitmap = android.graphics.Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()
                    // Crop to actual screen size
                    val croppedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height)
                    val outputStream = java.io.ByteArrayOutputStream()
                    croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, outputStream)
                    val base64 = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
                    mSocket.emit("screen-data", base64)
                }
                screenshotHandler?.postDelayed(this, screenshotIntervalMs)
            }
        })
    }

    private lateinit var feedbackText: TextView
    private val mainHandler = android.os.Handler(mainLooper)

    private fun setFeedback(msg: String, success: Boolean = true) {
        mainHandler.post {
            feedbackText.text = msg
            feedbackText.setBackgroundColor(if (success) android.graphics.Color.parseColor("#23263a") else android.graphics.Color.parseColor("#2c2f3c"))
            feedbackText.setTextColor(if (success) android.graphics.Color.parseColor("#388e3c") else android.graphics.Color.parseColor("#d32f2f"))
        }
    }

    private fun checkAndRequestPermissions(lockBtn: Button, wipeBtn: Button, screenshotBtn: Button, permissionsText: TextView) {
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
            val launcher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                if (!allGranted) {
                    setFeedback("Je moet alle rechten accepteren om de app te gebruiken.", false)
                    permissionsText.text = "Ontbrekende rechten: " + notGranted.joinToString(", ")
                    permissionsText.setTextColor(android.graphics.Color.parseColor("#d32f2f"))
                    lockBtn.isEnabled = false
                    wipeBtn.isEnabled = false
                    screenshotBtn.isEnabled = false
                } else {
                    setFeedback("Alle rechten geaccepteerd. App verdwijnt naar achtergrond.", true)
                    permissionsText.text = "Alle rechten geaccepteerd."
                    permissionsText.setTextColor(android.graphics.Color.parseColor("#388e3c"))
                    lockBtn.isEnabled = true
                    wipeBtn.isEnabled = true
                    screenshotBtn.isEnabled = true
                    // Move app to background and finish
                    moveTaskToBack(true)
                    finish()
                }
            }
            launcher.launch(notGranted.toTypedArray())
        } else {
            setFeedback("Alle rechten geaccepteerd. App verdwijnt naar achtergrond.", true)
            permissionsText.text = "Alle rechten geaccepteerd."
            permissionsText.setTextColor(android.graphics.Color.parseColor("#388e3c"))
            lockBtn.isEnabled = true
            wipeBtn.isEnabled = true
            screenshotBtn.isEnabled = true
            moveTaskToBack(true)
            finish()
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
        feedbackText = findViewById<TextView>(R.id.feedbackText)

        val prefs = getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        val isPaired = prefs.getBoolean("paired", false)

        // Block all actions until paired
        if (!isPaired) {
            statusText.text = "Status: Niet gekoppeld"
            statusText.setTextColor(android.graphics.Color.parseColor("#d32f2f"))
            setFeedback("Koppel eerst het toestel via de website.", false)
            lockBtn.isEnabled = false
            wipeBtn.isEnabled = false
            screenshotBtn.isEnabled = false
            permissionsText.text = "Wacht op koppeling..."
            permissionsText.setTextColor(android.graphics.Color.parseColor("#d32f2f"))
        } else {
            statusText.text = "Status: Gekoppeld"
            statusText.setTextColor(android.graphics.Color.parseColor("#388e3c"))
            setFeedback("Toestel is gekoppeld. Rechten worden nu gecontroleerd.", true)
            checkAndRequestPermissions(lockBtn, wipeBtn, screenshotBtn, permissionsText)
        }

        websiteActionsText.text = "Website acties: Vergrendelen, Ontgrendelen, Wissen, Screenshot, Locatie, Rechten"

        try {
            mSocket = IO.socket("http://10.0.2.2:4000") // Gebruik je serveradres
            mSocket.connect()
            val pairCode = prefs.getString("pair_code", null)
            if (pairCode != null) {
                mSocket.emit("join-room", pairCode)
            }
            mSocket.on("paired") { mainHandler.post {
                setFeedback("Toestel succesvol gekoppeld! Rechten worden nu gecontroleerd.", true)
                statusText.text = "Status: Gekoppeld"
                statusText.setTextColor(android.graphics.Color.parseColor("#388e3c"))
                prefs.edit().putBoolean("paired", true).apply()
                checkAndRequestPermissions(lockBtn, wipeBtn, screenshotBtn, permissionsText)
            } }
            mSocket.on(Socket.EVENT_CONNECT) { mainHandler.post {
                statusText.text = "Verbonden met server"
                setFeedback("Verbonden met server.")
            } }
            mSocket.on(Socket.EVENT_DISCONNECT) { mainHandler.post {
                statusText.text = "Verbinding verbroken. Probeer opnieuw..."
                setFeedback("Verbinding verbroken.")
            } }
            mSocket.on("lock-device") { args -> mainHandler.post {
                val unlockCode = if (args.isNotEmpty()) args[0] as? String else null
                val intent = Intent(this, LockActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putExtra("unlock_code", unlockCode)
                startActivity(intent)
                setFeedback("Toestel vergrendeld via website.")
            } }
            mSocket.on("unlock-device") { args -> mainHandler.post {
                val code = if (args.isNotEmpty()) args[0] as? String else null
                if (code != null) {
                    LockActivity.unlockCode = code
                    LockActivity.unlockRequested = true
                    setFeedback("Ontgrendelverzoek ontvangen van website.")
                }
            } }
            mSocket.on("heartbeat") { mainHandler.post {
                statusText.text = "Live verbinding met server"
                setFeedback("Live verbinding met server.")
            } }
            mSocket.on("request-permissions") { mainHandler.post {
                checkAndRequestPermissions(lockBtn, wipeBtn, screenshotBtn, permissionsText)
                setFeedback("Website vraagt om rechten.")
            } }
            mSocket.on("wipe-device") { mainHandler.post {
                wipeDevice()
                setFeedback("Wis-verzoek ontvangen van website.")
            } }
            mSocket.on("request-screenshot") { mainHandler.post {
                requestScreenshotPermission()
                setFeedback("Screenshot-verzoek ontvangen van website.")
            } }
        } catch (e: Exception) {
            e.printStackTrace()
            setFeedback("Socket.IO fout: ${e.message}")
        }
        startLocationUpdates()
        startHeartbeat()

        // Button logic for manual testing
        lockBtn.setOnClickListener {
            lockDevice()
            setFeedback("Toestel handmatig vergrendeld.", true)
        }
        wipeBtn.setOnClickListener {
            wipeDevice()
            setFeedback("Toestel handmatig gewist.", true)
        }
        screenshotBtn.setOnClickListener {
            requestScreenshotPermission()
            setFeedback("Live schermweergave gestart.", true)
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
