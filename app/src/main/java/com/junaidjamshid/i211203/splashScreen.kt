package com.junaidjamshid.i211203

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.junaidjamshid.i211203.HelperClasses.ApiService
import com.junaidjamshid.i211203.HelperClasses.NetworkUtils
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashScreen : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService
    private val TAG = "SplashScreen"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash_screen)

        sessionManager = SessionManager(this)
        apiService = ApiService(this)

        // Wait for 5 seconds before checking user session
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 5000) // 5-second delay
    }

    private fun checkUserSession() {
        Log.d(TAG, "Checking user session...")

        if (sessionManager.isLoggedIn()) {
            Log.d(TAG, "User is logged in, checking token validity")

            if (sessionManager.isTokenValid()) {
                val userId = sessionManager.getUserId()

                if (userId != null) {
                    handleLoggedInUser(userId)
                } else {
                    Log.e(TAG, "User ID is null despite session being active")
                    sessionManager.logout()
                    navigateToLogin()
                }
            } else {
                Log.d(TAG, "Token is invalid or expired")
                sessionManager.logout()
                navigateToLogin("Session expired. Please login again.")
            }
        } else {
            Log.d(TAG, "User is not logged in")
            navigateToLogin()
        }
    }

    private fun handleLoggedInUser(userId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Loading user profile for user: $userId")
                // This will work in both online and offline modes
                val userResult = apiService.getUserProfile(userId)

                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()

                    if (user != null) {
                        // Check if this is a first-time user
                        if (user.bio.isEmpty() && user.profilePicture == null || sessionManager.isFirstTimeUser()) {
                            Log.d(TAG, "First time user detected, navigating to profile setup")
                            navigateToProfileSetup()
                        } else {
                            Log.d(TAG, "User profile loaded successfully, navigating to main activity")
                            navigateToMainActivity()
                        }

                        // Try to sync in background if network is available
                        val isNetworkAvailable = NetworkUtils.isNetworkAvailable(this@SplashScreen)
                        if (isNetworkAvailable) {
                            Log.d(TAG, "Network available, attempting background sync")
                            withContext(Dispatchers.IO) {
                                try {
                                    apiService.syncUserData(userId)
                                    Log.d(TAG, "Background sync successful")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Background sync failed: ${e.message}", e)
                                    // Silent fail - we've already loaded offline data
                                }
                            }
                        } else {
                            Log.d(TAG, "No network available, skipping sync")
                        }
                    } else {
                        Log.e(TAG, "User profile is null despite successful result")
                        sessionManager.logout()
                        navigateToLogin("User data not found. Please login again.")
                    }
                } else {
                    // If we can't get profile data at all, logout and go to login
                    val error = userResult.exceptionOrNull()?.message ?: "Failed to load user data"
                    Log.e(TAG, "Failed to load user profile: $error")
                    sessionManager.logout()
                    navigateToLogin("Session expired. Please login again.")
                }
            } catch (e: Exception) {
                // If there's an error, show error and navigate to login
                Log.e(TAG, "Error during session check: ${e.message}", e)
                sessionManager.logout()
                navigateToLogin("An error occurred. Please login again.")
            }
        }
    }

    private fun navigateToLogin(message: String? = null) {
        message?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

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