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

class LoginScreen : AppCompatActivity() {
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
                val result = apiService.loginUser(email, password)

                progressDialog.dismiss()

                if (result.isSuccess) {
                    val user = result.getOrNull()

                    if (user != null) {
                        // Check if this is the first login for the user
                        if (user.bio.isEmpty() && user.profilePicture == null) {
                            sessionManager.setFirstTimeUser(true)
                            navigateToProfileSetup()
                        } else {
                            sessionManager.setFirstTimeUser(false)
                            navigateToMainActivity()
                        }

                        Toast.makeText(this@LoginScreen, "Login successful!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Login failed"
                    Toast.makeText(this@LoginScreen, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@LoginScreen, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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