package com.moajjem.myduuo.data

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

object VersionChecker {
    private const val TAG = "VersionChecker"
    private val client = OkHttpClient()
    private const val VERSION_URL = "https://raw.githubusercontent.com/Moajjem404/MyDuo/refs/heads/main/v/v.txt"

    /**
     * Checks if a newer version is available.
     * Returns the newer version string if available, or null if no update is needed.
     */
    fun checkVersion(currentVersion: Double): String? {
        val request = Request.Builder()
            .url(VERSION_URL)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()?.trim() ?: return null
                    if (bodyString.isEmpty()) return null
                    
                    var remoteVersionStr = bodyString
                    if (bodyString.contains("version=")) {
                        remoteVersionStr = bodyString.substringAfter("version=").trim()
                    }
                    remoteVersionStr = remoteVersionStr.split("\n")[0].trim()
                    
                    val remoteVersion = remoteVersionStr.toDoubleOrNull()
                    if (remoteVersion != null && remoteVersion > currentVersion) {
                        remoteVersionStr
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking version: ${e.message}")
            null
        }
    }
}
