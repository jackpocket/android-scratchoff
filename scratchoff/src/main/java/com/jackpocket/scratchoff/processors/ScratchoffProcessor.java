package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.ScratchoffController;
import com.jackpocket.scratchoff.tools.Sleeper;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    public interface Delegate {
        public void enqueueScratchMotionEvents(List<ScratchPathPoint> events);
    }

    private final Sleeper sleeper = new Sleeper(10, 50, 3000);

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

            if (delegate == null || events.size() == 0) {
                sleeper.sleep();

                continue;
            }

            delegate.enqueueScratchMotionEvents(events);
            invalidationProcessor.enqueueScratchMotionEvents(events);
            thresholdProcessor.enqueueScratchMotionEvents(events);

            if (!isActive(id))
                return;

            sleeper.notifyTriggered();
            sleeper.sleep();
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

        sleeper.reset();

        super.stop();
    }

    protected ScratchPathQueue getQueue() {
        return queue;
    }
}
