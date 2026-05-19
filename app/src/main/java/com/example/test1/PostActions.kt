package com.example.test1

import android.content.ContentResolver
import android.net.Uri
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object PostActions {

    private val client = OkHttpClient()


    fun loadComments(
        postId: Int,
        callback: (Boolean, List<Comment>) -> Unit
    ) {

        Thread {

            try {

                val request = Request.Builder()
//                .url("http://10.0.2.2:5000/comments?post_id=$postId")
                    .url("http://192.168.1.25:5000/comments?post_id=$postId")
                    .build()

                val response = client.newCall(request).execute()

                val body = response.body?.string()

                val jsonArray = org.json.JSONArray(body)

                val comments = mutableListOf<Comment>()

                for (i in 0 until jsonArray.length()) {

                    val obj = jsonArray.getJSONObject(i)

                    comments.add(
                        Comment(
                            obj.getInt("id"),
                            obj.getInt("user_id"),
                            obj.getInt("post_id"),
                            obj.getString("username"),
                            obj.optString("profilepic", null),
                            obj.getString("comment_text"),
                            obj.getString("createdAt")
                        )
                    )
                }

                callback(true, comments)

            } catch (e: Exception) {

                e.printStackTrace()

                callback(false, emptyList())
            }

        }.start()
    }

    fun createComment(
        userId: Int,
        postId: Int,
        commentText: String,
        callback: (Boolean) -> Unit
    ) {

        Thread {

            try {

                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("user_id", userId.toString())
                    .addFormDataPart("post_id", postId.toString())
                    .addFormDataPart("comment_text", commentText)
                    .build()

                val request = Request.Builder()
                    //                .url("http://10.0.2.2:5000/create_comment")
                    .url("http://192.168.1.25:5000/create_comment")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                callback(response.isSuccessful)

            } catch (e: Exception) {

                e.printStackTrace()

                callback(false)
            }

        }.start()
    }

    fun deleteComment(
        commentId: Int,
        userId: Int,
        callback: (Boolean) -> Unit
    ) {

        Thread {

            try {

                val body = FormBody.Builder()
                    .add("comment_id", commentId.toString())
                    .add("user_id", userId.toString())
                    .build()

                val request = Request.Builder()
//                .url("http://10.0.2.2:5000/comments?post_id=$postId")
                    .url("http://192.168.1.25:5000/delete_comment")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                callback(response.isSuccessful)

            } catch (e: Exception) {

                e.printStackTrace()

                callback(false)
            }

        }.start()
    }

    fun editComment(
        commentId: Int,
        userId: Int,
        commentText: String,
        callback: (Boolean) -> Unit
    ) {

        Thread {

            try {

                val body = FormBody.Builder()
                    .add("comment_id", commentId.toString())
                    .add("user_id", userId.toString())
                    .add("comment_text", commentText)
                    .build()

                val request = Request.Builder()
//                .url("http://10.0.2.2:5000/comments?post_id=$postId")
                    .url("http://192.168.1.25:5000/edit_comment")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                callback(response.isSuccessful)

            } catch (e: Exception) {

                e.printStackTrace()

                callback(false)
            }

        }.start()
    }

    fun toggleLike(postId: Int, userId: Int, callback: (Boolean, Boolean) -> Unit) {

        Thread {
            try {

                val body = FormBody.Builder()
                    .add("post_id", postId.toString())
                    .add("user_id", userId.toString())
                    .build()

                val request = Request.Builder()
//                .url("http://10.0.2.2:5000/toggle_like")
                    .url("http://192.168.1.25:5000/toggle_like")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string()

                // backend should return: {"liked": true/false}
                val liked = org.json.JSONObject(responseText).getBoolean("liked")

                callback(true, liked)

            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, false)
            }
        }.start()
    }

    fun editPost(
        postId: Int,
        userId: Int,
        postText: String,
        imageUri: Uri?,
        removeImage: Boolean,
        contentResolver: ContentResolver,
        callback: (Boolean) -> Unit
    ) {

        Thread {
            try {

                val builder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("post_id", postId.toString())
                    .addFormDataPart("user_id", userId.toString())
                    .addFormDataPart("post_text", postText)
                    .addFormDataPart(
                        "remove_image",
                        removeImage.toString()
                    )

                if (imageUri != null) {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bytes = inputStream!!.readBytes()

                    builder.addFormDataPart(
                        "image",
                        "edit.jpg",
                        bytes.toRequestBody("image/*".toMediaType())
                    )
                }

                val request = Request.Builder()
                    //                .url("http://10.0.2.2:5000/edit_post")
                    .url("http://192.168.1.25:5000/edit_post")
                    .post(builder.build())
                    .build()

                val response = OkHttpClient().newCall(request).execute()

                callback(response.isSuccessful)

            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }.start()
    }

    fun deletePost(postId: Int, userId: Int, callback: (Boolean) -> Unit) {

        Thread {
            try {
                val body = FormBody.Builder()
                    .add("post_id", postId.toString())
                    .add("user_id", userId.toString())
                    .build()

                val request = Request.Builder()
                    .url("http://192.168.1.25:5000/delete_post")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                callback(response.isSuccessful)

            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }.start()
    }


}