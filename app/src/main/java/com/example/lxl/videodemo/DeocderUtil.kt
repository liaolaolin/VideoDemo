package com.example.lxl.videodemo

import android.media.MediaCodec
import android.media.MediaExtractor

object DeocderUtil {

    fun decodeFrame(decoder: MediaCodec, extractor: MediaExtractor, inputIndex: Int): Boolean {
        var eos = false
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

    fun delay(bufferInfo: MediaCodec.BufferInfo, startMillis: Long) {
        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMillis) {
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                e.printStackTrace()
                break
            }
        }
    }
}