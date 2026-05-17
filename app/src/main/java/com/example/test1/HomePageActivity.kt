package com.example.test1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.yalantis.ucrop.UCrop
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.OkHttpClient
import java.io.File


class HomePageActivity : AppCompatActivity() {

    private lateinit var iconProfilePic: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homepage)

        iconProfilePic = findViewById(R.id.iconProfilePic)

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

    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            used lib here instead github ucrop
            if (uri != null) {
                startCrop(uri)
            }
        }

    private fun openGallery() {
        pickImage.launch("image/*")
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


//    github UCrop

    private fun startCrop(uri: Uri) {
        val destUri = Uri.fromFile(File(cacheDir, "cropped.jpg"))

        UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(500, 500)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {

            UCrop.REQUEST_CROP -> {
                if (resultCode == RESULT_OK && data != null) {

                    val resultUri = UCrop.getOutput(data)

                    if (resultUri != null) {
                        iconProfilePic.setImageURI(resultUri)
                        uploadImage(resultUri)
                    }

                } else if (resultCode == UCrop.RESULT_ERROR) {

                    val error = UCrop.getError(data!!)
                    error?.printStackTrace()
                }
            }
        }
    }
}