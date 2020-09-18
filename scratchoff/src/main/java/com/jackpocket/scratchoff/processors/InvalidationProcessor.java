package com.jackpocket.scratchoff.processors;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class InvalidationProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private static final int SLEEP_DELAY = 15;

    private WeakReference<Delegate> delegate;
    private final List<ScratchPathPoint> queuedEvents = new ArrayList<ScratchPathPoint>();

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    @Override
    public void postNewScratchedMotionEvents(List<ScratchPathPoint> events) {
        synchronized (queuedEvents){
            queuedEvents.addAll(events);
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id)) {
            synchronized (queuedEvents) {
                Delegate delegate = this.delegate.get();

                if (delegate != null && 0 < queuedEvents.size()) {
                    delegate.postInvalidateScratchableLayout();
                }

                queuedEvents.clear();
            }

            Thread.sleep(SLEEP_DELAY);
        }
    }
}
