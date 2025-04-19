package com.junaidjamshid.i211203.HelperClasses

import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Helper class to send FCM notifications using HTTP v1 API
 */
class FirebaseMessagingHelper(private val serviceAccountStream: InputStream) {

    companion object {
        private const val TAG = "FirebaseMessagingHelper"
        private const val FCM_API_URL = "https://fcm.googleapis.com/v1/projects/i211203/messages:send"
        private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"

        // Singleton instance
        @Volatile
        private var INSTANCE: FirebaseMessagingHelper? = null

        fun getInstance(serviceAccountStream: InputStream): FirebaseMessagingHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseMessagingHelper(serviceAccountStream).also { INSTANCE = it }
            }
        }
    }

    private var cachedToken: String? = null
    private var tokenExpiration: Long = 0

    /**
     * Get OAuth2 access token for the FCM API
     */
    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        // Check if we have a valid cached token
        val currentTime = System.currentTimeMillis()
        if (cachedToken != null && currentTime < tokenExpiration) {
            return@withContext cachedToken!!
        }

        try {
            // Create credentials from service account
            val credentials = GoogleCredentials
                .fromStream(serviceAccountStream)
                .createScoped(listOf(SCOPE))

            val accessToken = credentials.refreshAccessToken()
            cachedToken = accessToken.tokenValue

            // Set expiration to 1 hour (minus 5 minutes for safety)
            tokenExpiration = currentTime + TimeUnit.HOURS.toMillis(1) - TimeUnit.MINUTES.toMillis(5)

            return@withContext cachedToken!!
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token", e)
            throw e
        }
    }

    /**
     * Send a notification to a specific FCM token using the HTTP v1 API
     */
    suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()

            // Create FCM message payload according to HTTP v1 API format
            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    put("token", token)

                    // Notification payload
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })

                    // Data payload
                    if (data.isNotEmpty()) {
                        put("data", JSONObject().apply {
                            for ((key, value) in data) {
                                put(key, value)
                            }
                        })
                    }

                    // Android specific configuration
                    put("android", JSONObject().apply {
                        put("priority", "high")
                        put("notification", JSONObject().apply {
                            put("channel_id", "chat_notifications")
                            put("click_action", "OPEN_CHAT_ACTIVITY")
                        })
                    })
                })
            }

            // Create connection to FCM API
            val url = URL(FCM_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Write JSON payload
            val os = conn.outputStream
            val writer = OutputStreamWriter(os, "UTF-8")
            writer.write(message.toString())
            writer.flush()
            writer.close()
            os.close()

            // Check response
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "FCM notification sent successfully")
                return@withContext true
            } else {
                val errorStream = conn.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "FCM error: $responseCode, $errorResponse")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending FCM notification", e)
            return@withContext false
        }
    }

    /**
     * Send notification to a topic using the HTTP v1 API
     */
    suspend fun sendTopicNotification(
        topic: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()

            // Create FCM message payload for topic
            val message = JSONObject().apply {
                put("message", JSONObject().apply {
                    // Topic must be prefixed with "/topics/"
                    put("topic", topic)

                    // Notification payload
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                    })

                    // Data payload
                    if (data.isNotEmpty()) {
                        put("data", JSONObject().apply {
                            for ((key, value) in data) {
                                put(key, value)
                            }
                        })
                    }

                    // Android specific configuration
                    put("android", JSONObject().apply {
                        put("priority", "high")
                        put("notification", JSONObject().apply {
                            put("channel_id", "chat_notifications")
                        })
                    })
                })
            }

            // Create connection to FCM API
            val url = URL(FCM_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // Write JSON payload
            val os = conn.outputStream
            val writer = OutputStreamWriter(os, "UTF-8")
            writer.write(message.toString())
            writer.flush()
            writer.close()
            os.close()

            // Check response
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                Log.d(TAG, "FCM topic notification sent successfully")
                return@withContext true
            } else {
                val errorStream = conn.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "FCM error: $responseCode, $errorResponse")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending topic notification", e)
            return@withContext false
        }
    }
}