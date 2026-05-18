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