package com.tracker.admin

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LockActivity : AppCompatActivity() {
    companion object {
        var unlockCode: String = ""
        var unlockRequested: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setContentView(R.layout.activity_lock)

        val infoText = findViewById<TextView>(R.id.lockInfo)
        val codeInput = findViewById<EditText>(R.id.codeInput)
        val unlockBtn = findViewById<Button>(R.id.unlockBtn)

        // Get unlock code from intent
        val intentCode = intent.getStringExtra("unlock_code")
        if (!intentCode.isNullOrEmpty()) {
            unlockCode = intentCode
        }
        infoText.text = "Toestel is vergrendeld. Voer de code in om te ontgrendelen.\nCode: $unlockCode"

        unlockBtn.setOnClickListener {
            val code = codeInput.text.toString()
            if (code == unlockCode) {
                finish()
            } else {
                infoText.text = "Onjuiste code. Probeer opnieuw.\nCode: $unlockCode"
            }
        }
    }

    override fun onBackPressed() {
        // Blokkeer back
    }
}
