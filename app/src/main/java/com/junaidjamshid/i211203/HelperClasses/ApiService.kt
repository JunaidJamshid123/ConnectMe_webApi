package com.junaidjamshid.i211203.HelperClasses


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.junaidjamshid.i211203.HelperClasses.DatabaseHelper
import com.junaidjamshid.i211203.models.User
import com.junaidjamshid.i211203.models.Post
import com.junaidjamshid.i211203.HelperClasses.SessionManager
import com.junaidjamshid.i211203.models.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

class ApiService(private val context: Context) {
     val dbHelper = DatabaseHelper(context)
    private val sessionManager = SessionManager(context)

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/api" // For local testing
        private const val POSTS_ENDPOINT = "/posts"
        private const val UPLOAD_IMAGE_ENDPOINT = "/uploads/images"

        // API Endpoints
        private const val REGISTER_ENDPOINT = "/auth/register"
        private const val LOGIN_ENDPOINT = "/auth/login"
        private const val USER_PROFILE_ENDPOINT = "/users"
        private const val SYNC_USER_ENDPOINT = "/users/sync"
    }

    // Check network connectivity
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    // Login User
    // Login User
    suspend fun loginUser(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            try {
                // Online login
                val url = URL("$BASE_URL$LOGIN_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonBody.toString())
                    it.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonResponse = JSONObject(response.toString())
                    val token = jsonResponse.getString("token")
                    val userJson = jsonResponse.getJSONObject("user")

                    val user = User().apply {
                        userId = userJson.getString("userId")
                        username = userJson.getString("username")
                        this.email = email // Set the email from the login parameters
                        fullName = userJson.getString("fullName")
                        phoneNumber = userJson.optString("phoneNumber", "")
                        profilePicture = userJson.optString("profilePicture", null)
                        coverPhoto = userJson.optString("coverPhoto", null)
                        bio = userJson.optString("bio", "")
                        createdAt = userJson.optLong("createdAt", System.currentTimeMillis())
                        lastSeen = userJson.optLong("lastSeen", System.currentTimeMillis())
                    }

                    // Save user WITH password to local storage
                    val dbHelper = DatabaseHelper(context)
                    dbHelper.saveUserWithPassword(user, password) // Use password parameter

                    // Store auth token
                    val expiryTimeMillis = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
                    sessionManager.createLoginSession(user.userId, token, expiryTimeMillis)
                    dbHelper.saveAuthToken(user.userId, token, expiryTimeMillis)

                    return@withContext Result.success(user)
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonError = JSONObject(errorResponse.toString())
                    val errorMessage = jsonError.optString("error", "Login failed")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        } else {
            // Offline login
            val dbHelper = DatabaseHelper(context)
            val user = dbHelper.getUserByEmail(email)
            if (user != null && dbHelper.checkPassword(email, password)) {
                // Check if we have a token stored for this user
                val tokenPair = dbHelper.getAuthToken(user.userId)
                if (tokenPair != null) {
                    val (token, expiryTimeMillis) = tokenPair
                    sessionManager.createLoginSession(user.userId, token, expiryTimeMillis)
                    return@withContext Result.success(user)
                } else {
                    return@withContext Result.failure(Exception("No authentication token found for offline login"))
                }
            } else {
                return@withContext Result.failure(Exception("Invalid credentials or user not found in offline storage"))
            }
        }
    }

    // Register User
    suspend fun registerUser(userId: String, username: String, email: String, password: String, fullName: String, phoneNumber: String): Result<User> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection available for registration"))
        }

        try {
            val url = URL("$BASE_URL$REGISTER_ENDPOINT")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("userId", userId)
                put("username", username)
                put("email", email)
                put("password", password)
                put("fullName", fullName)
                put("phoneNumber", phoneNumber)
            }

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonBody.toString())
                it.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonResponse = JSONObject(response.toString())
                val token = jsonResponse.getString("token")
                val userJson = jsonResponse.getJSONObject("user")

                val user = User().apply {
                    this.userId = userJson.getString("userId")
                    this.username = userJson.getString("username")
                    this.email = userJson.getString("email")
                    this.fullName = userJson.getString("fullName")
                    this.phoneNumber = userJson.optString("phoneNumber", "")
                    this.createdAt = userJson.optLong("createdAt", System.currentTimeMillis())
                    this.lastSeen = userJson.optLong("lastSeen", System.currentTimeMillis())
                }

                // Save user and token locally for offline access
                dbHelper.saveUserWithPassword(user, password) // In a real app, store the hashed password
                val expiryTimeMillis = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
                sessionManager.createLoginSession(user.userId, token, expiryTimeMillis)

                return@withContext Result.success(user)
            } else {
                val reader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonError = JSONObject(errorResponse.toString())
                val errorMessage = jsonError.optString("error", "Registration failed")

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Sync user data
    suspend fun syncUserData(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection available for syncing"))
        }

        try {
            val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

            val url = URL("$BASE_URL$SYNC_USER_ENDPOINT")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("userId", userId)
            }

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonBody.toString())
                it.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonResponse = JSONObject(response.toString())
                val userJson = jsonResponse.getJSONObject("user")

                val user = User().apply {
                    this.userId = userJson.getString("userId")
                    this.username = userJson.getString("username")
                    this.email = userJson.getString("email")
                    this.fullName = userJson.getString("fullName")
                    this.phoneNumber = userJson.optString("phoneNumber", "")
                    this.profilePicture = userJson.optString("profilePicture", null)
                    this.coverPhoto = userJson.optString("coverPhoto", null)
                    this.bio = userJson.optString("bio", "")
                    this.createdAt = userJson.optLong("createdAt", System.currentTimeMillis())
                    this.lastSeen = userJson.optLong("lastSeen", System.currentTimeMillis())
                    this.onlineStatus = userJson.optBoolean("onlineStatus", false)
                    this.vanishModeEnabled = userJson.optBoolean("vanishModeEnabled", false)
                    if (userJson.has("storyExpiryTimestamp") && !userJson.isNull("storyExpiryTimestamp")) {
                        this.storyExpiryTimestamp = userJson.getLong("storyExpiryTimestamp")
                    }
                }

                // Update local user data
                dbHelper.saveUser(user)

                return@withContext Result.success(true)
            } else {
                val reader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonError = JSONObject(errorResponse.toString())
                val errorMessage = jsonError.optString("error", "Sync failed")

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Get user profile (supports both online and offline)
    suspend fun getUserProfile(userId: String): Result<User> = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            try {
                val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

                val url = URL("$BASE_URL$USER_PROFILE_ENDPOINT/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonResponse = JSONObject(response.toString())
                    val userJson = jsonResponse.getJSONObject("user")

                    val user = User().apply {
                        this.userId = userJson.getString("userId")
                        this.username = userJson.getString("username")
                        this.email = userJson.getString("email")
                        this.fullName = userJson.getString("fullName")
                        this.phoneNumber = userJson.optString("phoneNumber", "")
                        this.profilePicture = userJson.optString("profilePicture", null)
                        this.coverPhoto = userJson.optString("coverPhoto", null)
                        this.bio = userJson.optString("bio", "")
                        this.createdAt = userJson.optLong("createdAt", System.currentTimeMillis())
                        this.lastSeen = userJson.optLong("lastSeen", System.currentTimeMillis())
                        this.onlineStatus = userJson.optBoolean("onlineStatus", false)
                        this.vanishModeEnabled = userJson.optBoolean("vanishModeEnabled", false)
                        if (userJson.has("storyExpiryTimestamp") && !userJson.isNull("storyExpiryTimestamp")) {
                            this.storyExpiryTimestamp = userJson.getLong("storyExpiryTimestamp")
                        }
                    }

                    // Update local user data
                    dbHelper.saveUser(user)

                    return@withContext Result.success(user)
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonError = JSONObject(errorResponse.toString())
                    val errorMessage = jsonError.optString("error", "Failed to fetch user profile")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // If network call fails, try to get user from local database
                val user = dbHelper.getUserById(userId)
                if (user != null) {
                    return@withContext Result.success(user)
                } else {
                    return@withContext Result.failure(e)
                }
            }
        } else {
            // Offline mode - get from local database
            val user = dbHelper.getUserById(userId)
            if (user != null) {
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("User not found in offline storage"))
            }
        }
    }

    // Logout
    fun logout() {
        sessionManager.logout()
    }

    // Add this method to upload a post image to the server
    suspend fun uploadPostImage(imageBase64: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection available for image upload"))
        }

        try {
            val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

            val url = URL("$BASE_URL$UPLOAD_IMAGE_ENDPOINT")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("image", imageBase64)
            }

            OutputStreamWriter(connection.outputStream).use {
                it.write(jsonBody.toString())
                it.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonResponse = JSONObject(response.toString())
                val imageUrl = jsonResponse.getString("imageUrl")

                return@withContext Result.success(imageUrl)
            } else {
                val reader = BufferedReader(InputStreamReader(connection.errorStream))
                val errorResponse = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    errorResponse.append(line)
                }

                reader.close()
                connection.disconnect()

                val jsonError = JSONObject(errorResponse.toString())
                val errorMessage = jsonError.optString("error", "Image upload failed")

                return@withContext Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Add this method to create a new post
    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            try {
                val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

                val url = URL("$BASE_URL$POSTS_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("postId", post.postId)
                    put("userId", post.userId)
                    put("username", post.username)
                    put("userProfileImage", post.userProfileImage)
                    put("postImageUrl", post.postImageUrl)
                    put("caption", post.caption)
                    // Timestamp will be set on the server, so no need to send it
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonBody.toString())
                    it.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonResponse = JSONObject(response.toString())
                    val postJson = jsonResponse.getJSONObject("post")

                    val createdPost = Post().apply {
                        postId = postJson.getString("postId")
                        userId = postJson.getString("userId")
                        username = postJson.getString("username")
                        userProfileImage = postJson.optString("userProfileImage", "")
                        postImageUrl = postJson.getString("postImageUrl")
                        caption = postJson.optString("caption", "")
                        timestamp = postJson.getLong("timestamp")
                        // Server returns empty likes and comments initially
                    }

                    // Mark post as synced in local database
                    dbHelper.updatePostSyncStatus(createdPost.postId, true)

                    return@withContext Result.success(createdPost)
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonError = JSONObject(errorResponse.toString())
                    val errorMessage = jsonError.optString("error", "Failed to create post")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        } else {
            // Store post locally with sync status = unsynced
            post.timestamp = System.currentTimeMillis()
            val result = dbHelper.savePost(post)
            return@withContext if (result != -1L) {
                Result.success(post)
            } else {
                Result.failure(Exception("Failed to save post locally"))
            }
        }
    }

    // Add method to sync pending posts
    suspend fun syncPendingPosts(): Result<Int> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(Exception("No internet connection available for syncing"))
        }

        try {
            val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

            // Get all unsynced posts
            val unsyncedPosts = dbHelper.getUnSyncedPosts()
            var syncedCount = 0

            for (post in unsyncedPosts) {
                // If post contains a Base64 image, upload it first
                if (post.postImageUrl.startsWith("data:image") || post.postImageUrl.length > 200) {
                    val imageUploadResult = uploadPostImage(post.postImageUrl)
                    if (imageUploadResult.isSuccess) {
                        post.postImageUrl = imageUploadResult.getOrNull()!!
                    } else {
                        continue // Skip this post if image upload fails
                    }
                }

                // Now create the post on the server
                val url = URL("$BASE_URL$POSTS_ENDPOINT")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("postId", post.postId)
                    put("userId", post.userId)
                    put("username", post.username)
                    put("userProfileImage", post.userProfileImage)
                    put("postImageUrl", post.postImageUrl)
                    put("caption", post.caption)
                    put("timestamp", post.timestamp)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(jsonBody.toString())
                    it.flush()
                }

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    // Mark post as synced
                    dbHelper.updatePostSyncStatus(post.postId, true)
                    syncedCount++
                }

                connection.disconnect()
            }

            return@withContext Result.success(syncedCount)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Add method to get all posts
    suspend fun getPosts(page: Int, pageSize: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        if (isNetworkAvailable()) {
            try {
                val token = sessionManager.getAuthToken() ?: return@withContext Result.failure(Exception("Authentication token not found"))

                val url = URL("$BASE_URL$POSTS_ENDPOINT?page=$page&pageSize=$pageSize")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonResponse = JSONObject(response.toString())
                    val postsArray = jsonResponse.getJSONArray("posts")

                    val posts = mutableListOf<Post>()

                    for (i in 0 until postsArray.length()) {
                        val postJson = postsArray.getJSONObject(i)

                        val post = Post().apply {
                            postId = postJson.getString("postId")
                            userId = postJson.getString("userId")
                            username = postJson.getString("username")
                            userProfileImage = postJson.optString("userProfileImage", "")
                            postImageUrl = postJson.getString("postImageUrl")
                            caption = postJson.optString("caption", "")
                            timestamp = postJson.getLong("timestamp")

                            // Parse likes
                            val likesJson = postJson.optJSONObject("likes")
                            if (likesJson != null) {
                                val likesIterator = likesJson.keys()
                                while (likesIterator.hasNext()) {
                                    val key = likesIterator.next()
                                    likes[key] = likesJson.getBoolean(key)
                                }
                            }

                            // Parse comments
                            /*
                            val commentsArray = postJson.optJSONArray("comments")
                            if (commentsArray != null) {
                                for (j in 0 until commentsArray.length()) {
                                    val commentJson = commentsArray.getJSONObject(j)
                                    val comment = Comment(
                                        commentId = commentJson.getString("commentId"),
                                        userId = commentJson.getString("userId"),
                                        username = commentJson.getString("username"),
                                        userProfileImage = commentJson.optString("userProfileImage", ""),
                                        text = commentJson.getString("text"),
                                        timestamp = commentJson.getLong("timestamp")
                                    )
                                    comments.add(comment)
                                }
                            }
                            */

                        }

                        posts.add(post)

                        // Save post to local database for offline viewing
                        dbHelper.savePost(post)
                        dbHelper.updatePostSyncStatus(post.postId, true)
                    }

                    return@withContext Result.success(posts)
                } else {
                    val reader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }

                    reader.close()
                    connection.disconnect()

                    val jsonError = JSONObject(errorResponse.toString())
                    val errorMessage = jsonError.optString("error", "Failed to fetch posts")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                // If network call fails, try to get posts from local database
                val posts = dbHelper.getAllPosts(pageSize, (page - 1) * pageSize)
                if (posts.isNotEmpty()) {
                    return@withContext Result.success(posts)
                } else {
                    return@withContext Result.failure(e)
                }
            }
        } else {
            // Offline mode - get from local database
            val posts = dbHelper.getAllPosts(pageSize, (page - 1) * pageSize)
            if (posts.isNotEmpty()) {
                return@withContext Result.success(posts)
            } else {
                return@withContext Result.failure(Exception("No posts found in offline storage"))
            }
        }
    }
}