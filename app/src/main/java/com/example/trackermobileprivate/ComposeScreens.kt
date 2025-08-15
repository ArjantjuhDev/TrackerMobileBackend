package com.example.trackermobileprivate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trackermobileprivate.PairingViewModel
import android.content.Context

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.platform.LocalContext
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import android.util.Base64
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.util.concurrent.Executors
// QR-code imports removed
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import org.json.JSONObject

// QrCodeImage removed

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.LocalActivity

@Composable
fun MainScreen(
    pairingViewModel: PairingViewModel = viewModel(),
    locationText: MutableState<String>,
    fatalError: MutableState<String>,
    permissionGranted: MutableState<Boolean>,
    locationPermissionGranted: MutableState<Boolean>,
    cameraPermissionGranted: MutableState<Boolean>,
    downloadAndInstallApk: (String) -> Unit
) {
    val verificationState by pairingViewModel.verificationState.collectAsState()
    val verificationError by pairingViewModel.verificationError.collectAsState()
    val context = LocalContext.current
    var showLockOverlay by remember { mutableStateOf(false) }
    var lockCodeInput by remember { mutableStateOf("") }
    var lockCodeError by remember { mutableStateOf("") }
    var deviceTakenOver by remember { mutableStateOf(false) }
    var deviceLocked by remember { mutableStateOf(false) }
    // Poll device state from backend every 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                val deviceId = prefs.getString("device_id", "") ?: ""
                if (deviceId.isNotEmpty()) {
                    val url = URL("https://tracker-mobile-private.vercel.app/api/device_state?device_id=$deviceId")
                    val conn = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
                    conn.requestMethod = "GET"
                    val response = withContext(Dispatchers.IO) { conn.inputStream.bufferedReader().readText() }
                    val json = JSONObject(response)
                    val state = json.optJSONObject("state")
                    deviceLocked = state?.optBoolean("locked", false) ?: false
                    deviceTakenOver = state?.optBoolean("taken_over", false) ?: false
                }
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(5000)
        }
    }
    // Removed duplicate context declaration
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var lastPhotoTimestamp by remember { mutableStateOf(0L) }
    val photoIntervalMillis = 15 * 60 * 1000L // 15 minutes
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var codeValidated by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf("") }
    var codeShown by remember { mutableStateOf(false) }
    // Removed local verificationError; use ViewModel's state
    var deviceId by remember { mutableStateOf("") }
    var deviceRegistered by remember { mutableStateOf(false) }
    // Removed duplicate deviceLocked declaration
    var showSuccess by remember { mutableStateOf(false) }
    var deviceName by remember { mutableStateOf("") }
    var deviceNameError by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationPermissionGranted.value = granted
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraPermissionGranted.value = granted
    }

    // State triggers for coroutine launches
    var validateLockCodeTrigger by remember { mutableStateOf(false) }
    var pendingLockCode by remember { mutableStateOf("") }
    var registerDeviceTrigger by remember { mutableStateOf(false) }
    var pendingGeneratedCode by remember { mutableStateOf("") }

    // Lock code validation coroutine
    LaunchedEffect(validateLockCodeTrigger) {
        if (validateLockCodeTrigger && pendingLockCode.length == 6) {
            try {
                val url = URL("https://tracker-mobile-private.vercel.app/api/validate_code")
                val body = "{\"code\": \"$pendingLockCode\"}"
                val conn = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                withContext(Dispatchers.IO) { conn.outputStream.write(body.toByteArray()) }
                val response = withContext(Dispatchers.IO) { conn.inputStream.bufferedReader().readText() }
                val json = JSONObject(response)
                if (json.optBoolean("valid", false)) {
                    deviceLocked = false
                    showLockOverlay = false
                    lockCodeInput = ""
                } else {
                    lockCodeError = "Code is ongeldig of verlopen."
                }
            } catch (e: Exception) {
                lockCodeError = "Fout bij verbinden: ${e.message}"
            }
            validateLockCodeTrigger = false
        }
    }

    // Device registration coroutine
    LaunchedEffect(registerDeviceTrigger) {
        if (registerDeviceTrigger && pendingGeneratedCode.isNotEmpty()) {
            val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
            try {
                val deviceId = prefs.getString("device_id", "") ?: ""
                val url = URL("https://trackermobilebackend.onrender.com/api/register_device")
                val body = "{" +
                    "\"device_id\": \"$deviceId\"," +
                    "\"pair_code\": \"$pendingGeneratedCode\"}"
                val conn = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-api-key", "861396c20f3ffc10ae7af8def0783aeb")
                conn.doOutput = true
                withContext(Dispatchers.IO) { conn.outputStream.write(body.toByteArray()) }
                val response = withContext(Dispatchers.IO) { conn.inputStream.bufferedReader().readText() }
                val json = JSONObject(response)
                if (json.optString("status") == "registered") {
                    deviceRegistered = true
                } else {
                    codeError = "Registratie mislukt: ${json.optString("error", "Onbekende fout")}" 
                }
            } catch (e: Exception) {
                codeError = "Fout bij registratie: ${e.message}"
            }
            registerDeviceTrigger = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lock overlay logic
        if (deviceLocked && !deviceTakenOver) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC23263a)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("nu moet u eerst een code invoeren", color = Color.White, fontSize = 22.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = lockCodeInput,
                        onValueChange = { lockCodeInput = it },
                        label = { Text("Ontgrendelingscode") },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        lockCodeError = ""
                        if (lockCodeInput.length == 6) {
                            pendingLockCode = lockCodeInput
                            validateLockCodeTrigger = true
                        } else {
                            lockCodeError = "Voer een geldige 6-cijferige code in."
                        }
                    }, modifier = Modifier.fillMaxWidth(0.7f)) {
                        Text("Ontgrendel")
                    }
                    if (lockCodeError.isNotEmpty()) {
                        Text(lockCodeError, color = Color.Red, fontSize = 14.sp)
                    }
                }
            }
        }
        if (!locationPermissionGranted.value || !cameraPermissionGranted.value) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF23263a))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!locationPermissionGranted.value) {
                        Text("Locatierechten zijn vereist om je toestel te volgen.", color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            locationLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Geef locatierechten")
                        }
                    }
                    if (!cameraPermissionGranted.value) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Camerarechten zijn vereist om foto's te maken bij lock.", color = Color.Red, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            cameraLauncher.launch(android.Manifest.permission.CAMERA)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Geef camerarechten")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    Text("Toestel koppelen", fontSize = 28.sp)
    Spacer(modifier = Modifier.height(16.dp))
    if (showSuccess) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Koppeling geslaagd!", color = Color.White, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (deviceId.isNotEmpty()) {
                    Text("Device ID: $deviceId", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
    if (!codeValidated && !showSuccess) {
        if (!codeShown) {
            // Generate a random 6-digit code on first start
            generatedCode = (100000..999999).random().toString()
            codeShown = true
            val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("pair_code", generatedCode).apply()
            // Register device and code with backend
            pendingGeneratedCode = generatedCode
            registerDeviceTrigger = true
        }
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF23263a))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Koppelcode voor website:", color = Color.White, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(generatedCode, color = Color(0xFF388E3C), fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Voer deze code in op het dashboard om te koppelen.", color = Color.LightGray, fontSize = 14.sp)
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF23263a)).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = "Gekoppeld", tint = Color(0xFF388E3C), modifier = Modifier.size(60.dp))
                Spacer(modifier = Modifier.height(18.dp))
                Text("Toestel is succesvol gekoppeld!", color = Color(0xFF388E3C), fontSize = 22.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Je toestel is nu verbonden met de website en kan worden beheerd.", color = Color.LightGray, fontSize = 16.sp)
            }
        }
        // ...existing code for lock screen overlay and CameraX photo capture...
        // Takeover logic: if deviceTakenOver, remove all overlays and restrictions
        if (deviceTakenOver) {
            // Remove overlays, enable all features
            deviceLocked = false
            showLockOverlay = false
        }
    }
        Spacer(modifier = Modifier.height(16.dp))
        Text(locationText.value, fontSize = 16.sp, color = Color.DarkGray)
        if (fatalError.value.isNotEmpty()) {
            Text(fatalError.value, color = Color.Red, fontSize = 15.sp)
        }
    }
}
