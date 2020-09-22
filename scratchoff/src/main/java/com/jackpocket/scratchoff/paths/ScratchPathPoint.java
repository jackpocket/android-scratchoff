package com.jackpocket.scratchoff.paths;

import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScratchPathPoint {

    public final int pointerIndex;
    public final float x;
    public final float y;
    public final int action;

    public ScratchPathPoint(
            int pointerIndex,
            float x,
            float y,
            int action) {

        this.pointerIndex = pointerIndex;
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

    public static List<ScratchPathPoint> create(MotionEvent event) {
        final int historySize = event.getHistorySize();
        final int pointersCount = event.getPointerCount();

        ArrayList<ScratchPathPoint> events = new ArrayList<ScratchPathPoint>();

        for (int historyIndex = 0; historyIndex < historySize; historyIndex++) {
            for (int pointerIndex = 0; pointerIndex < pointersCount; pointerIndex++) {
                events.add(
                        new ScratchPathPoint(
                                pointerIndex,
                                event.getHistoricalX(pointerIndex, historyIndex),
                                event.getHistoricalY(pointerIndex, historyIndex),
                                MotionEvent.ACTION_MOVE));
            }
        }

        for (int pointerIndex = 0; pointerIndex < pointersCount; pointerIndex++) {
            events.add(
                    new ScratchPathPoint(
                            pointerIndex,
                            event.getX(pointerIndex),
                            event.getY(pointerIndex),
                            event.getActionMasked()));
        }

        return events;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%d: %f, %f - %d", pointerIndex, x, y, action);
    }
}
