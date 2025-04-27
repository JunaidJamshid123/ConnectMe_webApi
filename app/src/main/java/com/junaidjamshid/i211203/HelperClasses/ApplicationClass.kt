package com.junaidjamshid.i211203.HelperClasses

// ApplicationClass.kt
import android.app.Application
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class ApplicationClass : Application() {
    override fun onCreate() {
        super.onCreate()

        // Enable verbose logging for debugging (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        // Initialize with your OneSignal App ID - replace with your actual OneSignal App ID
        OneSignal.initWithContext(this, "5b6e61d3-d918-43cc-9e42-f535c877813c")
        // Request permission for push notifications
        // Set external user ID when user is logged in
        val currentUserId = getUserIdFromSharedPrefs() // Create this method to get user ID
        if (!currentUserId.isNullOrEmpty()) {
            OneSignal.login(currentUserId)
        }
    }

    private fun getUserIdFromSharedPrefs(): String? {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        return prefs.getString("user_id", null)
    }
}