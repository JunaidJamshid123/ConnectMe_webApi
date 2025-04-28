package com.junaidjamshid.i211203.HelperClasses

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.junaidjamshid.i211203.models.User
import com.junaidjamshid.i211203.models.Post
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "localAppDatabase.db"
        private const val DATABASE_VERSION = 1

        // Posts table
        private const val TABLE_POSTS = "posts"
        private const val COLUMN_POST_ID = "post_id"
        private const val COLUMN_POST_USER_ID = "user_id"
        private const val COLUMN_POST_USERNAME = "username"
        private const val COLUMN_POST_USER_PROFILE_IMAGE = "user_profile_image"
        private const val COLUMN_POST_IMAGE_URL = "post_image_url"
        private const val COLUMN_POST_CAPTION = "caption"
        private const val COLUMN_POST_TIMESTAMP = "timestamp"
        private const val COLUMN_POST_LIKES_COUNT = "likes_count"
        private const val COLUMN_POST_COMMENTS_COUNT = "comments_count"
        private const val COLUMN_POST_SYNC_STATUS = "sync_status" // 0=synced, 1=pending sync


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


        // Create posts table
        val createPostsTable = """
        CREATE TABLE $TABLE_POSTS (
            $COLUMN_POST_ID TEXT PRIMARY KEY,
            $COLUMN_POST_USER_ID TEXT NOT NULL,
            $COLUMN_POST_USERNAME TEXT NOT NULL,
            $COLUMN_POST_USER_PROFILE_IMAGE TEXT,
            $COLUMN_POST_IMAGE_URL TEXT,
            $COLUMN_POST_CAPTION TEXT,
            $COLUMN_POST_TIMESTAMP INTEGER,
            $COLUMN_POST_LIKES_COUNT INTEGER DEFAULT 0,
            $COLUMN_POST_COMMENTS_COUNT INTEGER DEFAULT 0,
            $COLUMN_POST_SYNC_STATUS INTEGER DEFAULT 0,
            FOREIGN KEY ($COLUMN_POST_USER_ID) REFERENCES $TABLE_USERS($COLUMN_USER_ID)
        )
    """.trimIndent()

        db.execSQL(createPostsTable)

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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSTS")
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
                this.email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)) // Fixed this line
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

    // Add post to SQLite database
    fun savePost(post: Post): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_POST_ID, post.postId)
            put(COLUMN_POST_USER_ID, post.userId)
            put(COLUMN_POST_USERNAME, post.username)
            put(COLUMN_POST_USER_PROFILE_IMAGE, post.userProfileImage)
            put(COLUMN_POST_IMAGE_URL, post.postImageUrl)
            put(COLUMN_POST_CAPTION, post.caption)
            put(COLUMN_POST_TIMESTAMP, post.timestamp)
            put(COLUMN_POST_LIKES_COUNT, post.likes.size)
            put(COLUMN_POST_COMMENTS_COUNT, post.comments.size)
            put(COLUMN_POST_SYNC_STATUS, 1) // Default to pending sync
        }

        return db.insertWithOnConflict(TABLE_POSTS, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Update the sync status of a post
    fun updatePostSyncStatus(postId: String, isSynced: Boolean) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_POST_SYNC_STATUS, if (isSynced) 0 else 1)
        }

        db.update(TABLE_POSTS, contentValues, "$COLUMN_POST_ID = ?", arrayOf(postId))
    }

    // Get posts that need to be synced with server
    fun getUnSyncedPosts(): List<Post> {
        val posts = mutableListOf<Post>()
        val db = this.readableDatabase

        val query = "SELECT * FROM $TABLE_POSTS WHERE $COLUMN_POST_SYNC_STATUS = 1"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val post = Post().apply {
                    postId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_ID))
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_ID))
                    username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USERNAME))
                    userProfileImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_PROFILE_IMAGE) ?: 0)
                    postImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_IMAGE_URL) ?: 0)
                    caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_CAPTION) ?: 0)
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_POST_TIMESTAMP))
                    // We don't load likes and comments as they're just counts in SQLite
                }
                posts.add(post)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return posts
    }

    // Get a post by ID
    fun getPostById(postId: String): Post? {
        val db = this.readableDatabase
        var post: Post? = null

        val query = "SELECT * FROM $TABLE_POSTS WHERE $COLUMN_POST_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(postId))

        if (cursor.moveToFirst()) {
            post = Post().apply {
                this.postId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_ID))
                userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_ID))
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USERNAME))
                userProfileImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_PROFILE_IMAGE) ?: 0)
                postImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_IMAGE_URL) ?: 0)
                caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_CAPTION) ?: 0)
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_POST_TIMESTAMP))
                // We don't load likes and comments as they're just counts in SQLite
            }
        }

        cursor.close()
        return post
    }

    // Get all posts with pagination
    fun getAllPosts(limit: Int, offset: Int): List<Post> {
        val posts = mutableListOf<Post>()
        val db = this.readableDatabase

        val query = "SELECT * FROM $TABLE_POSTS ORDER BY $COLUMN_POST_TIMESTAMP DESC LIMIT ? OFFSET ?"
        val cursor = db.rawQuery(query, arrayOf(limit.toString(), offset.toString()))

        if (cursor.moveToFirst()) {
            do {
                val post = Post().apply {
                    postId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_ID))
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_ID))
                    username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USERNAME))
                    userProfileImage = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_USER_PROFILE_IMAGE) ?: 0)
                    postImageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_IMAGE_URL) ?: 0)
                    caption = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_POST_CAPTION) ?: 0)
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_POST_TIMESTAMP))
                    // We don't load likes and comments as they're just counts in SQLite
                }
                posts.add(post)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return posts
    }



}