package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class InvalidationProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private static final int SLEEP_DELAY = 15;
    private static final int SLEEP_DELAY_RUNNING_NO_EVENTS = 50;

    private WeakReference<Delegate> delegate;
    private final ScratchPathQueue queue = new ScratchPathQueue();

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    @Override
    public void enqueueScratchMotionEvents(List<ScratchPathPoint> events) {
        queue.enqueue(events);
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            Delegate delegate = this.delegate.get();
            List<ScratchPathPoint> dequeuedEvents = queue.dequeue();

            if (delegate == null || dequeuedEvents.size() < 1) {
                Thread.sleep(SLEEP_DELAY_RUNNING_NO_EVENTS);

                continue;
            }

            delegate.postInvalidateScratchableLayout();

            Thread.sleep(SLEEP_DELAY);
        }
    }
}
