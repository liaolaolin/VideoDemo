package com.example.lxl.videodemo

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class VideoSurfaceViewWrapper(val videoView: SurfaceView) {

    lateinit var decoder: VideoDecoder
    lateinit var surface: Surface

    lateinit var asyncDecoder: AsyncVideoDecoder

    init {
        // to get surface
        videoView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                surface = holder.surface
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
            }
        })
    }


    fun start(videoPath: String) {
        Thread {
//            decoder = VideoDecoder(surface, videoPath)
//            decoder.start()
            asyncDecoder = AsyncVideoDecoder(surface, videoPath)
            asyncDecoder.start()
        }.start()

    }

}