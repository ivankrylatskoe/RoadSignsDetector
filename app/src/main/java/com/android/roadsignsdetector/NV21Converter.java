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

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import androidx.annotation.Nullable;

public class NV21Converter {
    private final Allocation in, out;
    private final ScriptIntrinsicYuvToRGB script;

    public NV21Converter(Context context, int width, int height) {
        RenderScript rs = RenderScript.create(context);
        this.script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        // NV21 YUV image of dimension 4 X 4 has following packing:
        // YYYYYYYYYYYYYYYYVUVUVUVU
        // With each pixel (of any channel) taking 8 bits.
        int yuvByteArrayLength = (int) (width * height * 1.5f);
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(yuvByteArrayLength);
        this.in = Allocation.createTyped(
                rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(width)
                .setY(height);
        this.out = Allocation.createTyped(
                rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
    }

    @Nullable
    public Bitmap toBitmap(int width, int height, byte[] nv21ByteArray) {
        if (nv21ByteArray == null) {
            return null;
        }
        in.copyFrom(nv21ByteArray);
        script.setInput(in);
        script.forEach(out);

        // Allocate memory for the bitmap to return. If you have a reusable Bitmap
        // I recommending using that.
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bitmap);
        return bitmap;
    }
}