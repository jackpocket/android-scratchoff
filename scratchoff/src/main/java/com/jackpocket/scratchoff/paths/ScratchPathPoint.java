package com.jackpocket.scratchoff.paths;

import android.view.MotionEvent;

public class ScratchPathPoint {

    public final float x;
    public final float y;
    public final int action;

    public ScratchPathPoint(MotionEvent event) {
        this.x = event.getX();
        this.y = event.getY();
        this.action = event.getAction();
    }

    public ScratchPathPoint(float x, float y, int action) {
        this.x = x;
        this.y = y;
        this.action = action;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ScratchPathPoint))
            return false;

        ScratchPathPoint another = (ScratchPathPoint) obj;

        return another.x == this.x
                && another.y == this.y
                && another.action == this.action;
    }
}
