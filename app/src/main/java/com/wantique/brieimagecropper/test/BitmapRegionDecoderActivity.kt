package com.wantique.brieimagecropper.test

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.wantique.brieimagecropper.R
import com.wantique.brieimagecropper.databinding.ActivityBitmapRegionDecoderBinding
import com.wantique.cropper.getExifOrientation

class BitmapRegionDecoderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBitmapRegionDecoderBinding
    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri != null) {
            Toast.makeText(this@BitmapRegionDecoderActivity, "Exif Rotation: ${getExifOrientation(this@BitmapRegionDecoderActivity, uri)}", Toast.LENGTH_SHORT).show()
            contentResolver.openInputStream(uri)?.let {
                val bitmapRegionDecoder = BitmapRegionDecoder.newInstance(it, false)
                if(bitmapRegionDecoder != null) {
                    val rect = Rect(0, 0, bitmapRegionDecoder.width, bitmapRegionDecoder.height)
                    val bitmap = bitmapRegionDecoder.decodeRegion(rect, BitmapFactory.Options())
                    binding.imageView.setImageBitmap(bitmap)
                }
                it.close()
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_bitmap_region_decoder)

        binding.loadImageButton.setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}