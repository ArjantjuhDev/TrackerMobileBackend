package com.tracker.admin

import android.content.Context

object PairingStatusChecker {
    fun isPaired(context: Context): Boolean {
        val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("paired", false)
    }
}
