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

import org.opencv.core.CvType;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Pair;


public class SignClassifier {
    private static final int numThreads = 4;
    private static final boolean isGPU = false;
    private static final boolean useXNNPack = true;

    private static final int inputWidth = 32;
    private static final int inputHeight = 32;

    private boolean initialized = false;

    private GpuDelegate gpuDelegate = null;

    final private int sourceByteBufferWidth = 640;

    private ByteBuffer imgData;
    private ByteBuffer outData;
    private MappedByteBuffer tfliteModel;
    private Interpreter tfLite;
    private Map<Integer, Object> outputMap = new HashMap<>();
    private Object[] inputArray;

    private int nClasses;

    private SignClassifier() {
    }

    private static class SignClassifierHolder {
        private final static SignClassifier instance = new SignClassifier();
    }

    public static SignClassifier create(
            final AssetManager assetManager,
            final String modelFilename)
    {
        SignClassifier instance = SignClassifier.SignClassifierHolder.instance;
        if (instance.initialized)
            return instance;

        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(numThreads);
            options.setUseXNNPACK(useXNNPack);
            if (isGPU) {
                GpuDelegate.Options gpuOptions = new GpuDelegate.Options();
                gpuOptions.setPrecisionLossAllowed(true);
                gpuOptions.setInferencePreference(GpuDelegate.Options.INFERENCE_PREFERENCE_SUSTAINED_SPEED);
                instance.gpuDelegate = new GpuDelegate(gpuOptions);
                options.addDelegate(instance.gpuDelegate);
            }
            instance.tfliteModel = loadModelFile(assetManager, modelFilename);
            instance.tfLite = new Interpreter(instance.tfliteModel, options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int numBytesPerChannel = 1;
        instance.imgData = ByteBuffer.allocateDirect( inputWidth * inputHeight * 3 * numBytesPerChannel);
        instance.imgData.order(ByteOrder.nativeOrder());

        int[] shape = instance.tfLite.getOutputTensor(0).shape();

        instance.nClasses = shape[1];
        instance.outData = ByteBuffer.allocateDirect(instance.nClasses * numBytesPerChannel);
        instance.outData.order(ByteOrder.nativeOrder());

        instance.outputMap.put(0, instance.outData);
        instance.inputArray = new Object[1];
        instance.inputArray[0] = instance.imgData;

        instance.initialized = true;
        return instance;
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public void close() {
        tfLite.close();
        tfLite = null;
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        tfliteModel = null;
    }


    synchronized public int getClassId(Recognition rec, Bitmap bitmap, ByteBuffer byteBuffer) throws Exception {
        int width = rec.right - rec.left;
        int height = rec.bottom - rec.top;
        int fragSize = Math.min(width, height);
        if (fragSize <= Config.MAX_OBJECT_SIZE_TAKE_ORIG) {
            // Coords rescale
            int newX = (rec.left * 9 + 2) / 4;
            int newY = (rec.top * 9 + 2) / 4;
            int newX2 = (rec.right * 9 - 7) / 4 + 1;
            int newY2 = (rec.bottom * 9 - 7) / 4 + 1;
            int newWidth = newX2 - newX;
            int newHeight = newY2 - newY;

            byte[] buf = new byte[newWidth * newHeight * 3];
            JniNativeOpsLib.cropBitmapToByteArray(bitmap, newX, newY, newWidth, newHeight, buf);
            Mat frag = new Mat(newHeight, newWidth, CvType.CV_8UC3);
            frag.put(0, 0, buf);

            Mat resizedFragment = new Mat(inputHeight, inputWidth, CvType.CV_8UC3);
            org.opencv.core.Size sz = new org.opencv.core.Size( inputWidth, inputHeight);
            Imgproc.resize(frag, resizedFragment, sz, 0, 0, Imgproc.INTER_CUBIC);

            JniNativeOpsLib.matToByteBuffer(resizedFragment, imgData);
        } else {
            byte[] buf = new byte[width * height * 3];
            JniNativeOpsLib.cropByteBufferToByteArray(byteBuffer, sourceByteBufferWidth, rec.left, rec.top, width, height, buf);
            Mat frag = new Mat(height, width, CvType.CV_8UC3);
            frag.put(0, 0, buf);

            if (width == inputWidth && height == inputHeight) {
                JniNativeOpsLib.matToByteBuffer(frag, imgData);
            } else {
                Mat resizedFragment = new Mat(inputHeight, inputWidth, CvType.CV_8UC3);
                org.opencv.core.Size sz = new org.opencv.core.Size(inputWidth, inputHeight);
                Imgproc.resize(frag, resizedFragment, sz, 0, 0, (width <= inputWidth) || (height <= inputHeight) ? Imgproc.INTER_CUBIC : Imgproc.INTER_AREA);

                JniNativeOpsLib.matToByteBuffer(resizedFragment, imgData);
            }
        }

        outData.rewind();

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        int maxIdx = outData.limit() - 1;
        int maxVal = 0;
        for(int i = 0; i < outData.limit(); i++) {
            int val = outData.get(i);
            if (val < 0)
                val += 256;
            if (val > maxVal) {
                maxVal = val;
                maxIdx = i;
            }
        }

        // Remap class
        for (Pair<Integer, Integer> remapEntry : Config.NN_CLASSES_REMAP) {
            if (maxIdx == remapEntry.first)
                return remapEntry.second;
        }
        return maxIdx;
    }

    public int getNClasses() {
        return nClasses;
    }

}

