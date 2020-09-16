package com.jackpocket.scratchoff.processors;

import android.graphics.Path;
import android.view.MotionEvent;

import com.jackpocket.scratchoff.ScratchoffController;

import java.util.ArrayList;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    private static final int SLEEP_DELAY = 10;

    private ScratchoffController controller;

    private ThresholdProcessor thresholdProcessor;
    private InvalidationProcessor invalidationProcessor;

    private final List<Path> queuedEvents = new ArrayList<Path>();

    private int[] lastTouchEvent = new int[]{ 0, 0 };

    public ScratchoffProcessor(ScratchoffController controller) {
        this.controller = controller;

        this.thresholdProcessor = new ThresholdProcessor(controller);
        this.invalidationProcessor = new InvalidationProcessor(controller);
    }

    public void setScratchValueChangedListener(ThresholdProcessor.ScratchValueChangedListener scratchValueChangedListener) {
        thresholdProcessor.setScratchValueChangedListener(scratchValueChangedListener);
    }

    public void onReceiveMotionEvent(MotionEvent e, boolean actionDown) {
        int[] event = new int[]{
                (int) e.getX(),
                (int) e.getY()
        };

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
        while (isActive(id) && controller.isProcessingAllowed()) {
            final List<Path> events = synchronouslyDequeueEvents();

            if (events.size() > 0){
                controller.post(new Runnable() {
                    public void run() {
                        controller.addPaths(events);
                    }
                });

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
}
