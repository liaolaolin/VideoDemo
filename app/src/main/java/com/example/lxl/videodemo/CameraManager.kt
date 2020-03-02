package com.example.lxl.videodemo

import android.hardware.Camera
import android.util.Log

class CameraManager {

    private val TAG = "CameraManager"

    private val backCameras = mutableListOf<Int>()
    private val frontCameras = mutableListOf<Int>()

    init {

    }

    fun listCameraInfo() {
        backCameras.clear()
        frontCameras.clear()
        for (i in 0 until Camera.getNumberOfCameras()) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(i, cameraInfo)

            //后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.i(TAG, "cameraInfo $i, 后置摄像头, orientation:${cameraInfo.orientation}, 可关闭快门声:${cameraInfo.canDisableShutterSound}")
                backCameras.add(i)
            }
            //前置摄像头
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.i(TAG, "cameraInfo $i, 前置摄像头, orientation:${cameraInfo.orientation}, 可关闭快门声:${cameraInfo.canDisableShutterSound}")
                frontCameras.add(i)
            }
        }
    }

    fun getBackCameraId(): Int {
        return backCameras.firstOrNull()?:-1
    }

    fun getFrontCameraId(): Int {
        return frontCameras.firstOrNull()?:-1
    }

    fun getBackCamera(): Camera? {
        val id = getBackCameraId()
        if (id >= 0) {
            return Camera.open(id)
        } else {
            return null
        }
    }

    fun getFrontCamera(): Camera? {
        val id = getFrontCameraId()
        if (id >= 0) {
            return Camera.open(id)
        } else {
            return null
        }
    }

    fun closeCamera(camera: Camera) {
        camera.release()
    }
}