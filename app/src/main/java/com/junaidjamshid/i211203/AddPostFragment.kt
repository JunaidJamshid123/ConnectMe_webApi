package com.junaidjamshid.i211203

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.junaidjamshid.i211203.HelperClasses.ApiService
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import com.junaidjamshid.i211203.models.Comment
import com.junaidjamshid.i211203.models.Post
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

class AddPostFragment : Fragment() {

    // UI components
    private lateinit var backButton: ImageView
    private lateinit var imagePlaceholder: FrameLayout
    private lateinit var postImage: ImageView
    private lateinit var selectImageButton: ImageButton
    private lateinit var captionInput: EditText
    private lateinit var shareButton: Button

    // API Service and Session Manager
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: SessionManager

    // Image URI
    private var selectedImageUri: Uri? = null

    // Request code for image selection
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_post, container, false)

        // Initialize UI components
        backButton = view.findViewById(R.id.back_button)
        imagePlaceholder = view.findViewById(R.id.image_placeholder)
        postImage = view.findViewById(R.id.post_image)
        selectImageButton = view.findViewById(R.id.select_image_button)
        captionInput = view.findViewById(R.id.caption_input)
        shareButton = view.findViewById(R.id.share)

        // Initialize API service and Session Manager
        apiService = ApiService(requireContext())
        sessionManager = SessionManager(requireContext())

        // Set click listeners
        setupClickListeners()

        return view
    }

    private fun setupClickListeners() {
        // Back button click listener
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        // Select image button click listener
        selectImageButton.setOnClickListener {
            openGallery()
        }

        // Also make the placeholder clickable to select image
        imagePlaceholder.setOnClickListener {
            openGallery()
        }

        // Share button click listener
        shareButton.setOnClickListener {
            if (validatePost()) {
                uploadPost()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data

            // Display the selected image
            postImage.visibility = View.VISIBLE
            postImage.setImageURI(selectedImageUri)

            // Hide the select image button
            selectImageButton.visibility = View.GONE
        }
    }

    private fun validatePost(): Boolean {
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return false
        }

        // Caption is optional, so we don't check for it
        return true
    }

    private fun uploadPost() {
        // Show loading
        showLoading(true)

        // Get the user ID from session
        val userId = sessionManager.getUserId()
        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        // Get the caption text
        val caption = captionInput.text.toString().trim()

        // Convert image to Base64 string
        val imageBase64 = convertImageToBase64()
        if (imageBase64 == null) {
            Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
            showLoading(false)
            return
        }

        // Use coroutine to perform network operations
        lifecycleScope.launch {
            try {
                // First get the user profile to include username and profile image
                val userProfileResult = apiService.getUserProfile(userId)

                if (userProfileResult.isSuccess) {
                    val user = userProfileResult.getOrNull()!!

                    // Create a unique post ID
                    val postId = UUID.randomUUID().toString()

                    // Create post object
                    val post = Post(
                        postId = postId,
                        userId = userId,
                        username = user.username,
                        userProfileImage = user.profilePicture ?: "",
                        postImageUrl = imageBase64, // We'll store base64 for offline posts
                        caption = caption,
                        timestamp = System.currentTimeMillis(),
                        likes = mutableMapOf(),
                        comments = mutableListOf()
                    )

                    // Try to create the post
                    val createPostResult = apiService.createPost(post)

                    if (createPostResult.isSuccess) {
                        Toast.makeText(context, "Post uploaded successfully", Toast.LENGTH_SHORT).show()

                        // If we're online and just created the post, try to sync any pending posts
                        apiService.syncPendingPosts()

                        // Clear inputs and go back
                        clearInputs()
                        showLoading(false)
                        requireActivity().onBackPressed()
                    } else {
                        val error = createPostResult.exceptionOrNull()?.message ?: "Unknown error"

                        if (error.contains("internet") || error.contains("network")) {
                            // If it's a network error but we saved locally
                            Toast.makeText(context, "Post saved offline and will sync when online", Toast.LENGTH_LONG).show()
                            clearInputs()
                            showLoading(false)
                            requireActivity().onBackPressed()
                        } else {
                            Toast.makeText(context, "Failed to upload post: $error", Toast.LENGTH_SHORT).show()
                            showLoading(false)
                        }
                    }
                } else {
                    val error = userProfileResult.exceptionOrNull()?.message ?: "Unknown error"
                    Toast.makeText(context, "Failed to get user profile: $error", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun convertImageToBase64(): String? {
        try {
            val inputStream = context?.contentResolver?.openInputStream(selectedImageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Resize bitmap to reduce file size
            val resizedBitmap = resizeBitmap(bitmap, 800)

            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        val ratio = width.toFloat() / height.toFloat()

        if (ratio > 1) {
            // Landscape image
            width = maxSize
            height = (width / ratio).toInt()
        } else {
            // Portrait image
            height = maxSize
            width = (height * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun clearInputs() {
        selectedImageUri = null
        postImage.visibility = View.GONE
        selectImageButton.visibility = View.VISIBLE
        captionInput.text.clear()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            shareButton.isEnabled = false
            shareButton.text = "Uploading..."
        } else {
            shareButton.isEnabled = true
            shareButton.text = "Share"
        }
    }
}