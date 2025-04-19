package com.junaidjamshid.i211203

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.junaidjamshid.i211203.models.User

class SignUpScreen : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_sign_up_screen)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Initialize Progress Dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating account...")
        progressDialog.setCancelable(false)

        val loginLink = findViewById<TextView>(R.id.LoginLink)
        val fullName = findViewById<EditText>(R.id.FullName)
        val username = findViewById<EditText>(R.id.username)
        val phone = findViewById<EditText>(R.id.Phone)
        val email = findViewById<EditText>(R.id.Email)
        val password = findViewById<EditText>(R.id.Password)
        val registerBtn = findViewById<Button>(R.id.registerBtn)

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginScreem::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        registerBtn.setOnClickListener {
            val fullNameInput = fullName.text.toString().trim()
            val usernameInput = username.text.toString().trim()
            val phoneInput = phone.text.toString().trim()
            val emailInput = email.text.toString().trim()
            val passwordInput = password.text.toString().trim()

            if (fullNameInput.isEmpty() || usernameInput.isEmpty() || phoneInput.isEmpty() ||
                emailInput.isEmpty() || passwordInput.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            } else {
                signUpUser(fullNameInput, usernameInput, phoneInput, emailInput, passwordInput)
            }
        }
    }

    private fun signUpUser(fullName: String, username: String, phone: String, email: String, password: String) {
        progressDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val user = User(
                            userId = userId,
                            username = username,
                            email = email,
                            fullName = fullName,
                            phoneNumber = phone,
                            profilePicture = null,
                            coverPhoto = null,
                            bio = "",
                            followers = hashMapOf(),
                            following = hashMapOf(),
                            blockedUsers = hashMapOf(),
                            onlineStatus = false,
                            pushToken = "",
                            createdAt = System.currentTimeMillis(),
                            lastSeen = System.currentTimeMillis(),
                            vanishModeEnabled = false,
                            storyExpiryTimestamp = null
                        )

                        database.child("Users").child(userId).setValue(user)
                            .addOnCompleteListener { dbTask ->
                                progressDialog.dismiss()

                                if (dbTask.isSuccessful) {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, EditProfile::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Failed to store user data: ${dbTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}