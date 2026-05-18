package com.example.test1

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CommentAdapter(
    private val currentUserId: Int,
    private val comments: MutableList<Comment>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgProfile: ImageView = view.findViewById(R.id.imgProfile)
        val txtUsername: TextView = view.findViewById(R.id.txtUsername)
        val txtPost: TextView = view.findViewById(R.id.txtPost)
        val timeStamp: TextView = view.findViewById(R.id.timeStampLayout)

        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comments, parent, false)

        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {

        val comment = comments[position]

        holder.txtUsername.text = comment.username
        holder.txtPost.text = comment.comment_text
        holder.timeStamp.text = getTimeAgo(comment.createdAt)

        holder.itemView.findViewById<TextView>(R.id.txtEmail).visibility = View.GONE

        holder.btnDelete.setOnClickListener {
            onDeleteClick(comment)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(comment)
        }

        val isOwner = comment.user_id == currentUserId

        holder.btnEdit.visibility =
            if (isOwner) View.VISIBLE else View.GONE

        holder.btnDelete.visibility =
            if (isOwner) View.VISIBLE else View.GONE

        if (!comment.profilepic.isNullOrEmpty()) {

            Thread {
                try {

                    val bitmap = BitmapFactory.decodeStream(
                        URL(comment.profilepic).openStream()
                    )

                    holder.itemView.post {
                        holder.imgProfile.setImageBitmap(bitmap)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun getTimeAgo(dateString: String): String {

        return try {

            val format = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )

            val commentDate: Date = format.parse(dateString)!!
            val now = Date()

            val diff = now.time - commentDate.time

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
}