package com.jackpocket.scratchoff.processors;

import android.graphics.Path;

import com.jackpocket.scratchoff.ScratchoffController;

import java.util.ArrayList;
import java.util.List;

public class InvalidationProcessor extends Processor {

    private static final int SLEEP_DELAY = 15;

    private ScratchoffController controller;
    private final List<Path> queuedEvents = new ArrayList<Path>();

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(ScratchoffController controller) {
        this.controller = controller;
    }

    @SuppressWarnings("WeakerAccess")
    public void addPaths(List<Path> paths) {
        synchronized (queuedEvents){
            queuedEvents.addAll(paths);
        }
    }

    @Override
    protected void doInBackground() throws Exception {
        while (isActive() && controller.isProcessingAllowed()){
            synchronized (queuedEvents) {
                if(queuedEvents.size() > 0)
                    controller.getScratchImageLayout()
                            .postInvalidate();

                queuedEvents.clear();
            }

            Thread.sleep(SLEEP_DELAY);
        }
    }
}
