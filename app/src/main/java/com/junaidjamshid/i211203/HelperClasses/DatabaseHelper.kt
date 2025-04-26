package com.junaidjamshid.i211203.HelperClasses

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.junaidjamshid.i211203.models.User
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "localAppDatabase.db"
        private const val DATABASE_VERSION = 1

        // User table
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password" // Only storing hashed password locally
        private const val COLUMN_FULL_NAME = "fullName"
        private const val COLUMN_PHONE_NUMBER = "phoneNumber"
        private const val COLUMN_PROFILE_PICTURE = "profilePicture"
        private const val COLUMN_COVER_PHOTO = "coverPhoto"
        private const val COLUMN_BIO = "bio"
        private const val COLUMN_ONLINE_STATUS = "onlineStatus"
        private const val COLUMN_PUSH_TOKEN = "pushToken"
        private const val COLUMN_CREATED_AT = "createdAt"
        private const val COLUMN_LAST_SEEN = "lastSeen"
        private const val COLUMN_VANISH_MODE_ENABLED = "vanishModeEnabled"
        private const val COLUMN_STORY_EXPIRY_TIMESTAMP = "storyExpiryTimestamp"

        // Auth token table
        private const val TABLE_AUTH_TOKEN = "auth_tokens"
        private const val COLUMN_TOKEN = "token"
        private const val COLUMN_EXPIRES_AT = "expiresAt"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create user table
        val createUserTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_FULL_NAME TEXT NOT NULL,
                $COLUMN_PHONE_NUMBER TEXT,
                $COLUMN_PROFILE_PICTURE TEXT,
                $COLUMN_COVER_PHOTO TEXT,
                $COLUMN_BIO TEXT,
                $COLUMN_ONLINE_STATUS INTEGER DEFAULT 0,
                $COLUMN_PUSH_TOKEN TEXT,
                $COLUMN_CREATED_AT INTEGER,
                $COLUMN_LAST_SEEN INTEGER,
                $COLUMN_VANISH_MODE_ENABLED INTEGER DEFAULT 0,
                $COLUMN_STORY_EXPIRY_TIMESTAMP INTEGER
            )
        """.trimIndent()

        // Create auth token table
        val createAuthTokenTable = """
            CREATE TABLE $TABLE_AUTH_TOKEN (
                $COLUMN_USER_ID TEXT PRIMARY KEY,
                $COLUMN_TOKEN TEXT NOT NULL,
                $COLUMN_EXPIRES_AT INTEGER
            )
        """.trimIndent()

        db.execSQL(createUserTable)
        db.execSQL(createAuthTokenTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_AUTH_TOKEN")
        onCreate(db)
    }

    // User Operations
    fun saveUser(user: User): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USER_ID, user.userId)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_PHONE_NUMBER, user.phoneNumber)
            put(COLUMN_PROFILE_PICTURE, user.profilePicture)
            put(COLUMN_COVER_PHOTO, user.coverPhoto)
            put(COLUMN_BIO, user.bio)
            put(COLUMN_ONLINE_STATUS, if (user.onlineStatus) 1 else 0)
            put(COLUMN_PUSH_TOKEN, user.pushToken)
            put(COLUMN_CREATED_AT, user.createdAt)
            put(COLUMN_LAST_SEEN, user.lastSeen)
            put(COLUMN_VANISH_MODE_ENABLED, if (user.vanishModeEnabled) 1 else 0)
            user.storyExpiryTimestamp?.let { put(COLUMN_STORY_EXPIRY_TIMESTAMP, it) }
        }

        return db.insertWithOnConflict(TABLE_USERS, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveUserWithPassword(user: User, hashedPassword: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USER_ID, user.userId)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PASSWORD, hashedPassword)
            put(COLUMN_FULL_NAME, user.fullName)
            put(COLUMN_PHONE_NUMBER, user.phoneNumber)
            put(COLUMN_PROFILE_PICTURE, user.profilePicture)
            put(COLUMN_COVER_PHOTO, user.coverPhoto)
            put(COLUMN_BIO, user.bio)
            put(COLUMN_ONLINE_STATUS, if (user.onlineStatus) 1 else 0)
            put(COLUMN_PUSH_TOKEN, user.pushToken)
            put(COLUMN_CREATED_AT, user.createdAt)
            put(COLUMN_LAST_SEEN, user.lastSeen)
            put(COLUMN_VANISH_MODE_ENABLED, if (user.vanishModeEnabled) 1 else 0)
            user.storyExpiryTimestamp?.let { put(COLUMN_STORY_EXPIRY_TIMESTAMP, it) }
        }

        return db.insertWithOnConflict(TABLE_USERS, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserByEmail(email: String): User? {
        val db = this.readableDatabase
        var user: User? = null

        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))

        if (cursor.moveToFirst()) {
            user = User().apply {
                userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
               //email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME))
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER) ?: 0)
                profilePicture = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_PICTURE))
                coverPhoto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_PHOTO))
                bio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIO) ?: 0)
                onlineStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ONLINE_STATUS)) == 1
                pushToken = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUSH_TOKEN) ?: 0)
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                lastSeen = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_SEEN))
                vanishModeEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VANISH_MODE_ENABLED)) == 1
                val expiryIndex = cursor.getColumnIndex(COLUMN_STORY_EXPIRY_TIMESTAMP)
                if (expiryIndex != -1 && !cursor.isNull(expiryIndex)) {
                    storyExpiryTimestamp = cursor.getLong(expiryIndex)
                }
            }
        }

        cursor.close()
        return user
    }

    fun getUserById(userId: String): User? {
        val db = this.readableDatabase
        var user: User? = null

        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId))

        if (cursor.moveToFirst()) {
            user = User().apply {
                this.userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME))
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                fullName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULL_NAME))
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE_NUMBER) ?: 0)
                profilePicture = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_PICTURE))
                coverPhoto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_PHOTO))
                bio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BIO) ?: 0)
                onlineStatus = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ONLINE_STATUS)) == 1
                pushToken = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PUSH_TOKEN) ?: 0)
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))
                lastSeen = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_SEEN))
                vanishModeEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VANISH_MODE_ENABLED)) == 1
                val expiryIndex = cursor.getColumnIndex(COLUMN_STORY_EXPIRY_TIMESTAMP)
                if (expiryIndex != -1 && !cursor.isNull(expiryIndex)) {
                    storyExpiryTimestamp = cursor.getLong(expiryIndex)
                }
            }
        }

        cursor.close()
        return user
    }

    fun checkPassword(email: String, password: String): Boolean {
        val db = this.readableDatabase
        var hashedPassword: String? = null

        val query = "SELECT $COLUMN_PASSWORD FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))

        if (cursor.moveToFirst()) {
            hashedPassword = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
        }

        cursor.close()

        // In a real app, you would use a proper password checking method here
        // For demo purposes only
        return hashedPassword == password
    }

    // Auth Token Operations
    fun saveAuthToken(userId: String, token: String, expiresAt: Long) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_TOKEN, token)
            put(COLUMN_EXPIRES_AT, expiresAt)
        }

        db.insertWithOnConflict(TABLE_AUTH_TOKEN, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAuthToken(userId: String): Pair<String, Long>? {
        val db = this.readableDatabase
        var tokenPair: Pair<String, Long>? = null

        val query = "SELECT $COLUMN_TOKEN, $COLUMN_EXPIRES_AT FROM $TABLE_AUTH_TOKEN WHERE $COLUMN_USER_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(userId))

        if (cursor.moveToFirst()) {
            val token = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOKEN))
            val expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRES_AT))
            tokenPair = Pair(token, expiresAt)
        }

        cursor.close()
        return tokenPair
    }

    fun deleteAuthToken(userId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_AUTH_TOKEN, "$COLUMN_USER_ID = ?", arrayOf(userId))
    }

    fun clearAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_USERS, null, null)
        db.delete(TABLE_AUTH_TOKEN, null, null)
    }
}