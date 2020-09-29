package com.jackpocket.scratchoff.paths;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collection;

public class ScratchPathManager {

    private static final int POINTER_LIMIT = 10;

    private Path[] activePaths = new Path[POINTER_LIMIT];
    private int[] lastActiveActions = new int[POINTER_LIMIT];

    private final ArrayList<Path> paths = new ArrayList<>();

    public void addMotionEvents(Collection<ScratchPathPoint> events) {
        for (ScratchPathPoint event : events)
            addMotionEvent(event);
    }

    public void addMotionEvent(ScratchPathPoint event) {
        if (POINTER_LIMIT <= event.pointerIndex)
            return;

        synchronized (paths) {
            switch (event.action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    handleTouchDown(event.pointerIndex, event.x, event.y);

                    break;
                default:
                    handleTouchMove(event.pointerIndex, event.x, event.y);

                    break;
            }

            lastActiveActions[event.pointerIndex] = event.action;
        }
    }

    protected void handleTouchDown(int pointerIndex, float x, float y) {
        createPath(pointerIndex, x, y);
    }

    protected void handleTouchMove(int pointerIndex, float x, float y) {
        // If the last event for this pointer was MotionEvent.ACTION_POINTER_UP
        // then it's possible the position has changed, so we should recreate
        // the Path to avoid errors
        if (MotionEvent.ACTION_POINTER_UP == lastActiveActions[pointerIndex])
            createPath(pointerIndex, x, y);

        this.activePaths[pointerIndex].lineTo(x, y);
    }

    protected void createPath(int pointerIndex, float x, float y) {
        this.activePaths[pointerIndex] = new Path();
        this.activePaths[pointerIndex].moveTo(x, y);

        this.paths.add(activePaths[pointerIndex]);
    }

    public void draw(Canvas canvas, Paint paint) {
        synchronized (paths) {
            for (Path path : paths)
                canvas.drawPath(path, paint);
        }
    }

    public void clear() {
        synchronized (paths) {
            this.activePaths = new Path[POINTER_LIMIT];
            this.lastActiveActions = new int[POINTER_LIMIT];
            this.paths.clear();
        }
    }

    protected ArrayList<Path> getPaths() {
        return paths;
    }

    public static Paint createBaseScratchoffPaint(int touchRadiusPx) {
        Paint markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeCap(Paint.Cap.ROUND);
        markerPaint.setStrokeJoin(Paint.Join.ROUND);
        markerPaint.setStrokeWidth(touchRadiusPx * 2);

        return markerPaint;
    }
}
