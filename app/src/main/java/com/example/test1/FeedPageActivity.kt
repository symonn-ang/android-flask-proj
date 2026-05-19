package com.example.test1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import com.yalantis.ucrop.UCrop
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
//import com.example.test1.HomePageActivity.Companion.CAMERA_PERMISSION_CODE
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class FeedPageActivity : AppCompatActivity() {

    private lateinit var recyclerPosts: RecyclerView
    private lateinit var adapter: PostAdapter
    lateinit var commentAdapter: CommentAdapter

    private val postList = mutableListOf<HomePageActivity.Post>()

    private var editingPostPosition: Int = -1
    private var isEditingPost = false
    private var pendingEditImageUri: android.net.Uri? = null
    private var removeExistingImage = false

    companion object {
        private const val CROP_EDIT_POST = 3001
        private const val CROP_NEW_EDIT_POST = 3002
        private const val CAMERA_PERMISSION_CODE = 2001
    }

    private var editingImagePreview: ImageView? = null
    private var editingRemoveButton: ImageButton? = null
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.feedpage)
        findViewById<ImageButton>(R.id.btnHomePage)
            .setImageResource(R.drawable.outline_person_24)

        findViewById<ImageButton>(R.id.btnFeedPage)
            .setImageResource(R.drawable.baseline_home_24)

        recyclerPosts = findViewById(R.id.recyclerPosts)

        recyclerPosts.layoutManager =
            LinearLayoutManager(this)

        val userId = intent.getIntExtra("id", -1)
        val logoutBtn = findViewById<ImageView>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        adapter = PostAdapter(

            userId,
            postList,

            onLikeClick = { position, post ->

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

                PostActions.deletePost(post.id, userId) { success ->

                    if (success) {

                        runOnUiThread {

                            val index =
                                postList.indexOfFirst { it.id == post.id }

                            if (index != -1) {

                                postList.removeAt(index)

                                adapter.notifyItemRemoved(index)

                                adapter.notifyItemRangeChanged(
                                    index,
                                    postList.size
                                )
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

                val currentProfilePic =
                    intent.getStringExtra("profilepic")

                if (!currentProfilePic.isNullOrEmpty()) {

                    Thread {

                        try {

                            val bitmap =
                                BitmapFactory.decodeStream(
                                    java.net.URL(currentProfilePic)
                                        .openStream()
                                )

                            runOnUiThread {
                                imgCurrentUser.setImageBitmap(bitmap)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                    }.start()
                }

                commentAdapter = CommentAdapter(

                    userId,
                    commentsList,

                    onEditClick = { comment ->

                        val dialogView = layoutInflater.inflate(
                            R.layout.edit_comment,
                            null
                        )

                        val editText =
                            dialogView.findViewById<EditText>(
                                R.id.etEditComment
                            )

                        editText.setText(comment.comment_text)

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setPositiveButton("Save") { _, _ ->

                                val newText =
                                    editText.text.toString()

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
                            .show()
                    },

                    onDeleteClick = { comment ->

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
                )

                recyclerComments.layoutManager =
                    LinearLayoutManager(this)

                recyclerComments.adapter =
                    commentAdapter

                refreshComments()

                btnSend.setOnClickListener {

                    val commentText =
                        etComment.text.toString()

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

        loadPosts()

        val btnHomePage =
            findViewById<ImageButton>(R.id.btnHomePage)

        val btnFeedPage =
            findViewById<ImageButton>(R.id.btnFeedPage)

        btnHomePage.setOnClickListener {
            val homeIntent = Intent(this, HomePageActivity::class.java)
            passUserData(homeIntent)
            startActivity(homeIntent)
            overridePendingTransition(0, 0)
            finish()
        }

        btnFeedPage.setOnClickListener {
            val feedIntent = Intent(this, FeedPageActivity::class.java)
            passUserData(feedIntent)
            startActivity(feedIntent)
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private val pickEditImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                startCrop(uri, CROP_EDIT_POST)
            }
        }
    private fun openCamera() {
        val imageFile = File(
            cacheDir,
            "camera_edit_${System.currentTimeMillis()}.jpg"
        )

        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            imageFile
        )

        takePicture.launch(cameraImageUri!!)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success && cameraImageUri != null) {
                startCrop(cameraImageUri!!, CROP_EDIT_POST)
            }
        }
    private fun startCrop(uri: Uri, requestCode: Int) {
        val destFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .start(this, requestCode)
    }
    private fun loadPosts() {

        Thread {

            try {

                val userId =
                    intent.getIntExtra("id", -1)

                val request = Request.Builder()
                    .url("http://192.168.1.25:5000/posts?user_id=$userId")
                    .build()

                val response =
                    OkHttpClient().newCall(request).execute()

                val body =
                    response.body?.string()

                val jsonArray =
                    org.json.JSONArray(body)

                val newPosts =
                    mutableListOf<HomePageActivity.Post>()

                for (i in 0 until jsonArray.length()) {

                    val obj =
                        jsonArray.getJSONObject(i)

                    newPosts.add(

                        HomePageActivity.Post(

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

    private fun passUserData(intent: Intent) {

        intent.putExtra("id", this.intent.getIntExtra("id", -1))

        intent.putExtra("username", this.intent.getStringExtra("username"))

        intent.putExtra("email", this.intent.getStringExtra("email"))

        intent.putExtra("profilepic", this.intent.getStringExtra("profilepic"))

        intent.putExtra("createdAt", this.intent.getStringExtra("createdAt"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data == null) return

        when (requestCode) {

            CROP_EDIT_POST -> {
                val resultUri = UCrop.getOutput(data) ?: return

                pendingEditImageUri = resultUri

                editingImagePreview?.apply {
                    setImageURI(resultUri)
                    visibility = View.VISIBLE
                }

                editingRemoveButton?.visibility = View.VISIBLE
                removeExistingImage = false
            }
        }
    }
}