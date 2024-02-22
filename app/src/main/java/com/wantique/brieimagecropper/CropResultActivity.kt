package com.wantique.brieimagecropper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.wantique.brieimagecropper.databinding.ActivityCropResultBinding

class CropResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCropResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_crop_result)

        intent.getStringExtra("CROPPED_IMAGE_URI")?.let {
            Glide.with(binding.croppedImageView.context)
                .load(it.toUri())
                .into(binding.croppedImageView)
        }
    }
}