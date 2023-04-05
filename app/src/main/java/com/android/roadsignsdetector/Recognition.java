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
public class Recognition {
    public float confidence;
    public int classId;
    public int left;
    public int top;
    public int right;
    public int bottom;

    public Recognition(float confidence, int classId, int left, int top, int right, int bottom) {
        this.confidence = confidence;
        this.classId = classId;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

}