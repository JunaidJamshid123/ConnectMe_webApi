package com.junaidjamshid.i211203.Adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.junaidjamshid.i211203.R
import com.junaidjamshid.i211203.models.Story
import de.hdodenhof.circleimageview.CircleImageView

class StoryAdapter(private val context: Context) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    private var stories: MutableList<Story> = mutableListOf()

    interface OnStoryClickListener {
        fun onStoryClick(story: Story, position: Int)
    }

    private var listener: OnStoryClickListener? = null

    fun setOnStoryClickListener(listener: OnStoryClickListener) {
        this.listener = listener
    }

    fun setStories(stories: List<Story>) {
        this.stories.clear()
        this.stories.addAll(stories)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.stroies, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]

        // Set username
        holder.usernameText.text = story.username

        // Load profile image
        if (!story.userProfileImage.isNullOrEmpty()) {
            try {
                val bitmap = decodeBase64Image(story.userProfileImage)
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

        // Set click listener
        holder.itemView.setOnClickListener {
            listener?.onStoryClick(story, position)
        }
    }

    override fun getItemCount(): Int = stories.size

    private fun decodeBase64Image(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: CircleImageView = itemView.findViewById(R.id.imgStoryProfile)
        val usernameText: TextView = itemView.findViewById(R.id.tvUsername)
    }
}