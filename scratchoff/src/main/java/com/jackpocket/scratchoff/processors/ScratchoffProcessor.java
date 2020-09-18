package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.ScratchoffController;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    public interface Delegate {
        public void postNewScratchedMotionEvents(List<ScratchPathPoint> events);
    }

    private static final int SLEEP_DELAY = 10;

    private WeakReference<Delegate> delegate;

    private ThresholdProcessor thresholdProcessor;
    private InvalidationProcessor invalidationProcessor;

    private final List<ScratchPathPoint> queuedEvents = new ArrayList<ScratchPathPoint>();

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

    public void synchronouslyQueueEvent(ScratchPathPoint event) {
        synchronized (queuedEvents) {
            queuedEvents.add(event);
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            Delegate delegate = this.delegate.get();
            List<ScratchPathPoint> events = synchronouslyDequeueEvents();

            if (delegate != null && 0 < events.size()) {
                delegate.postNewScratchedMotionEvents(events);
                invalidationProcessor.postNewScratchedMotionEvents(events);
                thresholdProcessor.postNewScratchedMotionEvents(events);
            }

            Thread.sleep(SLEEP_DELAY);
        }
    }

    protected List<ScratchPathPoint> synchronouslyDequeueEvents() {
        final List<ScratchPathPoint> tempEvents;

        synchronized (queuedEvents) {
            tempEvents = new ArrayList<ScratchPathPoint>(queuedEvents);

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

    protected List<ScratchPathPoint> getQueuedEvents() {
        synchronized (queuedEvents) {
            return queuedEvents;
        }
    }
}
