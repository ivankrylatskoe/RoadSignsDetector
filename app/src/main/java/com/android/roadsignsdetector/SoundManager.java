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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class SoundManager {
    private SoundPool mSoundPool;
    private int[] mSoundIds;
    private int[] mSoundDurations;
    private static long mLastSoundStartTime = 0;
    private static final String soundsDir = "sounds/";

    private static int mLastSoundId = 0;
    private static final int pauseDuration = 300;
    private static final int timeoutAdditionalDuration = 10;
    private static Queue<Integer> mQueue;

    SoundManager(AssetManager assetManager, int nClasses) {
        mSoundPool = new SoundPool.Builder().setMaxStreams(1).build();
        mSoundIds = new int[nClasses];
        mSoundDurations = new int[nClasses];
        mQueue = new LinkedList<>();
        Arrays.fill(mSoundIds, -1);
        try {
            String[] filelist = assetManager.list("sounds");
            for (String f : filelist) {
                int id = Integer.parseInt(f.substring(0, f.length() - 4));
                mSoundIds[id] = mSoundPool.load(assetManager.openFd(soundsDir + f), 1);
                mSoundDurations[id] = getSoundDuration(assetManager, soundsDir + f);
            }
        }
        catch (Exception e) {
        }
    }

    int getSoundDuration(AssetManager assetManager, String filename) throws Exception {
        MediaPlayer mp = new MediaPlayer();
        AssetFileDescriptor d = assetManager.openFd(filename);
        mp.reset();
        mp.setDataSource(d.getFileDescriptor(), d.getStartOffset(), d.getLength());
        mp.prepare();
        int duration = mp.getDuration();
        mp.release();
        return duration;
    }

    void enqueueClassSound(int clazz) {
        synchronized (mQueue) {
            mQueue.add(clazz);
        }
        maybePlayNextSound();
    }

    void maybePlayNextSound() {
        synchronized (mQueue) {
            // No new sounds to play
            if (mQueue.size() == 0)
                return;

            // Current sound is not finished yet
            if (System.currentTimeMillis() < mLastSoundStartTime + mSoundDurations[mLastSoundId] + pauseDuration)
                return;

            mLastSoundStartTime = System.currentTimeMillis();
            mLastSoundId = mQueue.remove();
        }
        mSoundPool.play(mSoundIds[mLastSoundId], 1, 1, 0, 0, 1);
        setTimeout(() -> maybePlayNextSound(), mSoundDurations[mLastSoundId] + pauseDuration + timeoutAdditionalDuration);
    }

    private static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
            }
        }).start();
    }
}
