package com.junaidjamshid.i211203

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.junaidjamshid.i211203.Adapters.PostAdapter
import com.junaidjamshid.i211203.Adapters.StoryAdapter
import com.junaidjamshid.i211203.models.Comment
import com.junaidjamshid.i211203.models.Post
import com.junaidjamshid.i211203.models.Story
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*

class HomeFragment : Fragment(), PostAdapter.OnPostInteractionListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var auth: FirebaseAuth
    private val TAG = "HomeFragment"
    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var storyAdapter: StoryAdapter
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val postJsonAdapter = moshi.adapter(Post::class.java).lenient()
    private val storyJsonAdapter = moshi.adapter(Story::class.java).lenient()
    private val commentJsonAdapter = moshi.adapter(Comment::class.java).lenient()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val addStroy = view.findViewById<FrameLayout>(R.id.addStroy)
        val dms = view.findViewById<ImageView>(R.id.DMs)
        val currentUserImage = view.findViewById<ImageView>(R.id.current_user_image)

        dms.setOnClickListener{
            val intent = Intent(context, DMs::class.java)
            startActivity(intent)
        }

        addStroy.setOnClickListener{
            val intent = Intent(context, newPostNext::class.java)
            startActivity(intent)
        }

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()

        // Load current user's profile image
        loadCurrentUserProfileImage(currentUserImage)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_posts)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize adapter
        postAdapter = PostAdapter(requireContext())
        postAdapter.setOnPostInteractionListener(this)
        recyclerView.adapter = postAdapter

        storiesRecyclerView = view.findViewById(R.id.recycler_view_stories)
        storiesRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        storyAdapter = StoryAdapter(requireContext())

        storyAdapter.setOnStoryClickListener(object : StoryAdapter.OnStoryClickListener {
            override fun onStoryClick(story: Story, position: Int) {
                // This is optional since we're already handling the click in the adapter
                // But you could do additional processing here if needed
            }
        })
        storiesRecyclerView.adapter = storyAdapter

        // Load posts from API
        loadPostsFromApi()
        loadStories()
        return view
    }

    private fun loadPostsFromApi() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("http://localhost:3000/api/posts/")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val posts = responseBody?.let { parsePosts(it) }
                    
                    withContext(Dispatchers.Main) {
                        posts?.let {
                            postAdapter.setPosts(it)
                            if (it.isEmpty()) {
                                Toast.makeText(context, "No posts found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading posts: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading posts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parsePosts(jsonString: String): List<Post> {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            val posts = mutableListOf<Post>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val post = postJsonAdapter.fromJson(jsonObject.toString())
                post?.let { posts.add(it) }
            }
            posts
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing posts: ${e.message}")
            emptyList()
        }
    }

    private fun loadStories() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("http://localhost:3000/api/stories/")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val stories = responseBody?.let { parseStories(it) }
                    
                    withContext(Dispatchers.Main) {
                        stories?.let {
                            // Filter out expired stories and sort by timestamp
                            val currentTime = System.currentTimeMillis()
                            val validStories = it.filter { story -> story.expiryTimestamp > currentTime }
                                .sortedByDescending { story -> story.timestamp }
                            
                            storyAdapter.setStories(validStories)
                            
                            if (validStories.isEmpty()) {
                                Log.d(TAG, "No active stories found")
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Failed to load stories")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stories: ${e.message}")
            }
        }
    }

    private fun parseStories(jsonString: String): List<Story> {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            val stories = mutableListOf<Story>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val story = storyJsonAdapter.fromJson(jsonObject.toString())
                story?.let { stories.add(it) }
            }
            stories
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stories: ${e.message}")
            emptyList()
        }
    }

    private fun loadCurrentUserProfileImage(imageView: ImageView) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Reference to user data in the database
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get profile picture as Base64 string
                    val profileImageBase64 = snapshot.child("profilePicture").getValue(String::class.java)

                    if (!profileImageBase64.isNullOrEmpty()) {
                        // Decode Base64 to bitmap and set to ImageView
                        val bitmap = decodeBase64Image(profileImageBase64)
                        bitmap?.let {
                            imageView.setImageBitmap(it)
                        } ?: run {
                            // Use default image if decoding fails
                            imageView.setImageResource(R.drawable.junaid1)
                        }
                    } else {
                        // Use default image if no profile picture
                        imageView.setImageResource(R.drawable.junaid1)
                    }
                } else {
                    // Use default image if user data doesn't exist
                    imageView.setImageResource(R.drawable.junaid1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to load user profile image: ${error.message}")
                // Set default image on error
                imageView.setImageResource(R.drawable.junaid1)
            }
        })
    }

    // Helper function to decode Base64 string to Bitmap
    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
            null
        }
    }

    // Implementation of PostAdapter.OnPostInteractionListener

    override fun onLikeClicked(postId: String, isLiked: Boolean) {
        // TODO: Implement like functionality with API
        Toast.makeText(context, "Like functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onCommentClicked(postId: String) {
        // TODO: Implement comment functionality with API
        Toast.makeText(context, "Comment functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onShareClicked(postId: String) {
        // TODO: Implement share functionality with API
        Toast.makeText(context, "Share functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onSaveClicked(postId: String, isSaved: Boolean) {
        // TODO: Implement save functionality with API
        Toast.makeText(context, "Save functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileClicked(userId: String) {
        // TODO: Implement profile navigation with API
        Toast.makeText(context, "Profile navigation coming soon", Toast.LENGTH_SHORT).show()
    }

    override fun onMenuClicked(post: Post, position: Int) {
        // TODO: Implement menu functionality with API
        Toast.makeText(context, "Menu functionality coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun deletePost(postId: String) {
        // Confirm deletion with dialog
        // Then delete from Firebase

        FirebaseDatabase.getInstance().getReference("posts")
            .child(postId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addStory(storyImage: String, caption: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                currentUser?.let { user ->
                    val story = Story(
                        storyId = UUID.randomUUID().toString(), // Generate a unique ID
                        userId = user.uid,
                        username = user.displayName ?: "Unknown User",
                        userProfileImage = user.photoUrl?.toString() ?: "",
                        storyImageUrl = storyImage,
                        caption = caption,
                        timestamp = System.currentTimeMillis(),
                        expiryTimestamp = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
                    )

                    val client = OkHttpClient()
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val json = JSONObject().apply {
                        put("id", story.storyId)
                        put("userId", story.userId)
                        put("username", story.username)
                        put("userProfileImage", story.userProfileImage)
                        put("storyImageUrl", story.storyImageUrl)
                        put("caption", story.caption)
                        put("timestamp", story.timestamp)
                        put("expiryTimestamp", story.expiryTimestamp)
                    }

                    val requestBody = json.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("http://localhost:3000/api/stories/")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Story added successfully", Toast.LENGTH_SHORT).show()
                            loadStories() // Refresh stories
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to add story", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error adding story: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}