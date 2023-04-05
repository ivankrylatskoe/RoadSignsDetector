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

import androidx.core.util.Pair;

import java.util.Arrays;
import java.util.List;

public class Config {
    public static final int MIN_OBJECT_SIZE = 15;
    public static final float DEACTIVATION_WINDOW_RATIO = 0.7f;
    public static final int MIN_QUEUE_ELEMENTS = 4; // Minimum number of queue elements guaranteed to be taken into account
    public static final int MIN_QUEUE_MILLIS = 150;
    public static final float MIN_ACTIVATION_RATIO = 0.7f;
    public static final int MAX_OBJECT_SIZE_TAKE_ORIG = 20;
    public static final float IOU_THRES = 0.5f;
    public static final float CONF_THRES = 0.5f;

    public static final List<Pair<Integer, Integer>> NN_CLASSES_REMAP = Arrays.asList(
            new Pair<>(13,10)
    );
}
