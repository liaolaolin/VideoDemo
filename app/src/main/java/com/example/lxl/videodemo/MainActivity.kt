package com.example.lxl.videodemo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.jump_to_decode).setOnClickListener {
            val intent = Intent(this@MainActivity, DecodeAndPlayActivity::class.java)
            startActivity(intent)
        }

    }
}
