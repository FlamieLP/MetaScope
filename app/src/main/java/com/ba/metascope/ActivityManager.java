package com.ba.metascope;


import static com.ba.metascope.ActivityManager.PredictionMode.*;

public class ActivityManager {
    private boolean isPredicting = false;
    public PredictionMode mode = DONT_PREDICT;

    public ActivityManager() {}

    public boolean isPredicting() {
        return isPredicting;
    }

    public void setPredicting(boolean predicting) {
        isPredicting = predicting;
    }

    public PredictionMode getMode() {
        return mode;
    }

    public boolean isActiveMode() {
        return mode != DONT_PREDICT;
    }

    public void setActive(PredictionMode mode) {
        this.mode = mode;
    }

    /**
     * Stops predicting.
     */
    public void setInactive() {
        this.mode = DONT_PREDICT;
        this.isPredicting = false;
    }

    public enum PredictionMode {
        SCAN_ALL,
        SCAN_AT_POSITION,
        SCAN_AT_POSITION_THEN_TRACK_FROM_CLASS,
        TRACK_ALL,
        TRACK_ALL_FROM_CLASS,
        SCAN_AND_TRACK_AT_POSITION,
        DONT_PREDICT
    }
}
