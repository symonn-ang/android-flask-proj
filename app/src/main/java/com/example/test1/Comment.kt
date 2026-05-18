package com.example.test1

data class Comment(
    val id: Int,
    val user_id: Int,
    val post_id: Int,
    val username: String,
    val profilepic: String?,
    val comment_text: String,
    val createdAt: String
)