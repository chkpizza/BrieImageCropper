package com.wantique.brieimagecropper.test

import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.wantique.brieimagecropper.R
import com.wantique.brieimagecropper.databinding.ActivityImageDecoderBinding
import com.wantique.cropper.getExifOrientation

@RequiresApi(Build.VERSION_CODES.P)
class ImageDecoderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImageDecoderBinding

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null) {
            Toast.makeText(this@ImageDecoderActivity, "Exif Rotation: ${getExifOrientation(this@ImageDecoderActivity, uri)}", Toast.LENGTH_SHORT).show()
            val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))

            binding.imageView.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_image_decoder)

        binding.loadImageButton.setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}