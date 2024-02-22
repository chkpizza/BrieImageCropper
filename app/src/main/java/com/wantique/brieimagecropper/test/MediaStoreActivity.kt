package com.wantique.brieimagecropper.test

import android.graphics.ImageDecoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.wantique.brieimagecropper.R
import com.wantique.brieimagecropper.databinding.ActivityMediaStoreBinding
import com.wantique.cropper.getExifOrientation

class MediaStoreActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaStoreBinding
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null) {
            Toast.makeText(this@MediaStoreActivity, "Exif Rotation: ${getExifOrientation(this@MediaStoreActivity, uri)}", Toast.LENGTH_SHORT).show()
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            binding.imageView.setImageBitmap(bitmap)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_media_store)

        binding.loadImageButton.setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}