package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.junaidjamshid.i211203.HelperClasses.ApiService
import com.junaidjamshid.i211203.HelperClasses.NetworkUtils
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import com.junaidjamshid.i211203.models.User
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfile : AppCompatActivity() {

    private lateinit var profileImageView: CircleImageView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var doneButton: TextView
    private lateinit var progressDialog: ProgressDialog

    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: ApiService

    private var profileImageByteArray: ByteArray? = null
    private var currentUser: User? = null

    // Image selection launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                try {
                    // Load and set the selected image
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    profileImageView.setImageBitmap(bitmap)

                    // Convert bitmap to ByteArray
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    profileImageByteArray = stream.toByteArray()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageBitmap = data?.extras?.get("data") as? Bitmap

            imageBitmap?.let { bitmap ->
                // Set the captured image
                profileImageView.setImageBitmap(bitmap)

                // Convert bitmap to ByteArray
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                profileImageByteArray = stream.toByteArray()
            }
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide Action Bar
        setContentView(R.layout.activity_edit_profile)

        // Initialize services
        sessionManager = SessionManager(this)
        apiService = ApiService(this)

        // Initialize views
        profileImageView = findViewById(R.id.profile_image)
        nameEditText = findViewById(R.id.et_name)
        usernameEditText = findViewById(R.id.et_username)
        phoneEditText = findViewById(R.id.et_phone)
        bioEditText = findViewById(R.id.et_bio)
        doneButton = findViewById(R.id.btn_done)

        // Initialize progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating profile...")
        progressDialog.setCancelable(false)

        // Set up click listener for profile image
        val imageContainer = findViewById<FrameLayout>(R.id.profile_image_container)
        imageContainer.setOnClickListener {
            showImagePickerDialog()
        }

        // Set up click listener for done button
        doneButton.setOnClickListener {
            saveUserProfile()
        }

        // Load current user data
        loadUserData()
    }

    private fun loadUserData() {
        progressDialog.setMessage("Loading profile...")
        progressDialog.show()

        val userId = sessionManager.getUserId()

        if (userId?.isNotEmpty() ?: false ) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = apiService.getUserProfile(userId.toString())

                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        currentUser = user

                        user?.let {
                            // Set text fields
                            nameEditText.setText(it.fullName)
                            usernameEditText.setText(it.username)
                            phoneEditText.setText(it.phoneNumber)
                            bioEditText.setText(it.bio)

                            // Load profile image if exists
                            val profilePictureBytes = it.getProfilePictureBytes()
                            profilePictureBytes?.let { bytes ->
                                profileImageByteArray = bytes
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                profileImageView.setImageBitmap(bitmap)
                            }
                        }
                        progressDialog.dismiss()
                    } else {
                        progressDialog.dismiss()
                        val error = result.exceptionOrNull()?.message ?: "Failed to load user data"
                        Toast.makeText(this@EditProfile, error, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(this@EditProfile, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            progressDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun saveUserProfile() {
        val name = nameEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val bio = bioEditText.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Name, username and phone are required", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.show()

        val userId = sessionManager.getUserId()

        if (userId?.isNotEmpty() ?: false) {
            // Create updates map
            val updates = JSONObject().apply {
                put("fullName", name)
                put("username", username)
                put("phoneNumber", phone)
                put("bio", bio)

                // Add profile picture if changed
                if (profileImageByteArray != null) {
                    val base64Image = Base64.encodeToString(profileImageByteArray, Base64.DEFAULT)
                    put("profilePicture", base64Image)
                }
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Check network connectivity first
                    if (NetworkUtils.isNetworkAvailable(this@EditProfile)) {
                        // Online update
                        val result = updateUserProfileOnline(userId, updates)
                        handleUpdateResult(result)
                    } else {
                        // Update locally and queue for sync later
                        val result = updateUserProfileOffline(userId, name, username, phone, bio, profileImageByteArray)
                        handleUpdateResult(result)
                    }
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(this@EditProfile, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            progressDialog.dismiss()
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private suspend fun updateUserProfileOnline(userId: String, updates: JSONObject): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = "http://10.0.2.2:3000/api"
                val updateEndpoint = "$baseUrl/users/$userId"
                val token = sessionManager.getAuthToken()

                if (token == null) {
                    return@withContext Result.failure(Exception("Authentication token not found"))
                }

                val url = java.net.URL(updateEndpoint)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "PUT"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true

                // Write request body
                val outputStream = connection.outputStream
                val writer = java.io.OutputStreamWriter(outputStream)
                writer.write(updates.toString())
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode

                if (responseCode == 200) {
                    // Update successful, now update local storage with the new data
                    val user = currentUser?.apply {
                        fullName = updates.getString("fullName")
                        username = updates.getString("username")
                        phoneNumber = updates.getString("phoneNumber")
                        bio = updates.getString("bio")

                        if (updates.has("profilePicture")) {
                            profilePicture = updates.getString("profilePicture")
                        }
                    }

                    if (user != null) {
                        apiService.dbHelper.saveUser(user)
                    }

                    return@withContext Result.success(true)
                } else {
                    // Handle error
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.errorStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()

                    val errorJson = JSONObject(response.toString())
                    val errorMessage = errorJson.optString("error", "Failed to update profile")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private suspend fun updateUserProfileOffline(
        userId: String,
        name: String,
        username: String,
        phone: String,
        bio: String,
        profileImageBytes: ByteArray?
    ): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Get existing user data
                val dbHelper = apiService.dbHelper
                val user = dbHelper.getUserById(userId) ?: return@withContext Result.failure(Exception("User not found"))

                // Update the user object
                user.fullName = name
                user.username = username
                user.phoneNumber = phone
                user.bio = bio

                if (profileImageBytes != null) {
                    user.setProfilePictureFromBytes(profileImageBytes)
                }

                // Save to local database
                dbHelper.saveUser(user)

                // Mark for future syncing when online
                // You would implement a sync queue mechanism here

                return@withContext Result.success(true)
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
    }

    private fun handleUpdateResult(result: Result<Boolean>) {
        progressDialog.dismiss()

        if (result.isSuccess) {
            Toast.makeText(this@EditProfile, "Profile updated successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to main activity
            val intent = Intent(this@EditProfile, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            val error = result.exceptionOrNull()?.message ?: "Failed to update profile"
            Toast.makeText(this@EditProfile, error, Toast.LENGTH_LONG).show()
        }
    }
}