package com.junaidjamshid.i211203.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.junaidjamshid.i211203.ProfileActivity
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.UserProfile
import com.junaidjamshid.i211203.models.Post
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(private val context: Context) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var postList: MutableList<Post> = mutableListOf()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    // Listener for post interactions
    interface OnPostInteractionListener {
        fun onLikeClicked(postId: String, isLiked: Boolean)
        fun onCommentClicked(postId: String)
        fun onShareClicked(postId: String)
        fun onSaveClicked(postId: String, isSaved: Boolean)
        fun onProfileClicked(userId: String)
        fun onMenuClicked(post: Post, position: Int)
    }

    private var listener: OnPostInteractionListener? = null

    fun setOnPostInteractionListener(listener: OnPostInteractionListener) {
        this.listener = listener
    }

    fun setPosts(posts: List<Post>) {
        this.postList.clear()
        this.postList.addAll(posts)
        notifyDataSetChanged()
    }

    fun addPost(post: Post) {
        this.postList.add(0, post)
        notifyItemInserted(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // Set post data
        holder.authorName.text = post.username
        holder.postCaption.text = post.caption
        holder.timestamp.text = getTimeAgo(post.timestamp)
        holder.locationText.text = "Your Location"

        // Load profile image
        if (!post.userProfileImage.isNullOrEmpty()) {
            try {
                val bitmap = decodeBase64Image(post.userProfileImage)
                bitmap?.let {
                    holder.profileImage.setImageBitmap(it)
                } ?: run {
                    holder.profileImage.setImageResource(R.drawable.junaid1)
                }
            } catch (e: Exception) {
                holder.profileImage.setImageResource(R.drawable.junaid1)
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.junaid1)
        }

        // Load post image
        if (!post.postImageUrl.isNullOrEmpty()) {
            try {
                val bitmap = decodeBase64Image(post.postImageUrl)
                bitmap?.let {
                    holder.postImage.setImageBitmap(it)
                } ?: run {
                    holder.postImage.setImageResource(R.drawable.junaid1)
                }
            } catch (e: Exception) {
                holder.postImage.setImageResource(R.drawable.junaid1)
            }
        } else {
            holder.postImage.setImageResource(R.drawable.junaid1)
        }

        // Set likes count
        holder.likesCount.text = "${post.likes.size} likes"

        // Set click listeners
        setupClickListeners(holder, post, position)
    }

    override fun getItemCount(): Int = postList.size

    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - timestamp

        val seconds = timeDifference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "JUST NOW"
            minutes < 60 -> "$minutes MINUTES AGO"
            hours < 24 -> "$hours HOURS AGO"
            days < 7 -> "$days DAYS AGO"
            else -> {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(timestamp)).uppercase(Locale.getDefault())
            }
        }
    }

    private fun setupClickListeners(holder: PostViewHolder, post: Post, position: Int) {
        // Heart/Like button click
        holder.heartIcon.setOnClickListener {
            listener?.onLikeClicked(post.postId, true)
        }

        // Comment button click
        holder.commentIcon.setOnClickListener {
            listener?.onCommentClicked(post.postId)
        }

        // Share button click
        holder.sendIcon.setOnClickListener {
            listener?.onShareClicked(post.postId)
        }

        // Save button click
        holder.saveIcon.setOnClickListener {
            listener?.onSaveClicked(post.postId, true)
        }

        // Profile click
        holder.profileImage.setOnClickListener {
            navigateToUserProfile(post.userId)
        }

        // Username click
        holder.usernameText.setOnClickListener {
            navigateToUserProfile(post.userId)
        }

        // Menu dots click
        holder.menuDots.setOnClickListener {
            listener?.onMenuClicked(post, position)
        }
    }

    private fun navigateToUserProfile(userId: String) {
        if (userId == currentUser?.uid) {
            // Navigate to own profile
            val intent = Intent(context, UserProfile::class.java)
            context.startActivity(intent)
        } else {
            // Navigate to other user's profile
            val intent = Intent(context, ProfileActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
            context.startActivity(intent)
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.profile_image)
        val usernameText: TextView = itemView.findViewById(R.id.username_text)
        val locationText: TextView = itemView.findViewById(R.id.location_text)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val heartIcon: ImageView = itemView.findViewById(R.id.heart)
        val commentIcon: ImageView = itemView.findViewById(R.id.comment)
        val sendIcon: ImageView = itemView.findViewById(R.id.send)
        val saveIcon: ImageView = itemView.findViewById(R.id.save)
        val likesCount: TextView = itemView.findViewById(R.id.likes_count)
        val authorName: TextView = itemView.findViewById(R.id.author_name)
        val postCaption: TextView = itemView.findViewById(R.id.post_caption)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val menuDots: ImageView = itemView.findViewById(R.id.menu_dots)
    }
}