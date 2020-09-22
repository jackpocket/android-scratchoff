package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.ScratchoffController;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    public interface Delegate {
        public void enqueueScratchMotionEvents(List<ScratchPathPoint> events);
    }

    private static final int SLEEP_DELAY = 10;

    private WeakReference<Delegate> delegate;

    private ThresholdProcessor thresholdProcessor;
    private InvalidationProcessor invalidationProcessor;

    private final ScratchPathQueue queue = new ScratchPathQueue();

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

    public void enqueue(List<ScratchPathPoint> events) {
        queue.enqueue(events);
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            Delegate delegate = this.delegate.get();
            List<ScratchPathPoint> events = queue.dequeue();

            if (delegate != null && 0 < events.size()) {
                delegate.enqueueScratchMotionEvents(events);
                invalidationProcessor.enqueueScratchMotionEvents(events);
                thresholdProcessor.enqueueScratchMotionEvents(events);
            }

            Thread.sleep(SLEEP_DELAY);
        }
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

    protected ScratchPathQueue getQueue() {
        return queue;
    }
}
