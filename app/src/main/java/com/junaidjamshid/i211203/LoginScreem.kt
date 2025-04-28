package com.junaidjamshid.i211203

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.junaidjamshid.i211203.HelperClasses.ApiService
import com.junaidjamshid.i211203.HelperClasses.NetworkUtils
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

class LoginScreem : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login_screem)

        sessionManager = SessionManager(this)
        apiService = ApiService(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Logging in...")
        progressDialog.setCancelable(false)

        val registerLink = findViewById<TextView>(R.id.registerLink)
        val email = findViewById<EditText>(R.id.Email)
        val password = findViewById<EditText>(R.id.Password)
        val loginBtn = findViewById<Button>(R.id.LoginBtn)

        registerLink.setOnClickListener {
            val intent = Intent(this, SignUpScreen::class.java)
            startActivity(intent)
        }

        loginBtn.setOnClickListener {
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(emailInput, passwordInput)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                // First check if we have internet
                val isOnline = withContext(Dispatchers.IO) {
                    NetworkUtils.isNetworkAvailable(this@LoginScreem)
                }

                val result = apiService.loginUser(email, password)

                progressDialog.dismiss()

                if (result.isSuccess) {
                    val user = result.getOrNull()

                    if (user != null) {
                        // If online, attempt to sync the latest user data
                        if (isOnline) {
                            try {
                                apiService.syncUserData(user.userId)
                                // Log the success but don't block the login flow
                                Log.d("LoginScreen", "User data synced successfully")
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Sync failed but continuing: ${e.message}")
                                // Don't block login if sync fails
                            }
                        }

                        // Check if this is the first login for the user
                        if (user.bio.isEmpty() && user.profilePicture == null) {
                            sessionManager.setFirstTimeUser(true)
                            navigateToProfileSetup()
                        } else {
                            sessionManager.setFirstTimeUser(false)
                            navigateToMainActivity()
                        }

                        Toast.makeText(this@LoginScreem, "Login successful!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Login failed"
                    Toast.makeText(this@LoginScreem, error, Toast.LENGTH_LONG).show()
                    Log.e("LoginError", "Error: $error")
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@LoginScreem, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("LoginError", "Exception: ${e.message}", e)
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, EditProfile::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}