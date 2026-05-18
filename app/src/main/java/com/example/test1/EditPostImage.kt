package com.example.test1

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File


class EditPostImage(
    private val activity: AppCompatActivity,
    private val onImageReady: (Uri) -> Unit
) {

    private var cameraUri: Uri? = null

    private val pickImage =
        activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { startCrop(it) }
        }

    private val takePicture =
        activity.registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraUri != null) {
                startCrop(cameraUri!!)
            }
        }

    fun openGallery() {
        pickImage.launch("image/*")
    }

    fun openCamera() {
        val file = File(activity.cacheDir, "edit_${System.currentTimeMillis()}.jpg")

        cameraUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.provider",
            file
        )

        takePicture.launch(cameraUri!!)
    }

    private fun startCrop(uri: Uri) {
        val dest = Uri.fromFile(
            File(activity.cacheDir, "edit_crop_${System.currentTimeMillis()}.jpg")
        )

        UCrop.of(uri, dest)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1000, 1000)
            .start(activity, UCrop.REQUEST_CROP)
    }

    fun handleCropResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == AppCompatActivity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val uri = UCrop.getOutput(data ?: return)
            uri?.let { onImageReady(it) }
        }
    }
}