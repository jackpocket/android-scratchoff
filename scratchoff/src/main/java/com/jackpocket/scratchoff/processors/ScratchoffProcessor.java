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

    private List<Path> queuedEvents = new ArrayList<Path>();

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

        if(!actionDown){
            Path path = new Path();
            path.moveTo(lastTouchEvent[0], lastTouchEvent[1]);
            path.lineTo(event[0], event[1]);

            queuedEvents.add(path);
        }

        this.lastTouchEvent = event;
    }

    @Override
    protected void doInBackground() throws Exception {
        while(isActive() && controller.isProcessingAllowed()){
            final List<Path> tempEvents = queuedEvents;
            this.queuedEvents = new ArrayList<Path>();

            if(tempEvents.size() > 0){
                controller.post(new Runnable() {
                    public void run() {
                        controller.addPaths(tempEvents);
                    }
                });

                invalidationProcessor.addPaths(tempEvents);
                thresholdProcessor.addPaths(tempEvents);
            }

            Thread.sleep(SLEEP_DELAY);
        }
    }

    @Override
    public void start() {
        if(thresholdProcessor != null)
            thresholdProcessor.start();

        if(invalidationProcessor != null)
            invalidationProcessor.start();

        super.start();
    }

    @Override
    public void cancel() {
        if(thresholdProcessor != null)
            thresholdProcessor.cancel();

        if(invalidationProcessor != null)
            invalidationProcessor.cancel();

        super.cancel();
    }
}
