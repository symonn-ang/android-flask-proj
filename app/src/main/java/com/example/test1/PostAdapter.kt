package com.example.test1

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.concurrent.TimeUnit

class PostAdapter(
    private val posts: List<HomePageActivity.Post>
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val txtUsername: TextView = view.findViewById(R.id.txtUsername)
        val txtEmail: TextView = view.findViewById(R.id.txtEmail)
        val txtPost: TextView = view.findViewById(R.id.txtPost)
        val imgPost: ImageView = view.findViewById(R.id.imgPost)
        val timeStamp: TextView = view.findViewById(R.id.timeStampLayout)
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

            "Unknown"
        }
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val post = posts[position]

        holder.txtUsername.text = post.username
        holder.txtEmail.text = post.email
        holder.txtPost.text = post.post_text
        holder.timeStamp.text = getTimeAgo(post.createdAt)

        // for profpic
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

        if (
            !post.post_image.isNullOrEmpty() &&
            post.post_image != "null"
        ) {

            holder.imgPost.visibility = View.VISIBLE

            Thread {
                try {

                    val bitmap = BitmapFactory.decodeStream(
                        URL(post.post_image).openStream()
                    )

                    holder.itemView.post {
                        holder.imgPost.setImageBitmap(bitmap)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        } else {

            holder.imgPost.setImageBitmap(null)
            holder.imgPost.setImageDrawable(null)
            holder.imgPost.visibility = View.GONE
        }
    }
}