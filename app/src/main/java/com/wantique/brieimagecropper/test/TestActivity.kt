package com.wantique.brieimagecropper.test

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.wantique.brieimagecropper.R
import com.wantique.brieimagecropper.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)

        binding.imageDecoderButton.setOnClickListener {
            startActivity(Intent(this@TestActivity, ImageDecoderActivity::class.java))
        }

        binding.mediaStoreButton.setOnClickListener {
            startActivity(Intent(this@TestActivity, MediaStoreActivity::class.java))
        }

        binding.bitmapRegionDecoderButton.setOnClickListener {
            startActivity(Intent(this@TestActivity, BitmapRegionDecoderActivity::class.java))
        }
    }
}