package com.example.test1

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

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