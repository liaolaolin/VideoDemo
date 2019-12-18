package com.example.lxl.videodemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.content.Intent
import android.provider.MediaStore
import android.widget.TextView

class DecodeAndPlayActivity : AppCompatActivity() {

    private val REQUEST_VIDEO_CODE = 1001

    private var videoPath = ""

    private lateinit var textureView: TextureView
    private lateinit var textureVideoWrapper: VideoTextureViewWrapper

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceVideoWrapper: VideoSurfaceViewWrapper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decode_and_play)

        textureView = findViewById(R.id.texture_videoview)
        textureVideoWrapper = VideoTextureViewWrapper(textureView)

        surfaceView = findViewById(R.id.surface_videoview)
        surfaceVideoWrapper = VideoSurfaceViewWrapper(surfaceView)

        initUI()
    }

    private fun initUI() {
        findViewById<View>(R.id.start_btn).setOnClickListener {
            surfaceVideoWrapper.start(videoPath)
        }

        findViewById<View>(R.id.choose_btn).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_VIDEO_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VIDEO_CODE) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.getData()
                val cr = this.getContentResolver()
                val cursor = cr.query(uri, null, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        val videoPath =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                        val duration =
                            cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        val size =
                            cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                        val imagePath =
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

                        findViewById<TextView>(R.id.path_tv).setText(videoPath)
                        this.videoPath = videoPath
                    }
                    cursor.close()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
