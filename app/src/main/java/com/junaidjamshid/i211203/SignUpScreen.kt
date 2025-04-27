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
import java.util.UUID

class SignUpScreen : AppCompatActivity() {
    private lateinit var progressDialog: ProgressDialog
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_sign_up_screen)

        sessionManager = SessionManager(this)
        apiService = ApiService(this)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating account...")
        progressDialog.setCancelable(false)

        val loginLink = findViewById<TextView>(R.id.LoginLink)
        val fullName = findViewById<EditText>(R.id.FullName)
        val username = findViewById<EditText>(R.id.username)
        val phoneNumber = findViewById<EditText>(R.id.Phone)
        val email = findViewById<EditText>(R.id.Email)
        val password = findViewById<EditText>(R.id.Password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)


        loginLink.setOnClickListener {
            finish()
        }

        registerBtn.setOnClickListener {
            val fullNameInput = fullName.text.toString().trim()
            val usernameInput = username.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString().trim()
            val phoneNumberInput = phoneNumber.text.toString().trim()

            if (fullNameInput.isEmpty() || usernameInput.isEmpty() || emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "All fields are required except phone number", Toast.LENGTH_SHORT).show()
            } else if (passwordInput.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            } else {
                // Check if network is available
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Toast.makeText(
                        this,
                        "Internet connection required for registration",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                registerUser(
                    UUID.randomUUID().toString(),
                    usernameInput,
                    emailInput,
                    passwordInput,
                    fullNameInput,
                    phoneNumberInput
                )
            }
        }
    }

    private fun registerUser(
        userId: String,
        username: String,
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String
    ) {
        progressDialog.show()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = apiService.registerUser(
                    userId, username, email, password, fullName, phoneNumber
                )

                progressDialog.dismiss()

                if (result.isSuccess) {
                    val user = result.getOrNull()

                    if (user != null) {
                        sessionManager.setFirstTimeUser(true)
                        Toast.makeText(
                            this@SignUpScreen,
                            "Registration successful!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to profile setup
                        val intent = Intent(this@SignUpScreen, EditProfile::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Registration failed"
                    Toast.makeText(this@SignUpScreen, error, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(
                    this@SignUpScreen,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}