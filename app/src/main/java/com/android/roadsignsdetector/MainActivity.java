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


import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;

import org.opencv.android.OpenCVLoader;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private final String yoloV5ModelFileName = "models/model_640x480_s_10_1000_2022-11-14_11-42-13-int8.tflite";
    private final String classificationModelFileName = "models/model_53_0.99831.hdf5_01_0.99893.hdf5_quantized.tflite";

    static {
        if (!OpenCVLoader.initDebug()) {
            System.exit(1);
        }
    }

    YoloV5Detector mYoloV5Detector;
    SoundManager mSoundManager;
    PresenceManager mPresenceManager;
    SignClassifier mSignClassifier;


    void resetStatusAndNavigationBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat windowInsetsCompat = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        windowInsetsCompat.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
        windowInsetsCompat.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // For camera
        CameraView cameraView = findViewById(R.id.camera);
        cameraView.setLifecycleOwner(this);

        // Choose dimension
        cameraView.setPreviewStreamSize(new SizeSelector() {
            @Override
            public List<Size> select(List<Size> source) {
                if (source.stream().filter(size -> size.getWidth() > size.getHeight()).collect(Collectors.counting()) >
                    source.stream().filter(size -> size.getWidth() <= size.getHeight()).collect(Collectors.counting())) {
                    return Arrays.asList(new Size(1440, 1080));
                } else {
                    return Arrays.asList(new Size(1080, 1440));
                }
            }
        });

        // Make active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Remove status and navigation bar
        resetStatusAndNavigationBars();

        // Create detector
        mYoloV5Detector = YoloV5Detector.create(getAssets(), yoloV5ModelFileName);

        // Create sign classifier
        mSignClassifier = SignClassifier.create(getAssets(), classificationModelFileName);

        // Create sound manager
        mSoundManager = new SoundManager(getAssets(), mSignClassifier.getNClasses());

        // Create presence manager
        mPresenceManager = new PresenceManager(mSoundManager, mSignClassifier.getNClasses());

        // To watch visibility changes
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        resetStatusAndNavigationBars();
                    }
                });

        // Adding Frame processor
        ImageView watermark = (ImageView) findViewById(R.id.watermark);
        cameraView.addFrameProcessor(new CameraFrameProcessor(this, watermark, mYoloV5Detector, mSignClassifier, mPresenceManager));
    }

}