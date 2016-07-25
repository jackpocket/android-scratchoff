package com.jackpocket.scratchoff.processors;

import android.graphics.Path;

import com.jackpocket.scratchoff.ScratchoffController;

import java.util.ArrayList;

public class InvalidationProcessor extends Processor {

    private static final int SLEEP_DELAY = 15;

    private ScratchoffController controller;
    private ArrayList<Path> queuedEvents = new ArrayList<Path>();

    public InvalidationProcessor(ScratchoffController controller) {
        this.controller = controller;
    }

    public void onReceievePaths(ArrayList<Path> paths) {
        queuedEvents.addAll(paths);
    }

    @Override
    protected void doInBackground() throws Exception {
        while(isActive()){
            ArrayList<Path> tempEvents = queuedEvents;
            queuedEvents = new ArrayList<Path>();

            if(tempEvents.size() > 0)
                controller.getScratchImageLayout()
                        .postInvalidate();

            sleep(SLEEP_DELAY);
        }
    }

}
