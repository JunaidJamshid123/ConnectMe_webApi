package com.junaidjamshid.i211203.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Story {
    @Json(name = "id")
    var storyId: String = ""          // Unique ID for the story
    
    @Json(name = "userId")
    var userId: String = ""          // ID of the user who created the story
    
    @Json(name = "username")
    var username: String = ""        // Username of the creator
    
    @Json(name = "userProfileImage")
    var userProfileImage: String = ""// Profile image URL of the user
    
    @Json(name = "storyImageUrl")
    var storyImageUrl: String = ""   // URL of the story image
    
    @Json(name = "caption")
    var caption: String = ""         // Story description or caption
    
    @Json(name = "timestamp")
    var timestamp: Long = 0          // Time when the story was created (Unix timestamp)
    
    @Json(name = "expiryTimestamp")
    var expiryTimestamp: Long = 0    // Time when the story will expire (Unix timestamp)

    constructor()

    constructor(
        storyId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        storyImageUrl: String,
        caption: String,
        timestamp: Long,
        expiryTimestamp: Long
    ) {
        this.storyId = storyId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.storyImageUrl = storyImageUrl
        this.caption = caption
        this.timestamp = timestamp
        this.expiryTimestamp = expiryTimestamp
    }
}
