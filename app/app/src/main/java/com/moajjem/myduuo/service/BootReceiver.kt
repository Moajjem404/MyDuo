package com.moajjem.myduuo.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.moajjem.myduuo.data.AppRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received boot action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val appRepository = AppRepository.getInstance(context)
            if (appRepository.isSetupComplete()) {
                Log.d("BootReceiver", "Setup is complete, starting ActivityMonitorService...")
                val serviceIntent = Intent(context, ActivityMonitorService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to start service on boot: ${e.message}", e)
                }
            } else {
                Log.d("BootReceiver", "Setup is not complete, skipping service startup.")
            }
        }
    }
}
