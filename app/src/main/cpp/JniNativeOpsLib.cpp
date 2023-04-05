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


#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <math.h>
#include <list>
#include <vector>

#define  LOG_TAG    "JniNativeOpsLib"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniRotateBitmap180(
            JNIEnv *env, jclass cls, jobject bitmap);

    JNIEXPORT jboolean JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniResizeBitmapToByteBuffer225(
            JNIEnv *env, jclass cls, jobject bitmap, jobject byteBuffer);

    JNIEXPORT jboolean JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniCropBitmapToByteArray(
            JNIEnv *env, jclass cls, jobject bitmap, jint x, jint y, jint width, jint height, jbyteArray byteArray);

    JNIEXPORT void JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniCropByteBufferToByteArray(
            JNIEnv *env, jclass cls, jobject byteBuffer, jint imgWidth, jint x, jint y, jint width, jint height, jbyteArray byteArray);

    JNIEXPORT void JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniMatToByteBuffer(
            JNIEnv *env, jclass cls, jlong matAddr, jobject byteBuffer);

    JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved);

    JNIEXPORT void JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniInitArraysForDetection(JNIEnv *env, jclass cls, jfloat oup_scale, jfloat oup_zero_point, jfloat obj_thres);

    JNIEXPORT jobjectArray  JNICALL
    Java_com_android_roadsignsdetector_JniNativeOpsLib_jniByteBufferToFloatDetections(
        JNIEnv *env, jclass cls, jint width, jint height, jobject byteBuffer, jint outputBoxCount, jint numClass, jfloat oup_scale, jfloat oup_zero_point, jfloat iou_thres);
}

float gByteToFloat[256];

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    for (unsigned short i = 0; i <= 255; i++) {
        gByteToFloat[i] = i / 255.0;
    }
    return JNI_VERSION_1_2;
}

bool checkLockBitmapBits(JNIEnv *env, jobject& bitmap, AndroidBitmapInfo& bitmapInfo, void** ppBitmapPixels) {
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed, error=%d", ret);
        return false;
    }
    if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888");
        return false;
    }
    if (bitmapInfo.stride != bitmapInfo.width * sizeof(uint32_t)) {
        LOGE("Bitmap stride <> width * 4");
        return false;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, ppBitmapPixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed, error=%d", ret);
        return false;
    }
    return true;
}

bool unlockBitmapBits(JNIEnv *env, jobject& bitmap) {
    int ret;
    if ((ret = AndroidBitmap_unlockPixels(env, bitmap)) < 0) {
        LOGE("AndroidBitmap_unlockPixels() failed, error=%d", ret);
        return false;
    }
    return true;
}


JNIEXPORT jboolean JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniRotateBitmap180(
        JNIEnv *env, jclass cls, jobject bitmap)
{
    AndroidBitmapInfo bitmapInfo;
    void* pBitmapPixels;
    if (!checkLockBitmapBits(env, bitmap, bitmapInfo, &pBitmapPixels)) {
        return false;
    }

    uint32_t* src = (uint32_t*) pBitmapPixels;
    uint32_t* dst = src + bitmapInfo.width * bitmapInfo.height - 1;
    while(dst > src) {
        uint32_t tmp = *src;
        *src = *dst;
        *dst = tmp;
        dst--;
        src++;
    }
    return unlockBitmapBits(env, bitmap);
}


JNIEXPORT jboolean JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniCropBitmapToByteArray(
        JNIEnv *env, jclass cls, jobject bitmap, jint x, jint y, jint width, jint height, jbyteArray byteArray)
{
    AndroidBitmapInfo bitmapInfo;
    void * pBitmapPixels;
    if (!checkLockBitmapBits(env, bitmap, bitmapInfo, &pBitmapPixels)) {
        return false;
    }

    int rowSizeInBytes = bitmapInfo.width * 4;
    uint8_t* srcBuf = ((uint8_t*) pBitmapPixels) + rowSizeInBytes * y + x * 4;

    jbyte* dstBuf = env->GetByteArrayElements(byteArray, NULL);
    uint8_t* curDstBuf = (uint8_t*)dstBuf;

    for(int j = 0;  j < height; j++) {
        uint8_t* curBuf = srcBuf;
        for (int i = 0; i < width; i++) {
            *((uint16_t*)curDstBuf) = *((uint16_t*)curBuf);
            ++++curBuf;
            ++++curDstBuf;
            *curDstBuf = *curBuf;
            ++++curBuf;
            curDstBuf++;
        }
        srcBuf += rowSizeInBytes;
    }

    env->ReleaseByteArrayElements(byteArray, dstBuf, 0);
    return unlockBitmapBits(env, bitmap);
}


JNIEXPORT void JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniCropByteBufferToByteArray(
        JNIEnv *env, jclass cls, jobject byteBuffer, jint imgWidth, jint x, jint y, jint width, jint height, jbyteArray byteArray)
{
    int rowSizeInBytes = imgWidth * 3;
    int cropLineBytes = width * 3;
    uint8_t* srcBuf = ((uint8_t*) env->GetDirectBufferAddress(byteBuffer)) + rowSizeInBytes * y + x * 3;

    jbyte* dstBuf = env->GetByteArrayElements(byteArray, NULL);
    uint8_t* curDstBuf = (uint8_t*)dstBuf;

    for(int j = 0;  j < height; j++) {
        memcpy(curDstBuf, srcBuf, cropLineBytes);
        curDstBuf += cropLineBytes;
        srcBuf += rowSizeInBytes;
    }

    env->ReleaseByteArrayElements(byteArray, dstBuf, 0);
}

#define RESIZE_225_CELL(d, di,s1,s2,s3,si,sh11,sh12,sh13,sh21,sh22,sh23,sh31,sh32,sh33) \
    d[di]   = ((((unsigned short) s1[si]) sh11) + (((unsigned short) s1[si+4]) sh12) + (((unsigned short) s1[si+8]) sh13) + \
               (((unsigned short) s2[si]) sh21) + (((unsigned short) s2[si+4]) sh22) + (((unsigned short) s2[si+8]) sh23) + \
               (((unsigned short) s3[si]) sh31) + (((unsigned short) s3[si+4]) sh32) + (((unsigned short) s3[si+8]) sh33) + \
               81) / 162; \
    d[di+1] = ((((unsigned short) s1[si+1]) sh11) + (((unsigned short) s1[si+5]) sh12) + (((unsigned short) s1[si+9]) sh13) + \
               (((unsigned short) s2[si+1]) sh21) + (((unsigned short) s2[si+5]) sh22) + (((unsigned short) s2[si+9]) sh23) + \
               (((unsigned short) s3[si+1]) sh31) + (((unsigned short) s3[si+5]) sh32) + (((unsigned short) s3[si+9]) sh33) + \
               81) / 162; \
    d[di+2] = ((((unsigned short) s1[si+2]) sh11) + (((unsigned short) s1[si+6]) sh12) + (((unsigned short) s1[si+10]) sh13) + \
               (((unsigned short) s2[si+2]) sh21) + (((unsigned short) s2[si+6]) sh22) + (((unsigned short) s2[si+10]) sh23) + \
               (((unsigned short) s3[si+2]) sh31) + (((unsigned short) s3[si+6]) sh32) + (((unsigned short) s3[si+10]) sh33) + \
               81) / 162;



JNIEXPORT jboolean JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniResizeBitmapToByteBuffer225(
        JNIEnv *env, jclass cls, jobject bitmap, jobject byteBuffer)
{
    AndroidBitmapInfo bitmapInfo;
    void * pBitmapPixels;
    if (!checkLockBitmapBits(env, bitmap, bitmapInfo, &pBitmapPixels)) {
        return false;
    }
    int dstWidth = bitmapInfo.width / 9 * 4;
    int dstHeight = bitmapInfo.height / 9 * 4;

    int srcLineStride = bitmapInfo.width * sizeof(uint32_t);
    int dstLineStride = dstWidth * 3;
    int srcLineStride9 = srcLineStride * 9;
    int dstLineStride4 = dstLineStride * 4;

    uint8_t* srcLine = (uint8_t*) pBitmapPixels;
    uint8_t* src = srcLine;
    uint8_t* srcNextLine = srcLine + srcLineStride;
    uint8_t* srcEnd = srcLine + srcLineStride*bitmapInfo.height;
    uint8_t* dstLine = (uint8_t*) env->GetDirectBufferAddress(byteBuffer);
    uint8_t* dst = dstLine;
    while(srcLine < srcEnd) {
        while (src < srcNextLine) {
            {
                uint8_t* srcLine2 = src + srcLineStride;
                uint8_t* srcLine3 = srcLine2 + srcLineStride;
                uint8_t* srcLine4 = srcLine3 + srcLineStride;
                uint8_t* srcLine5 = srcLine4 + srcLineStride;
                uint8_t* srcLine6 = srcLine5 + srcLineStride;
                uint8_t* srcLine7 = srcLine6 + srcLineStride;
                uint8_t* srcLine8 = srcLine7 + srcLineStride;
                uint8_t* srcLine9 = srcLine8 + srcLineStride;

                uint8_t* dstLine2 = dst + dstLineStride;
                uint8_t* dstLine3 = dstLine2 + dstLineStride;
                uint8_t* dstLine4 = dstLine3 + dstLineStride;

                RESIZE_225_CELL(dst,      0, src, srcLine2, srcLine3,     0, << 5, << 5, << 3, << 5, << 5, << 3, << 3, << 3, << 1 )
                RESIZE_225_CELL(dst,      3, src, srcLine2, srcLine3,     8, * 24, << 5, << 4, * 24, << 5, << 4, * 6, << 3, << 2)
                RESIZE_225_CELL(dst,      6, src, srcLine2, srcLine3,     16, << 4, << 5, * 24, << 4, << 5, * 24, << 2, << 3, * 6)
                RESIZE_225_CELL(dst,      9, src, srcLine2, srcLine3,     24, << 3, << 5, << 5, << 3, << 5, << 5, << 1 , << 3, <<3)

                RESIZE_225_CELL(dstLine2, 0, srcLine3, srcLine4, srcLine5, 0, * 24, * 24, * 6, << 5, << 5, << 3, << 4, << 4, << 2)
                RESIZE_225_CELL(dstLine2, 3, srcLine3, srcLine4, srcLine5, 8, * 18, * 24, * 12, * 24, << 5, << 4, * 12, << 4, << 3)
                RESIZE_225_CELL(dstLine2, 6, srcLine3, srcLine4, srcLine5,16, * 12, * 24, * 18, << 4, << 5, * 24, << 3, << 4, * 12)
                RESIZE_225_CELL(dstLine2, 9, srcLine3, srcLine4, srcLine5,24, * 6, * 24, * 24, << 3, << 5, << 5, << 2, << 4, << 4)

                RESIZE_225_CELL(dstLine3, 0, srcLine5, srcLine6, srcLine7, 0, << 4, << 4, << 2, << 5, << 5, << 3, * 24, * 24, * 6)
                RESIZE_225_CELL(dstLine3, 3, srcLine5, srcLine6, srcLine7, 8, * 12,  << 4, << 3, * 24, << 5, << 4, * 18, * 24, * 12)
                RESIZE_225_CELL(dstLine3, 6, srcLine5, srcLine6, srcLine7,16, << 3, << 4, * 12, << 4, << 5, * 24, * 12, * 24, * 18)
                RESIZE_225_CELL(dstLine3, 9, srcLine5, srcLine6, srcLine7,24, << 2, << 4, << 4, << 3, << 5, << 5, * 6, * 24, * 24)

                RESIZE_225_CELL(dstLine4, 0, srcLine7, srcLine8, srcLine9, 0, << 3, << 3, << 1 , << 5, << 5, << 3, << 5, << 5, << 3)
                RESIZE_225_CELL(dstLine4, 3, srcLine7, srcLine8, srcLine9, 8, * 6,  << 3, << 2, * 24, << 5, << 4, * 24, << 5, << 4)
                RESIZE_225_CELL(dstLine4, 6, srcLine7, srcLine8, srcLine9,16, << 2, << 3, * 6, << 4, << 5, * 24, << 4, << 5, * 24)
                RESIZE_225_CELL(dstLine4, 9, srcLine7, srcLine8, srcLine9,24,  << 1 , << 3, << 3, << 3, << 5, << 5, << 3, << 5, << 5)
            }
            src += 4 * 9;
            dst += 3 * 4;
        }
        srcLine += srcLineStride9;
        src = srcLine;
        srcNextLine = srcLine + srcLineStride;
        dstLine += dstLineStride4;
        dst = dstLine;
    }
    return unlockBitmapBits(env, bitmap);
}


// i-th element of array gBytesMulFirstMinThres equals to the minimum byte value j such that
// float representation of i and j (considering oup_scale and oup_zero_point) are at least obj_thres.
// If there is no such value than gBytesMulFirstMinThresCorrect[i] equals 0. Otherwise, it equals 1.
uint8_t gBytesMulFirstMinThres[256];
uint8_t gBytesMulFirstMinThresCorrect[256];

// i-th element of array gByteToFloatWithScaling contains float representation of byte with value equals to i
// (considering oup_scale and oup_zero_point).
float gByteToFloatWithScaling[256];

JNIEXPORT void JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniInitArraysForDetection(
        JNIEnv *env, jclass cls, jfloat oup_scale, jfloat oup_zero_point, jfloat obj_thres) {
    for (int i = 0; i < 256; i++) {
        if ( i - oup_zero_point < 1E-6) {
            gBytesMulFirstMinThresCorrect[i] = 0;
        } else {
            int val = (int)ceil(obj_thres / (oup_scale * oup_scale) / (i - oup_zero_point) + oup_zero_point);
            if (val >= 256) {
                gBytesMulFirstMinThresCorrect[i] = 0;
            } else {
                gBytesMulFirstMinThres[i] = val;
                gBytesMulFirstMinThresCorrect[i] = 1;
            }
        }

        gByteToFloatWithScaling[i] = (i - oup_zero_point) * oup_scale;
    }
}

inline int max(int a, int b) {
    return (a > b) ? a : b;
}

inline int min(int a, int b) {
    return (a < b) ? a : b;
}

struct Recognition {
    float confidence;
    int classId;
    int left;
    int top;
    int right;
    int bottom;

    inline float iou(const Recognition& other) {
        float inter = max(min(this->right, other.right) - max(this->left, other.left), 0) *
                      max(min(this->bottom, other.bottom) - max(this->top, other.top), 0);

        float un = (this->right - this->left) * (this->bottom - this->top) +
                   (other.right - other.left) * (other.bottom - other.top) - inter + 1e-7;

        return inter / un;
    }
};


void nms(const std::list<Recognition> &rec_lst, std::list<Recognition> &lst_out, float iou_thres) {

    // Copy list to array to sort
    std::vector<Recognition> rec_arr;
    rec_arr.reserve(rec_lst.size());
    std::copy(rec_lst.begin(), rec_lst.end(), std::back_inserter(rec_arr));

    // Sort array
    sort(rec_arr.begin(), rec_arr.end(), [](Recognition& a, Recognition& b) {return a.confidence > b.confidence; });

    // Local variable showing if the next box must be taken to the result list
    bool take;

    // NMS algorithm main loop
    for(std::vector<Recognition>::iterator it = rec_arr.begin(); it != rec_arr.end(); it++) {
        take = true;
        for(std::list<Recognition>::iterator it_ret = lst_out.begin(); it_ret != lst_out.end(); it_ret++) {
            if (it_ret->classId != it->classId)
                continue;
            if (it_ret->iou(*it) < iou_thres)
                continue;
            take = false;
            break;
        }
        if (take)
            lst_out.push_back(*it);
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniByteBufferToFloatDetections(
        JNIEnv *env, jclass cls, jint width, jint height, jobject byteBuffer, jint outputBoxCount, jint numClass, jfloat oup_scale, jfloat oup_zero_point, jfloat iou_thres) {
    std::list<Recognition> rec_lst;

    uint8_t* srcBuf = (uint8_t*) env->GetDirectBufferAddress(byteBuffer);
    uint8_t* src = srcBuf;
    uint8_t stride = 5 + numClass;
    uint8_t* srcEnd = src + stride * outputBoxCount;
    while (src < srcEnd) {
        uint8_t globalConfUint = *(src+4);
        uint8_t detectedClass = 0;
        uint8_t detectedClassVal = *(src+5);
        for(uint8_t cl = 1; cl < numClass; cl++)
        {
            uint8_t v = *(src+5+cl);
            if (v > detectedClassVal) {
                detectedClassVal = v;
                detectedClass = cl;
            }
        }
        if (gBytesMulFirstMinThresCorrect[detectedClassVal] && (gBytesMulFirstMinThres[detectedClassVal] <= globalConfUint)) {
            Recognition r;
            r.classId = detectedClass;
            r.confidence = gByteToFloatWithScaling[detectedClassVal] * gByteToFloatWithScaling[globalConfUint];

            float x = gByteToFloatWithScaling[*src];
            float y = gByteToFloatWithScaling[*(src+1)];
            float w = gByteToFloatWithScaling[*(src+2)];
            float h = gByteToFloatWithScaling[*(src+3)];

            r.left = round((x - w/2) * width);
            r.right = round((x + w/2) * width);
            r.top = round((y - h/2) * height);
            r.bottom = round((y + h/2) * height);

            r.left = (r.left >= 0) ? r.left : 0;
            r.top = (r.top >= 0) ? r.top : 0;
            r.right = (r.right < width) ? r.right : (width - 1);
            r.bottom = (r.bottom < height) ? r.bottom : (height - 1);

            rec_lst.push_back(r);
        }
        src += stride;
    }

    std::list<Recognition> lst_out;
    nms(rec_lst, lst_out, iou_thres);

    jclass recognitionCls = env->FindClass("com/android/roadsignsdetector/Recognition");
    jmethodID recognitionConstructor = env->GetMethodID(recognitionCls, "<init>", "(FIIIII)V");

    jobjectArray ret = env->NewObjectArray(lst_out.size(), recognitionCls,0);
    int i = 0;
    for (std::list<Recognition>::iterator it = lst_out.begin(); it != lst_out.end() ; it++, i++) {
        jobject jobj = env->NewObject(recognitionCls, recognitionConstructor, it->confidence, it->classId, it->left, it->top, it->right, it->bottom);
        env->SetObjectArrayElement(ret, i, jobj);
    }

    return ret;
}

/////////////////////
// OpenCV part

#include <opencv2/core/mat.hpp>

JNIEXPORT void JNICALL
Java_com_android_roadsignsdetector_JniNativeOpsLib_jniMatToByteBuffer(
        JNIEnv *env, jclass cls, jlong matAddr, jobject byteBuffer) {
    cv::Mat* pInputImage = (cv::Mat*)matAddr;
    char* buf = (char*) env->GetDirectBufferAddress(byteBuffer);
    memcpy(buf, pInputImage->data, pInputImage->rows * pInputImage->cols * pInputImage->channels());
}
