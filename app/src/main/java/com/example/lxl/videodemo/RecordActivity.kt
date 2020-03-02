package com.example.lxl.videodemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class RecordActivity : AppCompatActivity() {

    val cameraManager = CameraManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        val switchBtn = findViewById<Button>(R.id.switch_camera_btn)
        val recordBtn = findViewById<Button>(R.id.record_btn)
        val picBtn = findViewById<Button>(R.id.pic_btn)

        cameraManager.listCameraInfo()
    }
}
