package com.jackpocket.scratchoff.processors;

import android.graphics.Path;
import android.view.MotionEvent;

import com.jackpocket.scratchoff.ScratchoffController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    public interface Delegate {
        public void postNewScratchedPaths(List<Path> paths);
    }

    private static final int SLEEP_DELAY = 10;

    private WeakReference<Delegate> delegate;

    private ThresholdProcessor thresholdProcessor;
    private InvalidationProcessor invalidationProcessor;

    private final List<Path> queuedEvents = new ArrayList<Path>();

    private int[] lastTouchEvent = new int[] { 0, 0 };

    public ScratchoffProcessor(ScratchoffController controller) {
        this.delegate = new WeakReference<Delegate>(controller);

        this.thresholdProcessor = new ThresholdProcessor(
                controller.getTouchRadiusPx(),
                controller.getThresholdPercent(),
                controller);

        this.invalidationProcessor = new InvalidationProcessor(controller);
    }

    public ScratchoffProcessor(
            Delegate delegate,
            ThresholdProcessor thresholdProcessor,
            InvalidationProcessor invalidationProcessor) {

        this.delegate = new WeakReference<Delegate>(delegate);
        this.thresholdProcessor = thresholdProcessor;
        this.invalidationProcessor = invalidationProcessor;
    }

    public void onReceiveMotionEvent(MotionEvent e, boolean actionDown) {
        onReceiveMotionEvent((int) e.getX(), (int) e.getY(), actionDown);
    }

    protected void onReceiveMotionEvent(int x, int y, boolean actionDown) {
        int[] event = new int[] { x, y };

        if (!actionDown) {
            Path path = new Path();
            path.moveTo(lastTouchEvent[0], lastTouchEvent[1]);
            path.lineTo(event[0], event[1]);

            synchronized (queuedEvents) {
                queuedEvents.add(path);
            }
        }

        this.lastTouchEvent = event;
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            Delegate delegate = this.delegate.get();
            List<Path> events = synchronouslyDequeueEvents();

            if (delegate != null && 0 < events.size()) {
                delegate.postNewScratchedPaths(events);

                invalidationProcessor.addPaths(events);
                thresholdProcessor.addPaths(events);
            }

            Thread.sleep(SLEEP_DELAY);
        }
    }

    private List<Path> synchronouslyDequeueEvents() {
        final List<Path> tempEvents;

        synchronized (queuedEvents) {
            tempEvents = new ArrayList<Path>(queuedEvents);

            this.queuedEvents.clear();
        }

        return tempEvents;
    }

    @Override
    public void start() {
        super.start();

        thresholdProcessor.start();
        invalidationProcessor.start();
    }

    @Override
    public void stop() {
        thresholdProcessor.stop();
        invalidationProcessor.stop();

        super.stop();
    }

    protected List<Path> getQueuedEvents() {
        synchronized (queuedEvents) {
            return queuedEvents;
        }
    }
}
