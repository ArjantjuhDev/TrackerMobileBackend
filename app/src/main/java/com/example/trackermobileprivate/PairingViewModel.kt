package com.example.trackermobileprivate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class PairingViewModel : ViewModel() {
    private val _verificationState = MutableStateFlow(false)
    val verificationState: StateFlow<Boolean> = _verificationState

    private val _verificationError = MutableStateFlow("")
    val verificationError: StateFlow<String> = _verificationError

    fun startVerificationPolling(appCode: String, deviceName: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            var verified = false
            var attempts = 0
            while (!verified && attempts < 90) {
                try {
                    val url = URL("https://tracker-mobile-private.vercel.app/api/verify_app_code")
                    val body = "{\"app_code\": \"$appCode\"}"
                    val conn = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doOutput = true
                    withContext(Dispatchers.IO) { conn.outputStream.write(body.toByteArray()) }
                    val response = withContext(Dispatchers.IO) { conn.inputStream.bufferedReader().readText() }
                    val json = JSONObject(response)
                    if (json.optBoolean("verified", false)) {
                        _verificationState.value = true
                        verified = true
                        // Register device after successful verification
                        try {
                            val regUrl = URL("https://tracker-mobile-private.vercel.app/api/register_device")
                            val regBody = "{\"device_id\": \"$appCode\", \"device_name\": \"$deviceName\"}"
                            val regConn = withContext(Dispatchers.IO) { regUrl.openConnection() as HttpURLConnection }
                            regConn.requestMethod = "POST"
                            regConn.setRequestProperty("Content-Type", "application/json")
                            regConn.doOutput = true
                            withContext(Dispatchers.IO) { regConn.outputStream.write(regBody.toByteArray()) }
                            val regResponse = withContext(Dispatchers.IO) { regConn.inputStream.bufferedReader().readText() }
                            android.util.Log.i("PairingViewModel", "Register device response: $regResponse")
                        } catch (e: Exception) {
                            android.util.Log.e("PairingViewModel", "Register device failed: ${e.message}")
                        }
                        onSuccess(appCode)
                        break
                    } else {
                        _verificationError.value = json.optString("reason", "Verificatie mislukt.")
                    }
                } catch (e: Exception) {
                    _verificationError.value = "Fout bij verificatie: ${e.message}"
                }
                attempts++
                kotlinx.coroutines.delay(2000)
            }
            if (!verified) {
                _verificationError.value = "Verificatie is niet gelukt binnen de tijdslimiet. Probeer opnieuw."
            }
        }
    }
}
