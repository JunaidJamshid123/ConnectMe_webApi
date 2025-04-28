package com.junaidjamshid.i211203.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Comment {
    @Json(name = "id")
    var commentId: String = ""      // Unique ID for the comment
    
    @Json(name = "userId")
    var userId: String = ""         // ID of the user who made the comment
    
    @Json(name = "username")
    var username: String = ""       // Username of the commenter
    
    @Json(name = "userProfileImage")
    var userProfileImage: String = "" // Profile image URL of the commenter
    
    @Json(name = "text")
    var text: String = ""           // Comment text
    
    @Json(name = "timestamp")
    var timestamp: Long = 0         // Time when the comment was created (Unix timestamp)

    constructor()

    constructor(
        commentId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        text: String,
        timestamp: Long
    ) {
        this.commentId = commentId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.text = text
        this.timestamp = timestamp
    }
}
