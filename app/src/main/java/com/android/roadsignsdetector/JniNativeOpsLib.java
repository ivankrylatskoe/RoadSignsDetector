// 
// Copyright 2022-2023 Ivan Bychkov
// Email: ivankrylatskoe@gmail.com
//
// Licensed under a Creative Commons Attribution-NonCommercial 4.0 
// International License (the "License").
// You may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://creativecommons.org/licenses/by-nc/4.0/
//
// Unless otherwise separately undertaken by the Licensor, to the extent 
// possible, the Licensor offers the Licensed Material as-is and as-available,
// and makes no representations or warranties of any kind concerning the 
// Licensed Material, whether express, implied, statutory, or other. 
//

package com.android.roadsignsdetector;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.nio.ByteBuffer;

public class JniNativeOpsLib {
    static {
        System.loadLibrary("native-lib");
    }

    public static void rotateBitmap180(Bitmap bitmap) throws Exception {
        if (!jniRotateBitmap180(bitmap)) {
            throw new Exception("Failed to rotate bitmap");
        }
    }

    public static void resizeBitmapToByteBuffer225(Bitmap bitmap, ByteBuffer byteBuffer) throws Exception {
        if (!jniResizeBitmapToByteBuffer225(bitmap, byteBuffer)) {
            throw new Exception("Failed to convert bitmap to float buffer");
        }
    }

    public static void cropBitmapToByteArray(Bitmap bitmap, int x, int y, int width, int height, byte[] byteArray) throws Exception {
        if (!jniCropBitmapToByteArray(bitmap, x, y, width, height, byteArray)) {
            throw new Exception("Failed to crop bitmap");
        }
    }

    public static void cropByteBufferToByteArray(ByteBuffer byteBuffer, int imgWidth, int x, int y, int width, int height, byte[] byteArray) throws Exception {
        jniCropByteBufferToByteArray(byteBuffer, imgWidth, x, y, width, height, byteArray);
    }

    public static void matToByteBuffer(Mat mat, ByteBuffer byteBuffer)  {
        jniMatToByteBuffer(mat.getNativeObjAddr(), byteBuffer);
    }

    public static void initArraysForDetection(float oupScale, float oupZeroPoint, float objThres) {
        jniInitArraysForDetection( oupScale, oupZeroPoint, objThres);
    }

    public static Recognition[] byteBufferToFloatDetections(int width, int height, ByteBuffer byteBuffer, int outputBoxCount, int numClass, float oupScale, float oupZeroPoint, float iouThres) {
        return jniByteBufferToFloatDetections( width,  height,  byteBuffer,  outputBoxCount,  numClass,  oupScale, oupZeroPoint, iouThres);
    }

    ///////////////////////////////

    private native static boolean jniRotateBitmap180(Bitmap bitmap);
    private native static boolean jniResizeBitmapToByteBuffer225(Bitmap bitmap, ByteBuffer byteBuffer);
    private native static void jniInitArraysForDetection(float oupScale, float oupZeroPoint, float objThres);
    private native static Recognition[] jniByteBufferToFloatDetections(int width, int height, ByteBuffer byteBuffer, int outputBoxCount, int numClass, float oup_scale, float oup_zero_point, float iou_thres);
    private native static boolean jniCropBitmapToByteArray(Bitmap bitmap, int x, int y, int width, int height, byte[] byteArray);
    private native static void jniCropByteBufferToByteArray(ByteBuffer byteBuffer, int imgWidth, int x, int y, int width, int height, byte[] byteArray);
    private native static void jniMatToByteBuffer(long matAddr, ByteBuffer byteBuffer);

}
