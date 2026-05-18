package com.example.test1

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.concurrent.TimeUnit

class PostAdapter(
    private val currentUserId: Int,
    private val posts: MutableList<HomePageActivity.Post>,
    private val onLikeClick: (Int, HomePageActivity.Post) -> Unit,
    private val onDeleteClick: (Int, HomePageActivity.Post) -> Unit,
    private val onEditClick: (Int, HomePageActivity.Post) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val txtUsername: TextView = view.findViewById(R.id.txtUsername)
        val txtEmail: TextView = view.findViewById(R.id.txtEmail)
        val txtPost: TextView = view.findViewById(R.id.txtPost)
        val imgPost: ImageView = view.findViewById(R.id.imgPost)
        val timeStamp: TextView = view.findViewById(R.id.timeStampLayout)
        val btnLike: ImageView = view.findViewById(R.id.btnLike)
        val likeCount: TextView = view.findViewById(R.id.likeCount)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.posts, parent, false)

        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return posts.size
    }

    private fun getTimeAgo(dateString: String): String {

        return try {

            val format = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

            val postDate: Date = format.parse(dateString)!!
            val now = Date()

            val diff = now.time - postDate.time

            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)

            when {

                seconds < 60 -> "${seconds}s ago"

                minutes < 60 -> "${minutes}m ago"

                hours < 24 -> "${hours}h ago"

                days < 7 -> "${days}d ago"

                else -> "${days / 7}w ago"
            }

        } catch (e: Exception) {

            "now"
        }
    }

    private fun showFullScreenImage(context: Context, imageUrl: String) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.post_full_image)

        val photoView: PhotoView = dialog.findViewById(R.id.fullImageView)
        val btnClose: ImageButton = dialog.findViewById(R.id.btnClose)

        Thread {
            try {
                val bitmap = BitmapFactory.decodeStream(URL(imageUrl).openStream())
                photoView.post {
                    photoView.setImageResource(R.drawable.outline_image_search_24)
                    photoView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
//        try glide
//        Glide.with(context)
//            .load(imageUrl)
//            .into(photoView)

        btnClose.setOnClickListener { dialog.dismiss() }
        photoView.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val post = posts[position]

        val isOwner = post.post_user_id == currentUserId
        val deleteBtn = holder.itemView.findViewById<ImageButton>(R.id.btnDelete)
        val editBtn = holder.itemView.findViewById<ImageButton>(R.id.btnEdit)

        holder.txtUsername.text = post.username
        holder.txtEmail.text = post.email
        holder.txtPost.text = post.post_text
        holder.timeStamp.text = getTimeAgo(post.createdAt)
        holder.likeCount.text = post.likeCount.toString()


        if (isOwner) {
            deleteBtn.alpha = 1f
            editBtn.alpha = 1f

            deleteBtn.isEnabled = true
            editBtn.isEnabled = true
        } else {
            deleteBtn.alpha = 0.3f
            editBtn.alpha = 0.3f

            deleteBtn.isEnabled = false
            editBtn.isEnabled = false
        }

//        onclick stuff vv

        holder.btnEdit.setOnClickListener {
            val currentPosition = holder.adapterPosition

            if (currentPosition != RecyclerView.NO_POSITION) {
                onEditClick(currentPosition, posts[currentPosition])
            }
        }

        holder.itemView.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            val context = holder.itemView.context
            val currentPosition = holder.adapterPosition

            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Yes") { dialog, _ ->
                    if (currentPosition != RecyclerView.NO_POSITION) {
                        onDeleteClick(currentPosition, posts[currentPosition])
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }


        if (post.isLiked) {
            holder.btnLike.setImageResource(R.drawable.baseline_favorite_24)
        } else {
            holder.btnLike.setImageResource(R.drawable.outline_favorite_24)
        }
        holder.btnLike.setOnClickListener {
            val currentPosition = holder.adapterPosition

            if (currentPosition != RecyclerView.NO_POSITION) {
                onLikeClick(currentPosition, posts[currentPosition])
            }
        }

        // for profilepic
        if (!post.profilepic.isNullOrEmpty()) {

            Thread {
                try {

                    val bitmap = BitmapFactory.decodeStream(
                        URL(post.profilepic).openStream()
                    )

                    holder.itemView.post {
                        holder.imgProfile.setImageBitmap(bitmap)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
        if (!post.post_image.isNullOrEmpty() && post.post_image != "null") {

            holder.imgPost.visibility = View.VISIBLE

            Thread {
                try {
                    val bitmap = BitmapFactory.decodeStream(
                        URL(post.post_image).openStream()
                    )

                    holder.itemView.post {
                        holder.imgPost.setImageBitmap(bitmap)

                        holder.imgPost.setOnClickListener {
                            showFullScreenImage(holder.itemView.context, post.post_image!!)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        } else {
            holder.imgPost.visibility = View.GONE
            holder.imgPost.setImageBitmap(null)
            holder.imgPost.setImageDrawable(null)
            holder.imgPost.setOnClickListener(null)
        }

    }
}