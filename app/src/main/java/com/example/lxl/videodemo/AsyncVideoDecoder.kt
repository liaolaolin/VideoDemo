package com.example.lxl.videodemo

import android.media.*
import android.view.Surface
import java.io.IOException

class AsyncVideoDecoder(val surface: Surface, val path: String) {

    val TAG = "AsyncVideoDecoder"

    lateinit var videoExtractor: MediaExtractor
    lateinit var videoDecoder: MediaCodec
    lateinit var audioExtractor: MediaExtractor
    lateinit var audioDecoder: MediaCodec
    lateinit var audioTrack: AudioTrack
    private var audioInputBufferSize = 0

    private var videoPlayEnd = false
    private var videoEos = false
    private var audioPlayEnd = false
    private var audioEos = false

    private var startMillis = System.currentTimeMillis()

    private val videoCodecCallback = object: MediaCodec.Callback() {
        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            outputIndex: Int,
            bufferInfo: MediaCodec.BufferInfo
        ) {
            when (outputIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED,
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                else -> {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        videoPlayEnd = true
                    } else {
                        DeocderUtil.delay(bufferInfo, startMillis)
                        videoDecoder.releaseOutputBuffer(outputIndex, true)
                    }
                }
            }

            if (videoPlayEnd) {
                videoDecoder.stop()
                videoDecoder.release()
                videoExtractor.release()
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, inputIndex: Int) {
            if (!videoEos) {
                videoEos = DeocderUtil.decodeFrame(codec, videoExtractor, inputIndex)
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        }
    }

    private val audioCodecCallback = object: MediaCodec.Callback() {
        var mAudioOutTempBuf = ByteArray(1)

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            outputBufferIndex: Int,
            audioBufferInfo: MediaCodec.BufferInfo
        ) {
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
                MediaCodec.INFO_TRY_AGAIN_LATER,
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                }
                else -> {
                    if (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        audioPlayEnd = true
                    } else {
                        audioDecoder.getOutputBuffer(outputBufferIndex)?.let { outputBuffer ->
                            // 延时解码，跟视频时间同步
                            DeocderUtil.delay(audioBufferInfo, startMillis)
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
            if (audioPlayEnd) {
                audioDecoder.stop()
                audioDecoder.release()
                audioExtractor.release()
                audioTrack.stop()
                audioTrack.release()
            }
        }

        override fun onInputBufferAvailable(codec: MediaCodec, inputIndex: Int) {
            if (!audioEos) {
                audioEos = DeocderUtil.decodeFrame(codec, audioExtractor, inputIndex)
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        }

    }

    init {
        initVideo(videoCodecCallback)
        initAudio(audioCodecCallback)
    }

    fun start() {
        startMillis = System.currentTimeMillis()
        videoDecoder.start()
        audioDecoder.start()
    }

    private fun initVideo(callback: MediaCodec.Callback?) {
        try {
            videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(path)
            for (i in 0..(videoExtractor.trackCount - 1)) {
                val mediaFormat = videoExtractor.getTrackFormat(i)
                val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    videoExtractor.selectTrack(i)
                    videoDecoder = MediaCodec.createDecoderByType(mime)
                    if (callback != null) {
                        videoDecoder.setCallback(callback)
                    }

                    videoDecoder.configure(mediaFormat, surface, null, 0)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun initAudio(callback: MediaCodec.Callback?) {
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
                        if (callback != null) {
                            audioDecoder.setCallback(callback)
                        }
                        audioDecoder.configure(mediaFormat, null, null, 0)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    break
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

}