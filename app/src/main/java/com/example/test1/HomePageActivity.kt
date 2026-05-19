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
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.view.LayoutInflater
//import android.provider.MediaStore

class HomePageActivity : AppCompatActivity() {

    private lateinit var iconProfilePic: ImageView
    private lateinit var recyclerPosts: RecyclerView
    private var selectedPostImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private lateinit var editPostImage: EditPostImage
    private var pendingEditImageUri: Uri? = null
    private var editingPostPosition: Int = -1
    private var editingImagePreview: ImageView? = null
    private var editingRemoveButton: ImageButton? = null
    private var isEditingPost = false
    private val postList = mutableListOf<Post>()
    private lateinit var adapter: PostAdapter
    lateinit var commentAdapter: CommentAdapter
    private var removeExistingImage = false

    data class Post(
        val id: Int,
        val post_user_id: Int,
        val username: String,
        val email: String,
        val profilepic: String?,
        var post_text: String,
        var post_image: String?,
        val createdAt: String,
        var likeCount: Int,
        var commentCount: Int,
        var isLiked: Boolean = false
    )

    companion object {
        private const val CROP_PROFILE = 1001
        private const val CROP_NEW_POST = 1002
        private const val CROP_EDIT_POST = 1003
        private const val CAMERA_PERMISSION_CODE = 2001
//        add here if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)
        findViewById<ImageButton>(R.id.btnHomePage)
            .setImageResource(R.drawable.baseline_person_24)

        findViewById<ImageButton>(R.id.btnFeedPage)
            .setImageResource(R.drawable.outline_home_24)

        val etPostText = findViewById<EditText>(R.id.etPostText)
        val btnPost = findViewById<Button>(R.id.btnPost)
        val imgSelected = findViewById<ImageView>(R.id.imgSelected)
        val btnRemoveImage = findViewById<ImageButton>(R.id.btnRemoveImage)

        iconProfilePic = findViewById(R.id.iconProfilePic)
        recyclerPosts = findViewById(R.id.recyclerPosts)
        recyclerPosts.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val userId = intent.getIntExtra("id", -1)

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
                            val index = postList.indexOfFirst { it.id == post.id }

                            if (index != -1) {
                                postList.removeAt(index)

                                adapter.notifyItemRemoved(index)

                                adapter.notifyItemRangeChanged(index, postList.size)
                            }
                        }

                    }
                }
            },
            onEditClick = { position, post ->

                editingPostPosition = position

                val bottomSheet = BottomSheetDialog(this)
                val view = layoutInflater.inflate(R.layout.edit_post, null)

                bottomSheet.setContentView(view)
                bottomSheet.show()

                val etEdit = view.findViewById<EditText>(R.id.etEditPost)
                val imgPreview = view.findViewById<ImageView>(R.id.imgPreview)
                val btnRemoveEditImage = view.findViewById<ImageButton>(R.id.btnRemoveEditImage)
                if (post.post_image.isNullOrEmpty() || post.post_image == "null") {
                    imgPreview.setImageDrawable(null)
                    imgPreview.visibility = View.GONE
                    btnRemoveEditImage.visibility = View.GONE
                    pendingEditImageUri = null
                    post.post_image = null
                }
                isEditingPost = true
                editingImagePreview = imgPreview
                editingRemoveButton = btnRemoveEditImage
                editingPostPosition = position

                etEdit.setText(post.post_text)
                pendingEditImageUri = null
                removeExistingImage = false

                if (!post.post_image.isNullOrEmpty()) {

                    imgPreview.visibility = View.VISIBLE
                    btnRemoveEditImage.visibility = View.VISIBLE

                    Thread {
                        try {
                            val url = java.net.URL(post.post_image)
                            val bitmap = android.graphics.BitmapFactory.decodeStream(url.openStream())

                            runOnUiThread {
                                imgPreview.setImageBitmap(bitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()

                } else {
                    imgPreview.setImageDrawable(null)
                    imgPreview.visibility = View.GONE
                    btnRemoveEditImage.visibility = View.GONE
                    pendingEditImageUri = null
                    post.post_image = null
                }

                btnRemoveEditImage.setOnClickListener {

                    imgPreview.visibility = View.GONE
                    btnRemoveEditImage.visibility = View.GONE

                    imgPreview.setImageDrawable(null)
                    removeExistingImage = true
                    pendingEditImageUri = null
//                    post.post_image = null
                }

                view.findViewById<ImageButton>(R.id.btnGallery).setOnClickListener {
                    pickEditImage.launch("image/*")
                }

                view.findViewById<ImageButton>(R.id.btnCamera).setOnClickListener {
                    isEditingPost = true

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
                view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
                    pendingEditImageUri = null
                    editingImagePreview = null
                    isEditingPost = false

                    bottomSheet.dismiss()
                }

                view.findViewById<Button>(R.id.btnUpdate).setOnClickListener {

                    val cleanText = etEdit.text.toString().trim()

                    val hasText = cleanText.isNotEmpty()
                    val hasNewImage = pendingEditImageUri != null
                    val hasExistingImage = !post.post_image.isNullOrEmpty()
                    val removedImage = removeExistingImage

                    val willHaveImage =
                        hasNewImage || (hasExistingImage && !removedImage)

                    if (!hasText && !willHaveImage) {
                        Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    PostActions.editPost(
                        post.id,
                        userId,
                        etEdit.text.toString(),
                        pendingEditImageUri,
                        removeExistingImage,
                        contentResolver
                    ) { success ->

                        if (success) runOnUiThread {
                            post.post_text = etEdit.text.toString()
//                            if (pendingEditImageUri != null) {
//                                post.post_image = pendingEditImageUri.toString()
//                            }
                            loadPosts() // dis instead of vert
//                            recyclerPosts.adapter?.notifyItemChanged(position, "edit")
                            pendingEditImageUri = null
                            editingImagePreview = null
                            isEditingPost = false
                            bottomSheet.dismiss()
                        }
                    }
                }
            },
            onCommentClick = { position, post ->

                val bottomSheet = BottomSheetDialog(this)

                val view = layoutInflater.inflate(
                    R.layout.comment_modal,
                    null
                )

                bottomSheet.setContentView(view)
                bottomSheet.show()

                val imgCurrentUser =
                    view.findViewById<ImageView>(R.id.imgCurrentUser)

                val recyclerComments =
                    view.findViewById<RecyclerView>(R.id.recyclerComments)

                val etComment =
                    view.findViewById<EditText>(R.id.etCommentInput)

                val btnSend =
                    view.findViewById<ImageButton>(R.id.btnSendComment)

                val commentsList = mutableListOf<Comment>()

                fun refreshComments() {

                    PostActions.loadComments(post.id) { success, comments ->

                        if (success) {

                            runOnUiThread {

                                commentsList.clear()

                                commentsList.addAll(comments)

                                commentAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                val currentProfilePic = intent.getStringExtra("profilepic")

                if (!currentProfilePic.isNullOrEmpty()) {

                    Thread {
                        try {

                            val bitmap = android.graphics.BitmapFactory.decodeStream(
                                java.net.URL(currentProfilePic).openStream()
                            )

                            runOnUiThread {
                                imgCurrentUser.setImageBitmap(bitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }.start()
                }

                    commentAdapter =
                    CommentAdapter(

                        userId,
                        commentsList,

                        onEditClick = { comment ->

                            val dialogView = layoutInflater.inflate(
                                R.layout.edit_comment,
                                null
                            )

                            val editText =
                                dialogView.findViewById<EditText>(R.id.etEditComment)

                            editText.setText(comment.comment_text)

                            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("")
                                .setView(dialogView)
                                .setPositiveButton("Save") { _, _ ->

                                    val newText = editText.text.toString()

                                    PostActions.editComment(
                                        comment.id,
                                        userId,
                                        newText
                                    ) { success ->

                                        if (success) {
                                            runOnUiThread {
                                                refreshComments()
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .create()

                            dialog.window?.setBackgroundDrawableResource(R.drawable.edit_comment_style)

                            dialog.show()

                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(android.graphics.Color.parseColor("#1DA1F2"))

                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(android.graphics.Color.GRAY)
                        },

                        onDeleteClick = { comment ->

                            val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog)
                                .setTitle("Delete Comment")
                                .setMessage("Are you sure you want to delete this comment?")
                                .setPositiveButton("Delete") { _, _ ->

                                    PostActions.deleteComment(
                                        comment.id,
                                        userId
                                    ) { success ->

                                        if (success) {

                                            runOnUiThread {

                                                post.commentCount -= 1

                                                adapter.notifyItemChanged(position)

                                                refreshComments()
                                            }
                                        }
                                    }
                                }

                                .setNegativeButton("Cancel", null)
                                .create()

                            dialog.window?.setBackgroundDrawableResource(R.drawable.edit_comment_style)

                            dialog.show()
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(android.graphics.Color.parseColor("#FF0000"))

                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(android.graphics.Color.GRAY)




                        }
                    )

                recyclerComments.layoutManager =
                    LinearLayoutManager(this)

                recyclerComments.adapter = commentAdapter


                refreshComments()

                btnSend.setOnClickListener {

                    val commentText = etComment.text.toString()

                    if (commentText.isBlank()) {
                        return@setOnClickListener
                    }

                    PostActions.createComment(
                        userId,
                        post.id,
                        commentText
                    ) { success ->

                        if (success) {

                            runOnUiThread {

                                etComment.text.clear()

                                post.commentCount += 1

                                adapter.notifyItemChanged(position)

                                refreshComments()
                            }
                        }
                    }
                }

                view.findViewById<ImageButton>(R.id.btnClose)
                    .setOnClickListener {

                        bottomSheet.dismiss()
                    }
            }
        )

        recyclerPosts.adapter = adapter

        recyclerPosts.setHasFixedSize(false)
        recyclerPosts.isNestedScrollingEnabled = false


        val editProfPicLayout = findViewById<Button>(R.id.editProfPicLayout)

        editProfPicLayout.setOnClickListener {
            openGallery()
        }

        val btnHomePage = findViewById<ImageButton>(R.id.btnHomePage)
        val btnFeedPage = findViewById<ImageButton>(R.id.btnFeedPage)

        btnHomePage.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            passUserData(intent)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        btnFeedPage.setOnClickListener {
            val intent = Intent(this, FeedPageActivity::class.java)
            passUserData(intent)
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
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
//start now
        editPostImage = EditPostImage(this) { uri ->
            selectedPostImageUri = uri
            findViewById<ImageView>(R.id.imgSelected).apply {
                setImageURI(uri)
                visibility = View.VISIBLE
            }
            findViewById<ImageButton>(R.id.btnRemoveImage).visibility = View.VISIBLE
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

                        val responseBody = response.body?.string()

                        val json = org.json.JSONObject(responseBody!!)

                        val newPostId = json.getInt("post_id")

                        runOnUiThread {

                            etPostText.text.clear()

                            selectedPostImageUri = null

                            imgSelected.setImageDrawable(null)
                            imgSelected.visibility = View.GONE

                            btnRemoveImage.visibility = View.GONE

                            loadPosts()
                        }
                    }
//                            recyclerPosts.scrollToPosition(0)

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


    private fun passUserData(intent: Intent) {

        intent.putExtra("id", this.intent.getIntExtra("id", -1))
        intent.putExtra("username", this.intent.getStringExtra("username"))
        intent.putExtra("email", this.intent.getStringExtra("email"))
        intent.putExtra("profilepic", this.intent.getStringExtra("profilepic"))
        intent.putExtra("createdAt", this.intent.getStringExtra("createdAt"))
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
                startCrop(uri, CROP_NEW_POST)
            }
        }
//    or dis
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && cameraImageUri != null) {

                if (isEditingPost) {
                    startCrop(cameraImageUri!!, CROP_EDIT_POST)
                } else {
                    startCrop(cameraImageUri!!, CROP_NEW_POST)
                }
            }
        }

    private fun loadPosts() {

        Thread {

            try {
                val userId = intent.getIntExtra("id", -1)
                val request = Request.Builder()
                    //                .url("http://10.0.2.2:5000/posts?user_id=$userId")
//                    .url("http://192.168.1.25:5000/posts?user_id=$userId") // or
                    .url("http://192.168.1.25:5000/my_posts?user_id=$userId")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val body = response.body?.string()

                val jsonArray = org.json.JSONArray(body)

                val newPosts = mutableListOf<Post>()

                for (i in 0 until jsonArray.length()) {

                    val obj = jsonArray.getJSONObject(i)

                    newPosts.add(
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

                runOnUiThread {
                    postList.clear()
                    postList.addAll(newPosts)

                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }.start()
    }

    private val pickEditImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                isEditingPost = true
                startCrop(uri, CROP_EDIT_POST)
            }
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

        val imageFile = File(
            cacheDir,
            "camera_photo_${System.currentTimeMillis()}.jpg"
        )

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
        val destFile = File(cacheDir, "cropped_${requestCode}_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

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

            CROP_NEW_POST -> {
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {
                    selectedPostImageUri = resultUri
                    findViewById<ImageView>(R.id.imgSelected).apply {
                        setImageDrawable(null)
                        setImageURI(resultUri)
                        visibility = View.VISIBLE
                    }
                    findViewById<ImageButton>(R.id.btnRemoveImage)
                        .visibility = View.VISIBLE
                }
            }

            CROP_EDIT_POST -> {
                val resultUri = UCrop.getOutput(data!!)
                if (resultUri != null) {

                    pendingEditImageUri = resultUri

                    editingImagePreview?.let {
                        it.setImageURI(resultUri)
                        it.visibility = View.VISIBLE
                    }
                    editingRemoveButton?.visibility = View.VISIBLE

                    removeExistingImage = false
                    pendingEditImageUri = resultUri
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