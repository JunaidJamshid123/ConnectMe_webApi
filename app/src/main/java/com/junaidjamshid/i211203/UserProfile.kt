package com.junaidjamshid.i211203

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.junaidjamshid.i211203.Adapters.PostGridAdapter
import com.junaidjamshid.i211203.models.Post

class UserProfile : AppCompatActivity() {

    // UI Components
    private var userId: String? = null
    private lateinit var currentUserId: String
    private lateinit var userNameText: TextView
    private lateinit var userBioText: TextView
    private lateinit var userFollowersCount: TextView
    private lateinit var userFollowingCount: TextView
    private lateinit var userPostsCount: TextView
    private lateinit var userProfileImage: ImageView
    private lateinit var followBtn: AppCompatButton
    private lateinit var messageBtn: AppCompatButton
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var emptyPostsView: View

    // Posts RecyclerView components
    private lateinit var postGridAdapter: PostGridAdapter
    private val postsList = ArrayList<Post>()

    // Firebase Reference
    private val realtimeDb = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Follow Status
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        supportActionBar?.hide()

        // Initialize UI Components
        initializeViews()

        // Check Authentication
        checkAuthentication()

        // Setup Click Listeners
        setupClickListeners()

        // Setup RecyclerView
        setupRecyclerView()

        // Load User Profile Data
        loadUserProfile()
    }

    private fun initializeViews() {
        userNameText = findViewById(R.id.user_name_text)
        userBioText = findViewById(R.id.user_bio_text)
        userFollowersCount = findViewById(R.id.user_followers_count)
        userFollowingCount = findViewById(R.id.user_following_count)
        userPostsCount = findViewById(R.id.user_posts_count)
        userProfileImage = findViewById(R.id.user_profile_image)
        followBtn = findViewById(R.id.btn_follow)
        messageBtn = findViewById(R.id.btn_message)
        postsRecyclerView = findViewById(R.id.posts_recycler_view)
        emptyPostsView = findViewById(R.id.empty_posts_view)
    }

    private fun setupRecyclerView() {
        // Create a GridLayoutManager with 3 columns
        val gridLayoutManager = GridLayoutManager(this, 3)
        postsRecyclerView.layoutManager = gridLayoutManager

        // Initialize the adapter
        postGridAdapter = PostGridAdapter(this, postsList)
        postsRecyclerView.adapter = postGridAdapter
    }

    private fun checkAuthentication() {
        currentUserId = auth.currentUser?.uid
            ?: run {
                Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
    }

    private fun setupClickListeners() {
        // Back Button
        findViewById<ImageView>(R.id.back_button).setOnClickListener {
            finish()
        }

        // Follow Button
        followBtn.setOnClickListener {
            toggleFollowStatus()
        }

        // Message Button
        messageBtn.setOnClickListener {
            // Pass the OTHER user's ID (the profile being viewed), not the current user's ID
            val intent = Intent(this, chats::class.java)
            intent.putExtra("USER_ID", userId)  // Use the userId of the profile being viewed
            startActivity(intent)
        }
    }

    private fun loadUserProfile() {
        // Get User ID from Intent
        userId = intent.getStringExtra("USER_ID")

        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Prevent following/messaging self
        if (userId == currentUserId) {
            followBtn.visibility = View.GONE
            messageBtn.visibility = View.GONE
        }

        // Load User Data
        loadUserData()

        // Load User Posts
        loadUserPosts()
    }

    private fun loadUserData() {
        realtimeDb.child("Users").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Extract User Details
                    val username = snapshot.child("username").getValue(String::class.java) ?: "User Name"
                    val bio = snapshot.child("bio").getValue(String::class.java) ?: ""

                    // Update UI
                    updateUserDetailsUI(username, bio)

                    // Load Additional Data
                    loadProfileImage(snapshot)
                    loadFollowersCounts()
                    loadPostsCount()
                    checkFollowStatus()
                } else {
                    handleUserNotFound()
                }
            }
            .addOnFailureListener { handleUserDataLoadError(it) }
    }

    private fun loadUserPosts() {
        // Reference to posts where userId matches the viewed user
        val postsRef = realtimeDb.child("posts").orderByChild("userId").equalTo(userId)

        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postsList.clear()

                for (postSnapshot in snapshot.children) {
                    try {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            postsList.add(it)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@UserProfile, "Error parsing post: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Update posts count
                userPostsCount.text = postsList.size.toString()

                // Update view visibility based on posts count
                updatePostsViewVisibility(postsList.size)

                // Notify adapter about data change
                postGridAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfile, "Error loading posts: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUserDetailsUI(username: String, bio: String) {
        userNameText.text = username

        if (bio.isNotEmpty()) {
            userBioText.text = bio
            userBioText.visibility = View.VISIBLE
        } else {
            userBioText.visibility = View.GONE
        }
    }

    private fun loadProfileImage(userSnapshot: DataSnapshot) {
        val profileImageData = userSnapshot.child("profilePicture").getValue(String::class.java)

        if (!profileImageData.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(profileImageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                userProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                userProfileImage.setImageResource(R.drawable.default_profile)
            }
        } else {
            userProfileImage.setImageResource(R.drawable.default_profile)
        }
    }

    private fun loadFollowersCounts() {
        // Followers Count
        realtimeDb.child("followers").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                val followersCount = snapshot.childrenCount.toInt()
                userFollowersCount.text = followersCount.toString()
            }
            .addOnFailureListener {
                userFollowersCount.text = "0"
            }

        // Following Count
        realtimeDb.child("following").child(userId!!)
            .get()
            .addOnSuccessListener { snapshot ->
                val followingCount = snapshot.childrenCount.toInt()
                userFollowingCount.text = followingCount.toString()
            }
            .addOnFailureListener {
                userFollowingCount.text = "0"
            }
    }

    private fun loadPostsCount() {
        realtimeDb.child("posts")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val postsCount = snapshot.childrenCount.toInt()
                userPostsCount.text = postsCount.toString()

                // Update Posts View Visibility
                updatePostsViewVisibility(postsCount)
            }
            .addOnFailureListener {
                userPostsCount.text = "0"
                updatePostsViewVisibility(0)
            }
    }

    private fun updatePostsViewVisibility(postsCount: Int) {
        postsRecyclerView.visibility = if (postsCount > 0) View.VISIBLE else View.GONE
        emptyPostsView.visibility = if (postsCount == 0) View.VISIBLE else View.GONE
    }

    private fun checkFollowStatus() {
        val followingRef = realtimeDb.child("following")
            .child(currentUserId)
            .child(userId!!)

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isFollowing = snapshot.exists()
                updateFollowButtonUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserProfile, "Error checking follow status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleFollowStatus() {
        if (isFollowing) {
            unfollowUser()
        } else {
            followUser()
        }
    }

    private fun followUser() {
        val currentUserFollowingRef = realtimeDb.child("following")
            .child(currentUserId)
            .child(userId!!)

        val profileUserFollowersRef = realtimeDb.child("followers")
            .child(userId!!)
            .child(currentUserId)

        currentUserFollowingRef.setValue(true)
            .addOnSuccessListener {
                profileUserFollowersRef.setValue(true)
                    .addOnSuccessListener {
                        isFollowing = true
                        updateFollowButtonUI()
                        updateFollowersCount(true)
                        Toast.makeText(this, "Followed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        currentUserFollowingRef.removeValue()
                        Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unfollowUser() {
        val currentUserFollowingRef = realtimeDb.child("following")
            .child(currentUserId)
            .child(userId!!)

        val profileUserFollowersRef = realtimeDb.child("followers")
            .child(userId!!)
            .child(currentUserId)

        currentUserFollowingRef.removeValue()
            .addOnSuccessListener {
                profileUserFollowersRef.removeValue()
                    .addOnSuccessListener {
                        isFollowing = false
                        updateFollowButtonUI()
                        updateFollowersCount(false)
                        Toast.makeText(this, "Unfollowed successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        currentUserFollowingRef.setValue(true)
                        Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFollowButtonUI() {
        followBtn.text = if (isFollowing) "Unfollow" else "Follow"
    }

    private fun updateFollowersCount(isFollowing: Boolean) {
        val followersCountRef = realtimeDb.child("Users")
            .child(userId!!)
            .child("followersCount")

        followersCountRef.get()
            .addOnSuccessListener { snapshot ->
                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                val newCount = if (isFollowing) currentCount + 1 else maxOf(currentCount - 1, 0)

                followersCountRef.setValue(newCount)
                    .addOnSuccessListener {
                        loadFollowersCounts()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error updating followers count", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error retrieving followers count", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleUserNotFound() {
        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun handleUserDataLoadError(e: Exception) {
        Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
        finish()
    }
}