package com.example.lxl.videodemo

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface

class VideoDecoder(val surface: Surface, val path: String) {

    lateinit var videoExtractor: MediaExtractor
    lateinit var videoDecoder: MediaCodec

    init {
        initVideo()
        initAudio()
    }

    private fun initVideo() {
        try {
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(path)
            for (i in 0..(videoExtractor.trackCount - 1)) {
                val mediaFormat = videoExtractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoDecoder = MediaCodec.createDecoderByType(mime)
                    videoDecoder.configure(mediaFormat, surface, null, 0)
                    videoDecoder.start()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun initAudio() {

    }

    fun start() {
        Thread {
            playVideo()
        }.start()

        Thread {
            playAudio()
        }.start()
    }

    private fun playAudio() {

    }


    private fun playVideo() {
        var playEnd = false
        var eos = false
        val startMillis = System.currentTimeMillis()
        while (!playEnd) {
            if (!eos) {
                // videoExtractor frame and put it into videoDecoder
                eos = decodeFrame(videoDecoder, videoExtractor)
            }
            val bufferInfo = MediaCodec.BufferInfo()
            val outputIndex = videoDecoder.dequeueOutputBuffer(bufferInfo, -1)
            when (outputIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED,
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // log
                }
                else -> {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        playEnd = true
                    } else {
                        delay(bufferInfo, startMillis)
                        videoDecoder.releaseOutputBuffer(outputIndex, true)
                    }
                }
            }
        }
        videoDecoder.stop()
        videoDecoder.release()
        videoExtractor.release()
    }

    private fun delay(bufferInfo: MediaCodec.BufferInfo, startMillis: Long) {
        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                break
            }
        }
    }

    private fun decodeFrame(decoder: MediaCodec, extractor: MediaExtractor): Boolean {
        var eos = false
        val inputIndex = decoder.dequeueInputBuffer(-1)
        if (inputIndex >= 0) {
            val inputBuffer = decoder.getInputBuffer(inputIndex)
            val sampleSize = extractor.readSampleData(inputBuffer, 0)
            if (sampleSize < 0) {
                eos = true
                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            } else {
                decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0)
                extractor.advance()
            }
        }
        return eos
    }

}