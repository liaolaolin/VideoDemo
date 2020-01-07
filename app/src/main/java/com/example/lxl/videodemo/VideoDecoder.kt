package com.example.lxl.videodemo

import android.view.Surface
import android.media.*
import java.io.IOException
import android.media.MediaCodec
import android.util.Log
import android.media.MediaExtractor


class VideoDecoder(val surface: Surface, val path: String) {

    val TAG = "VideoDecoder"

    lateinit var videoExtractor: MediaExtractor
    lateinit var videoDecoder: MediaCodec
    lateinit var audioExtractor: MediaExtractor
    lateinit var audioDecoder: MediaCodec
    lateinit var audioTrack: AudioTrack
    private var audioInputBufferSize = 0

    private val TIMEOUT_US = 30 * 1000L

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
        try {
            audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(path)
            for (i in 0..(audioExtractor.trackCount - 1)) {
                val mediaFormat = audioExtractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("audio/")) {
                    audioExtractor.selectTrack(i)
                    val audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    val audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        audioSampleRate,
                        if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    audioInputBufferSize = if (minBufferSize > 0) minBufferSize * 4 else maxInputSize
                    val frameSizeInBytes = audioChannels * 2
                    audioInputBufferSize = audioInputBufferSize / frameSizeInBytes * frameSizeInBytes
                    audioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        audioSampleRate,
                        if (audioChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        audioInputBufferSize,
                        AudioTrack.MODE_STREAM
                    )
                    audioTrack.setVolume(1f)
                    audioTrack.play()
                    try {
                        audioDecoder = MediaCodec.createDecoderByType(mime)
                        audioDecoder.configure(mediaFormat, null, null, 0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    if (audioDecoder == null) {
                        return
                    }
                    audioDecoder.start()

                    break
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
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
        val buffers = audioDecoder.getOutputBuffers()
        var sz = buffers[0].capacity()
        if (sz <= 0) {
            sz = audioInputBufferSize
        }
        var mAudioOutTempBuf = ByteArray(sz)

        val audioBufferInfo = MediaCodec.BufferInfo()
        var playEnd = false
        var eos = false
        val startMillis = System.currentTimeMillis()
        var outputBuffers = audioDecoder.getOutputBuffers()
        while (!playEnd) {
            // 解码
            if (!eos) {
                eos = decodeFrame(audioDecoder, audioExtractor)
            }
            // 获取解码后的数据
            val outputBufferIndex = audioDecoder.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_US) // 这里不能用-1，有些视频是需要积累几帧才能解出数据的，妈蛋
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
                MediaCodec.INFO_TRY_AGAIN_LATER -> { }
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    outputBuffers = audioDecoder.getOutputBuffers()
                    Log.i(TAG, "INFO_OUTPUT_BUFFERS_CHANGED")
                }
                else -> {
                    if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        playEnd = true
                    } else {
                        outputBuffers[outputBufferIndex]?.let { outputBuffer ->
                            // 延时解码，跟视频时间同步
                            delay(audioBufferInfo, startMillis)
                            // 如果解码成功，则将解码后的音频PCM数据用AudioTrack播放出来
                            if (audioBufferInfo.size > 0) {
                                if (mAudioOutTempBuf.size < audioBufferInfo.size) {
                                    mAudioOutTempBuf = ByteArray(audioBufferInfo.size)
                                }
                                outputBuffer.position(0)
                                outputBuffer.get(mAudioOutTempBuf, 0, audioBufferInfo.size)
                                outputBuffer.clear()
                                audioTrack.write(mAudioOutTempBuf, 0, audioBufferInfo.size)
                            }
                        }
                        // 释放资源
                        audioDecoder.releaseOutputBuffer(outputBufferIndex, false)
                    }
                }
            }

            // 结尾了
            if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                break
            }
        }

        // 释放MediaCode 和AudioTrack
        audioDecoder.stop()
        audioDecoder.release()
        audioExtractor.release()
        audioTrack.stop()
        audioTrack.release()
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
            val outputIndex = videoDecoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
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