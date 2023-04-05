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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class PresenceManager {
    SoundManager mSoundManager;
    int mClasses;

    boolean[] mClassesActivated;

    private class RecognitionsWithTime {
        List<Recognition> recognitions;
        Long timeMillis;

        RecognitionsWithTime(List<Recognition> recognitions, Long timeMillis) {
            this.recognitions = recognitions;
            this.timeMillis = timeMillis;
        }
    };

    LinkedList<RecognitionsWithTime> mRecHistory;

    PresenceManager(SoundManager soundManager, int nClasses) {
        mRecHistory = new LinkedList<RecognitionsWithTime>();
        mSoundManager = soundManager;
        mClasses = nClasses;
        mClassesActivated = new boolean[mClasses];
        Arrays.fill(mClassesActivated, false);
    }

    void SaveRecognitions(List<Recognition> recognitions) {
        Long timeMillis = System.currentTimeMillis();
        mRecHistory.addLast(new RecognitionsWithTime(recognitions, timeMillis));
        int nCheckEls = mRecHistory.size() - Config.MIN_QUEUE_ELEMENTS;
        if (nCheckEls > 0) {
            for(int n = 0; n < nCheckEls; n++) {
                if (timeMillis - mRecHistory.peekFirst().timeMillis > Config.MIN_QUEUE_MILLIS) {
                    mRecHistory.removeFirst();
                }
            }
        }

        // Calculate deactivation window parameters
        Long historyTimeRange = timeMillis - mRecHistory.get(0).timeMillis;
        Long startingDeactivationWindowTime = timeMillis - (long)(historyTimeRange * Config.DEACTIVATION_WINDOW_RATIO);
        int startingDeactivationWindowIndex = 0;
        ListIterator<RecognitionsWithTime> it = mRecHistory.listIterator();
        while (it.hasNext()) {
            int index = it.nextIndex();
            RecognitionsWithTime elem = it.next();
            if (elem.timeMillis >= startingDeactivationWindowTime) {
                startingDeactivationWindowIndex = index;
                break;
            }
        }

        // Calculate precences:
        // 1) over all remaining records for activation
        // 2) over deactivation window for deactivation
        int[] nPresent = calcNPresent(0);
        int[] nDeactivationPresent = calcNPresent(startingDeactivationWindowIndex);

        for (int c = 0; c < mClasses; c++) {
            if (!mClassesActivated[c]) {
                if (nPresent[c] >= Config.MIN_ACTIVATION_RATIO * mRecHistory.size()) {
                    mSoundManager.enqueueClassSound(c);
                    mClassesActivated[c] = true;
                }
            } else {
                if (nDeactivationPresent[c] == 0) {
                    mClassesActivated[c] = false;
                }
            }
        }
    }

    // Calculates number of presence of each class
    private int[] calcNPresent(int startIndex) {
        int[] ret = new int[mClasses];
        int[] frameCount = new int[mClasses];

        ListIterator<RecognitionsWithTime> it = mRecHistory.listIterator();
        while (it.hasNext()) {
            int index = it.nextIndex();
            if (index == startIndex)
                break;
            it.next();
        }

        while (it.hasNext()) {
            RecognitionsWithTime frame = it.next();
            Arrays.fill(frameCount, 0);
            for (Recognition rec : frame.recognitions) {
                frameCount[rec.classId] = 1;
            }
            for(int i = 0; i < mClasses; i++) {
                ret[i] += frameCount[i];
            }
        }

        return ret;
    }

}
