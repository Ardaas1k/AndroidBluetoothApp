package com.example.bluetoothapp

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_gallery.*


class GalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val imageUriTmp = intent.getStringExtra("imageUri")
        if(imageUriTmp!=null){
            val imageUri= Uri.parse(imageUriTmp)
            imageView2.setImageURI(imageUri)
        }
    }
}
