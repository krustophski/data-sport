package com.krustophski.data.sport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        Log.d("BootReceiver", "Boot completed; starting foreground service")
        val serviceIntent = Intent(context, BootForegroundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
