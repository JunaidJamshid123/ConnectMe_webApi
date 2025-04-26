package com.junaidjamshid.i211203.HelperClasses


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.junaidjamshid.i211203.HelperClasses.DatabaseHelper
import com.junaidjamshid.i211203.models.User
import com.junaidjamshid.i211203.HelperClasses.SessionManager
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
    private val dbHelper = DatabaseHelper(context)
    private val sessionManager = SessionManager(context)

    companion object {
        private const val BASE_URL = "http://10.0.2.2:3000/api" // For local testing
        // private const val BASE_URL = "https://your-production-api.com/api" // For production

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
                        //email = userJson.getString("email")
                        fullName = userJson.getString("fullName")
                        phoneNumber = userJson.optString("phoneNumber", "")
                        profilePicture = userJson.optString("profilePicture", null)
                        coverPhoto = userJson.optString("coverPhoto", null)
                        bio = userJson.optString("bio", "")
                        createdAt = userJson.optLong("createdAt", System.currentTimeMillis())
                        lastSeen = userJson.optLong("lastSeen", System.currentTimeMillis())
                    }

                    // Save user and token to local storage
                    dbHelper.saveUser(user)
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
                    val errorMessage = jsonError.optString("error", "Login failed")

                    return@withContext Result.failure(Exception(errorMessage))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        } else {
            // Offline login
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
}