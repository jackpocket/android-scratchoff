package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.ScratchoffController;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;
import com.jackpocket.scratchoff.tools.Sleeper;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchoffProcessor extends Processor {

    public interface Delegate {
        public void enqueueScratchMotionEvents(List<ScratchPathPoint> events);
    }

    private ThresholdProcessor thresholdProcessor;
    private InvalidationProcessor invalidationProcessor;

    private final ScratchPathQueue queue = new ScratchPathQueue();

    private final Sleeper sleeper = new Sleeper(10, 50, 3000);

    public ScratchoffProcessor(ScratchoffController controller) {
        this.thresholdProcessor = new ThresholdProcessor(
                controller.getTouchRadiusPx(),
                controller.getThresholdPercent(),
                controller);

        this.invalidationProcessor = new InvalidationProcessor(controller);
    }

    public ScratchoffProcessor(
            ThresholdProcessor thresholdProcessor,
            InvalidationProcessor invalidationProcessor) {

        this.thresholdProcessor = thresholdProcessor;
        this.invalidationProcessor = invalidationProcessor;
    }

    public void enqueue(List<ScratchPathPoint> events) {
        queue.enqueue(events);
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            List<ScratchPathPoint> events = queue.dequeue();

            if (events.size() == 0) {
                sleeper.sleep();

                continue;
            }

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
