package com.jackpocket.scratchoff.paths;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collection;

public class ScratchPathManager {

    private Path activePath = new Path();
    private final ArrayList<Path> paths = new ArrayList<>();

    public void addMotionEvents(Collection<ScratchPathPoint> events) {
        for (ScratchPathPoint event : events)
            addMotionEvent(event);
    }

    public void addMotionEvent(ScratchPathPoint event) {
        switch (event.action) {
            case MotionEvent.ACTION_UP:
                handleTouchUp();

                break;
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(event.x, event.y);

                break;
            default:
                handleTouchMove(event.x, event.y);

                break;
        }
    }

    protected void handleTouchDown(float x, float y) {
        synchronized (paths) {
            if (!activePath.isEmpty()) {
                paths.add(activePath);
            }

            this.activePath = new Path();
            this.activePath.moveTo(x, y);
        }
    }

    protected void handleTouchMove(float x, float y) {
        synchronized (paths) {
            this.activePath.lineTo(x, y);
        }
    }

    protected void handleTouchUp() {
        synchronized (paths) {
            paths.add(activePath);

            this.activePath = new Path();
        }
    }

    public void draw(Canvas canvas, Paint paint) {
        for (Path path : paths)
            canvas.drawPath(path, paint);

        canvas.drawPath(getActivePath(), paint);
    }

    public void clear() {
        synchronized (paths) {
            this.activePath = new Path();
            this.paths.clear();
        }
    }

    public Path getActivePath() {
        synchronized (paths) {
            return activePath;
        }
    }

    public ArrayList<Path> getPaths() {
        synchronized (paths) {
            return paths;
        }
    }

    public int getPathCount() {
        synchronized (paths) {
            return paths.size();
        }
    }
}
