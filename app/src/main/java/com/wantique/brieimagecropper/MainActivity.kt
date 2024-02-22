package com.wantique.brieimagecropper

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.wantique.brieimagecropper.databinding.ActivityMainBinding
import com.wantique.brieimagecropper.test.TestActivity
import com.wantique.cropper.getExifOrientation
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null) {
            Toast.makeText(this@MainActivity, "Exif Rotation: ${getExifOrientation(this@MainActivity, uri)}", Toast.LENGTH_SHORT).show()
            binding.brieImageCropper.load(uri)

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.selectImageButton.setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.cropButton.setOnClickListener {
            binding.brieImageCropper.crop()?.let { bitmap ->
                val saveUri = binding.brieImageCropper.save(bitmap)
                Intent(this@MainActivity, CropResultActivity::class.java).apply {
                    putExtra("CROPPED_IMAGE_URI", saveUri.toString())
                    startActivity(this)
                }
            } ?: Toast.makeText(this@MainActivity,  "crop failed", Toast.LENGTH_SHORT).show()
        }

        binding.testButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, TestActivity::class.java))
        }
    }
}