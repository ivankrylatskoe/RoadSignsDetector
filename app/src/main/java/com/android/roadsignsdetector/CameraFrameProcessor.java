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

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Environment;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.size.Size;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CameraFrameProcessor implements FrameProcessor {
    long lastFrameTime = 0;
    long lastTime = 0;
    int lastNRecognitions = 0;
    ImageView mWatermark = null;
    NV21Converter nv21Converter = null;
    final String APP_TAG = "sign_detector";
    File mMediaStorageDir;
    YoloV5Detector mDetector;
    SignClassifier mSignClassifier;
    PresenceManager mPresenceManager;
    MainActivity mMainActivity;
    Bitmap mWatermarkBitmap = null;
    final int mWatermarkWidth = 640;
    final int mWatermarkHeight = 480;
    final String rotateText1 = "Поверните устройство";
    final String rotateText2 = "на 90 градусов";


    public CameraFrameProcessor(MainActivity mainActivity, ImageView watermark, YoloV5Detector detector, SignClassifier signClassifier, PresenceManager presenseManager) {
        mMainActivity = mainActivity;
        mWatermark = watermark;
        mDetector = detector;
        mSignClassifier = signClassifier;
        mPresenceManager = presenseManager;

        mMediaStorageDir = new File(mWatermark.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG);
        if (!mMediaStorageDir.exists()) {
            mMediaStorageDir.mkdirs();
        }
    }

    @Override
    @WorkerThread
    public void process(@NonNull Frame frame) {

        Size size = frame.getSize();
        if (nv21Converter == null) {
            nv21Converter = new NV21Converter(mWatermark.getContext(), size.getWidth(), size.getHeight());
        }
        long frameTime = frame.getTime();
        long frameTimeDiff = frameTime - lastFrameTime;
        long time = System.currentTimeMillis();
        boolean wrongRotation = false;
        if (lastFrameTime == 0) {
            lastFrameTime = frameTime;
            return;
        }
        lastFrameTime = frameTime;
        lastTime = time;

        lastNRecognitions = 0;

        int viewRotation = frame.getRotationToView();
        int width;
        int height;
        if (viewRotation == 0 || viewRotation == 180) {
            width = mWatermarkWidth;
            height = mWatermarkHeight;
        } else {
            width = mWatermarkHeight ;
            height =  mWatermarkWidth;
            wrongRotation = true;
        }

        Bitmap newWatermarkBitmap = Bitmap.createBitmap(width, height, ARGB_8888, true);
        newWatermarkBitmap.eraseColor(0);

        Canvas canvas = new Canvas(newWatermarkBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setTextSize(15);

        if (wrongRotation) {
            paint.setTextSize(30);
            paint.setColor(Color.RED);
            float textWidth = paint.measureText(rotateText1);
            canvas.drawText(rotateText1, (width-textWidth)/2, height/2 - 20, paint);
            textWidth = paint.measureText(rotateText2);
            canvas.drawText(rotateText2, (width-textWidth)/2, height/2 + 20, paint);

            mMainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (this)
                    {
                        mWatermark.setImageBitmap(newWatermarkBitmap);
                        if (mWatermarkBitmap != null)
                            mWatermarkBitmap.recycle();
                        mWatermarkBitmap = newWatermarkBitmap;
                    }
                }
            });
            return;
        }

        int format = frame.getFormat();
        int userRotation = frame.getRotationToUser();
        if (frame.getDataClass() == byte[].class) {
            byte[] data = frame.getData();
            // Image processing stage 1
            Bitmap previewBitmap = nv21Converter.toBitmap(size.getWidth(), size.getHeight(), data);

            if (frame.getRotationToView() == 180) {
                try {
                    // Image processing stage 2
                    JniNativeOpsLib.rotateBitmap180(previewBitmap);
                }
                catch (Exception e) {
                    return;
                }
            }

            try{
                // Image processing stage 3
                JniNativeOpsLib.resizeBitmapToByteBuffer225(previewBitmap, mDetector.getImgData());

                Recognition[] rawRecognitions = mDetector.recognizeImage();

                // Make recognitions include right and bottom border
                for (Recognition r: rawRecognitions) {
                    r.right = Math.min(r.right + 1, mWatermarkWidth - 1);
                    r.bottom = Math.min(r.bottom + 1, mWatermarkHeight - 1);
                }

                // Filter recognitions
                List<Recognition> filteredRecognitions = Arrays.stream(rawRecognitions).filter(rec -> Math.min(rec.right-rec.left, rec.bottom-rec.top) >= Config.MIN_OBJECT_SIZE).collect(Collectors.toList());

                lastNRecognitions = filteredRecognitions.size();

                for (Recognition r: filteredRecognitions) {
                    r.classId = mSignClassifier.getClassId(r, previewBitmap, mDetector.getImgData());
                }

                // Remove "other" class
                filteredRecognitions.removeIf(r -> r.classId == mSignClassifier.getNClasses() - 1);

                // Paint detections
                paint.setStyle(Paint.Style.STROKE);
                for (Recognition r: filteredRecognitions) {
                    canvas.drawRect(new Rect(r.left, r.top, r.right, r.bottom), paint);
                }
                mPresenceManager.SaveRecognitions(filteredRecognitions);

                mMainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (this)
                        {
                            mWatermark.setImageBitmap(newWatermarkBitmap);
                            if (mWatermarkBitmap != null)
                                mWatermarkBitmap.recycle();
                            mWatermarkBitmap = newWatermarkBitmap;
                        }
                    }
                });

            }
            catch (Exception e) {
                return;
            }
        }
    }

}
