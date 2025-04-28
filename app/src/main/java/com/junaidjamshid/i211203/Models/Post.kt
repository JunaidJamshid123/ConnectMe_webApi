package com.junaidjamshid.i211203.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Post {
    @Json(name = "id")
    var postId: String = ""          // Unique ID for the post
    
    @Json(name = "userId")
    var userId: String = ""          // ID of the user who created the post
    
    @Json(name = "username")
    var username: String = ""        // Username of the creator
    
    @Json(name = "userProfileImage")
    var userProfileImage: String = ""// Profile image URL of the user
    
    @Json(name = "postImageUrl")
    var postImageUrl: String = ""    // URL of the uploaded image (if any)
    
    @Json(name = "caption")
    var caption: String = ""         // Post description or caption
    
    @Json(name = "timestamp")
    var timestamp: Long = 0          // Time when the post was created (Unix timestamp)
    
    @Json(name = "likes")
    var likes: MutableMap<String, Boolean> = mutableMapOf() // Map of userId to like status
    
    @Json(name = "comments")
    var comments: MutableList<Comment> = mutableListOf()    // List of comments

    constructor()

    constructor(
        postId: String,
        userId: String,
        username: String,
        userProfileImage: String,
        postImageUrl: String,
        caption: String,
        timestamp: Long,
        likes: MutableMap<String, Boolean>,
        comments: MutableList<Comment>
    ) {
        this.postId = postId
        this.userId = userId
        this.username = username
        this.userProfileImage = userProfileImage
        this.postImageUrl = postImageUrl
        this.caption = caption
        this.timestamp = timestamp
        this.likes = likes
        this.comments = comments
    }
}
