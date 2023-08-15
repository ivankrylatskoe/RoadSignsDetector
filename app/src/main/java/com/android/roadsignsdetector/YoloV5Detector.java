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

import static java.lang.Math.round;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;


public class YoloV5Detector  {
    private static final int numThreads = 4;
    private static final boolean isGPU = false;
    private static final boolean useXNNPack = true;
    private static final int inputWidth = 640;
    private static final int inputHeight = 480;

    private GpuDelegate gpuDelegate = null;
    private MappedByteBuffer tfliteModel;
    private Interpreter tfLite;
    private ByteBuffer imgData;
    private ByteBuffer outData;
    private int outputBoxCount;
    private int numClass;
    private boolean initialized = false;

    private Map<Integer, Object> outputMap = new HashMap<>();
    private Object[] inputArray;

    private float oupScale;
    private int oupZeroPoint;

    private YoloV5Detector() {
    }

    private static class YoloV5DetectorHolder {
        private final static YoloV5Detector instance = new YoloV5Detector();
    }

    public static YoloV5Detector create(
            final AssetManager assetManager,
            final String modelFilename)
    {
        YoloV5Detector instance = YoloV5DetectorHolder.instance;
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
        instance.imgData = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * numBytesPerChannel);
        instance.imgData.order(ByteOrder.nativeOrder());

        instance.outputBoxCount = (int) ((inputWidth / 32) * (inputHeight/32) * 21 * 3);

        int[] shape = instance.tfLite.getOutputTensor(0).shape();
        instance.numClass = shape[shape.length - 1] - 5;

        instance.outData = ByteBuffer.allocateDirect(instance.outputBoxCount * (instance.numClass + 5) * numBytesPerChannel);
        instance.outData.order(ByteOrder.nativeOrder());

        instance.outputMap.put(0, instance.outData);
        instance.inputArray = new Object[1];
        instance.inputArray[0] = instance.imgData;

        Tensor oupten = instance.tfLite.getOutputTensor(0);
        instance.oupScale = oupten.quantizationParams().getScale();
        instance.oupZeroPoint = oupten.quantizationParams().getZeroPoint();

        JniNativeOpsLib.initArraysForDetection(instance.oupScale, instance.oupZeroPoint, Config.CONF_THRES);

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

    synchronized public Recognition[] recognizeImage()  {
        outData.rewind();
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Recognition[] detections = JniNativeOpsLib.byteBufferToFloatDetections(inputWidth, inputHeight, outData, outputBoxCount, numClass, oupScale, oupZeroPoint, Config.IOU_THRES);
        return detections;
    }

    public ByteBuffer getImgData(){
        return imgData;
    }
	
    public int getInputWidth() {
        return inputWidth;
    }

	public int getInputHeight() {
        return inputHeight;
    }

}
