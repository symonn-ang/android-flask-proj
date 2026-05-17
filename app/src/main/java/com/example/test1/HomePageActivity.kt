package com.example.test1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.OkHttpClient
import java.io.File
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import android.provider.MediaStore

class HomePageActivity : AppCompatActivity() {

    private lateinit var iconProfilePic: ImageView
    private lateinit var recyclerPosts: RecyclerView
    private var selectedPostImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    data class Post(
        val id: Int,
        val post_user_id: Int,
        val username: String,
        val email: String,
        val profilepic: String?,
        val post_text: String,
        val post_image: String?,
        val createdAt: String,
        var likeCount: Int,
        var commentCount: Int,
        var isLiked: Boolean = false
    )

    companion object {
        private const val CROP_PROFILE = 1001
        private const val CROP_POST = 1002
        private const val CAMERA_PERMISSION_CODE = 2001
//        add here if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)

        val etPostText = findViewById<EditText>(R.id.etPostText)
        val btnPost = findViewById<Button>(R.id.btnPost)
        val imgSelected = findViewById<ImageView>(R.id.imgSelected)
        val btnRemoveImage = findViewById<ImageButton>(R.id.btnRemoveImage)

        iconProfilePic = findViewById(R.id.iconProfilePic)
        recyclerPosts = findViewById(R.id.recyclerPosts)
        recyclerPosts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        recyclerPosts.setHasFixedSize(false)
        recyclerPosts.isNestedScrollingEnabled = false


        val editProfPicLayout = findViewById<Button>(R.id.editProfPicLayout)

        editProfPicLayout.setOnClickListener {
            openGallery()
        }

        val usernameText = findViewById<TextView>(R.id.usernameLayout)
        val emailText = findViewById<TextView>(R.id.emailLayout)
        val dateText = findViewById<TextView>(R.id.dateLayout)

        val profilepic = intent.getStringExtra("profilepic")
        val username = intent.getStringExtra("username")
        val email = intent.getStringExtra("email")
        val createdAt = intent.getStringExtra("createdAt")

        val logoutBtn = findViewById<ImageView>(R.id.logoutBtn)

        usernameText.text = username
        emailText.text = email
        dateText.text = createdAt

        if (!profilepic.isNullOrEmpty()) {

            Thread {
                try {
                    val url = java.net.URL(profilepic)
                    val bitmap = android.graphics.BitmapFactory.decodeStream(url.openStream())

                    runOnUiThread {
                        iconProfilePic.setImageBitmap(bitmap)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        logoutBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnImage).setOnClickListener {
            pickPostImage.launch("image/*")
        }
//        or dis
        findViewById<ImageButton>(R.id.btnCamera).setOnClickListener {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                openCamera()

            } else {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        }

        btnPost.setOnClickListener {

            val text = etPostText.text.toString()
            if (text.isBlank() && selectedPostImageUri == null) {
                Toast.makeText(this, "Please enter a twoot or something", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userId = intent.getIntExtra("id", -1)

            Thread {

                try {
                    val builder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("user_id", userId.toString())
                        .addFormDataPart("post_text", text)

                    if (selectedPostImageUri != null) {
                        val inputStream = contentResolver.openInputStream(selectedPostImageUri!!)
                        val bytes = inputStream!!.readBytes()

                        builder.addFormDataPart(
                            "image",
                            "post.jpg",
                            bytes.toRequestBody("image/*".toMediaType())
                        )
                    }

                    val requestBody = builder.build()

                    val request = Request.Builder()
//                        .url("http://10.0.2.2:5000/create_post")
                        .url("http://192.168.1.25:5000/create_post")
                        .post(requestBody)
                        .build()

                    val response = OkHttpClient().newCall(request).execute()

                    if (response.isSuccessful) {

                        runOnUiThread {

                            loadPosts()

                            etPostText.text.clear()

                            selectedPostImageUri = null

                            imgSelected.setImageDrawable(null)
                            imgSelected.visibility = View.GONE

                            btnRemoveImage.visibility = View.GONE
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }.start()
        }
        btnRemoveImage.setOnClickListener {

            selectedPostImageUri = null

            imgSelected.setImageDrawable(null)

            imgSelected.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }

        loadPosts()

    }



    private val pickPostImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            if (uri != null) {
//                selectedPostImageUri = uri
//                findViewById<ImageView>(R.id.imgSelected).setImageURI(uri)
//                findViewById<ImageView>(R.id.imgSelected).visibility = View.VISIBLE
//                findViewById<ImageButton>(R.id.btnRemoveImage).visibility = View.VISIBLE
//            }
            if (uri != null) {
                startCrop(uri, CROP_POST)
            }
        }
//    or dis
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && cameraImageUri != null) {

                startCrop(cameraImageUri!!, CROP_POST)
            }
        }

    private fun loadPosts() {

        Thread {

            try {
                val userId = intent.getIntExtra("id", -1)
                val request = Request.Builder()
                    //                .url("http://10.0.2.2:5000/posts?user_id=$userId")
                    .url("http://192.168.1.25:5000/posts?user_id=$userId")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string()

                val jsonArray = org.json.JSONArray(body)
                val postList = mutableListOf<Post>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    postList.add(
                        Post(
                            obj.getInt("id"),
                            obj.getInt("user_id"),
                            obj.getString("username"),
                            obj.getString("email"),
                            obj.optString("profilepic", null),
                            obj.getString("post_text"),
                            obj.optString("post_image", null),
                            obj.getString("createdAt"),
                            obj.optInt("likeCount", 0),
                            obj.optInt("commentCount", 0),
                            obj.optBoolean("isLiked", false)
                        )
                    )
                }
                lateinit var adapter: PostAdapter
                adapter = PostAdapter(
                    userId,
                    postList,
                    onLikeClick = { position, post ->

                        val userId = intent.getIntExtra("id", -1)

                        PostActions.toggleLike(post.id, userId) { success, liked ->

                            if (success) {

                                runOnUiThread {

                                    post.isLiked = liked

                                    if (liked) {
                                        post.likeCount += 1
                                    } else {
                                        post.likeCount -= 1
                                    }

                                    adapter.notifyItemChanged(position)
                                }
                            }
                        }
                    },
                    onDeleteClick = { position, post ->
                        val userId = intent.getIntExtra("id", -1)

                        PostActions.deletePost(post.id, userId) { success ->
                            if (success) {
                                runOnUiThread {
                                    postList.removeAt(position)
                                    adapter.notifyItemRemoved(position)
                                }

                            }
                        }
                    })
                runOnUiThread {

                    recyclerPosts.adapter = adapter
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }.start()
    }



    // edit prof start here vvvv
    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            used lib here instead, GitHub ucrop check ra dependency n manifest
            if (uri != null) {
                startCrop(uri, CROP_PROFILE)
            }
        }

    private fun openGallery() {
        pickImage.launch("image/*")
    }
//    or dis
    private fun openCamera() {

        val imageFile = File(cacheDir, "camera_photo.jpg")

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        takePicture.launch(cameraImageUri!!)
    }

    private fun uploadImage(uri: Uri) {

        Thread {

            try {

                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream!!.readBytes()
                val userId = intent.getIntExtra("id", -1)

//                    .addFormDataPart("user_id", "1")
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("user_id", userId.toString())
                    .addFormDataPart(
                        "image",
                        "profile.jpg",
                        bytes.toRequestBody("image/*".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
//                .url("http://10.0.2.2:5000/upload_profile_pic")
                    .url("http://192.168.1.25:5000/upload_profile_pic")
//                .url("http://192.168.1.7:5000/upload_profile_pic")
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                client.newCall(request).execute()

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }.start()
    }

    private fun startCrop(uri: Uri, requestCode: Int) {
        val destUri = Uri.fromFile(File(cacheDir, "cropped_${requestCode}.jpg"))

        UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .start(this, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            CROP_PROFILE -> {
                if (resultCode == RESULT_OK && data != null) {

                    val resultUri = UCrop.getOutput(data)

                    if (resultUri != null) {
                        iconProfilePic.setImageURI(resultUri)
                        uploadImage(resultUri)
                    }
                }
            }

            CROP_POST -> {
                if (resultCode == RESULT_OK && data != null) {

                    val resultUri = UCrop.getOutput(data)

                    if (resultUri != null) {
                        selectedPostImageUri = resultUri

                        findViewById<ImageView>(R.id.imgSelected).apply {
                            setImageURI(resultUri)
                            visibility = View.VISIBLE
                        }

                        findViewById<ImageButton>(R.id.btnRemoveImage).visibility = View.VISIBLE
                    }
                }
            }

            UCrop.RESULT_ERROR -> {
                val error = UCrop.getError(data!!)
                error?.printStackTrace()
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_CODE) {

            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                openCamera()

            } else {

                Toast.makeText(
                    this,
                    "Camera permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}