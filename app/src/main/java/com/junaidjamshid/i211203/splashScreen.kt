package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.junaidjamshid.i211203.HelperClasses.ApiService
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreen : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)

        sessionManager = SessionManager(this)
        apiService = ApiService(this)

        // Wait for 5 seconds before navigating to the next screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 5000) // 5-second delay as requested
    }

    private fun checkUserSession() {
        if (sessionManager.isLoggedIn()) {
            // Check if token is valid
            if (sessionManager.isTokenValid()) {
                val userId = sessionManager.getUserId()

                if (userId != null) {
                    // Try to sync user data if possible, then navigate
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            // Get user profile (will work in both online and offline modes)
                            val userResult = apiService.getUserProfile(userId)

                            if (userResult.isSuccess) {
                                // If this is a first-time user, navigate to profile setup
                                if (sessionManager.isFirstTimeUser()) {
                                    navigateToProfileSetup()
                                } else {
                                    // Otherwise, go to main activity
                                    navigateToMainActivity()
                                }

                                // Try to sync in background if network is available
                                withContext(Dispatchers.IO) {
                                    try {
                                        apiService.syncUserData(userId)
                                    } catch (e: Exception) {
                                        // Silent fail - we've already loaded offline data
                                    }
                                }
                            } else {
                                // If we can't get profile data, logout and go to login
                                sessionManager.logout()
                                navigateToLogin()
                                Toast.makeText(
                                    this@SplashScreen,
                                    "Session expired. Please login again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            // If there's an error, show error and navigate to login
                            sessionManager.logout()
                            navigateToLogin()
                            Toast.makeText(
                                this@SplashScreen,
                                "An error occurred: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // No user ID in session, go to login
                    sessionManager.logout()
                    navigateToLogin()
                }
            } else {
                // Token expired, logout and go to login
                sessionManager.logout()
                navigateToLogin()
                Toast.makeText(
                    this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Not logged in, navigate to login
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginScreem::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, EditProfile::class.java)
        startActivity(intent)
        finish()
    }
}