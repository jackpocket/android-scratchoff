package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.tools.Sleeper;

import java.lang.ref.WeakReference;
import java.util.List;

public class InvalidationProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private WeakReference<Delegate> delegate;
    private boolean invalidationRequired = false;

    private final Sleeper sleeper = new Sleeper(10, 50, 3000);

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(Delegate delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    @Override
    public void enqueueScratchMotionEvents(List<ScratchPathPoint> events) {
        synchronized (sleeper) {
            this.invalidationRequired = invalidationRequired || 0 < events.size();
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            performBackgroundInvalidationLoopSegment(id);
        }
    }

    protected void performBackgroundInvalidationLoopSegment(long id) throws InterruptedException {
        Delegate delegate = this.delegate.get();

        if (delegate == null || !isInvalidationRequired()) {
            sleeper.sleep();

            return;
        }

        synchronized (sleeper) {
            this.invalidationRequired = false;
        }

        delegate.postInvalidateScratchableLayout();

        if (!isActive(id))
            return;

        sleeper.notifyTriggered();
        sleeper.sleep();
    }

    @Override
    public void stop() {
        super.stop();

        this.sleeper.reset();
    }

    protected boolean isInvalidationRequired() {
        synchronized (sleeper) {
            return invalidationRequired;
        }
    }
}
