package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.lang.ref.WeakReference;
import java.util.List;

public class InvalidationProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private static final int SLEEP_DELAY = 10;
    private static final int SLEEP_DELAY_RUNNING_NO_EVENTS = 50;

    private WeakReference<Delegate> delegate;
    private boolean invalidationRequired = false;

    private final Boolean lock = false;

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    @Override
    public void enqueueScratchMotionEvents(List<ScratchPathPoint> events) {
        synchronized (lock) {
            this.invalidationRequired = invalidationRequired || 0 < events.size();
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            performBackgroundInvalidationLoopSegment();
        }
    }

    protected void performBackgroundInvalidationLoopSegment() throws InterruptedException {
        Delegate delegate = this.delegate.get();

        if (delegate == null || !isInvalidationRequired()) {
            Thread.sleep(SLEEP_DELAY_RUNNING_NO_EVENTS);

            return;
        }

        synchronized (lock) {
            this.invalidationRequired = false;
        }

        delegate.postInvalidateScratchableLayout();

        Thread.sleep(SLEEP_DELAY);
    }

    protected boolean isInvalidationRequired() {
        synchronized (lock) {
            return invalidationRequired;
        }
    }
}
