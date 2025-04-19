package com.junaidjamshid.i211203.HelperClasses

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.chats

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Chat Notifications"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "New Message"
            val messageBody = notification.body ?: "You have a new message"

            // Get data payload
            val senderId = remoteMessage.data["senderId"] ?: ""
            val messageId = remoteMessage.data["messageId"] ?: ""
            val receiverId = remoteMessage.data["receiverId"] ?: ""

            // Only show notification if it's for the current user
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && receiverId == currentUser.uid) {
                // Use the NotificationHelper to check if we're in a chat with this sender
                /*
                if (NotificationHelper.isAppInForeground(this)) {
                    val currentActivity = (applicationContext as? YourApplication)?.currentActivity
                    if (currentActivity is chats) {
                        val currentChatUserId = currentActivity.intent.getStringExtra("USER_ID")
                        if (currentChatUserId == senderId) {
                            // Already chatting with sender, no need for notification
                            return
                        }

                 }
                }

                 */

                sendNotification(title, messageBody, senderId, messageId)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If the app gets a new FCM token, save it to the user's database entry
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            saveTokenToDatabase(token)
        }
    }

    private fun saveTokenToDatabase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val userRef = database.reference.child("Users").child(userId)
        userRef.child("fcmToken").setValue(token)
    }

    private fun sendNotification(title: String, messageBody: String, senderId: String, messageId: String) {
        // Create intent to open the chat activity with the sender when notification is clicked
        val intent = Intent(this, chats::class.java).apply {
            putExtra("USER_ID", senderId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.bell)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for chat notifications"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Show the notification (use messageId as notification ID for uniqueness)
        val notificationId = messageId.hashCode()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}