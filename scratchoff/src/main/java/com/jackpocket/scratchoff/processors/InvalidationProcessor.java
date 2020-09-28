package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.tools.Sleeper;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.lang.ref.WeakReference;
import java.util.List;

public class InvalidationProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private final Sleeper sleeper = new Sleeper(10, 50, 3000);

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
            sleeper.sleep();

            return;
        }

        synchronized (lock) {
            this.invalidationRequired = false;
        }

        delegate.postInvalidateScratchableLayout();

        sleeper.notifyTriggered();
        sleeper.sleep();
    }

    @Override
    public void stop() {
        super.stop();

        this.sleeper.reset();
    }

    protected boolean isInvalidationRequired() {
        synchronized (lock) {
            return invalidationRequired;
        }
    }
}
