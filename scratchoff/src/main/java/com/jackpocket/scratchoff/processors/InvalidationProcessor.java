package com.jackpocket.scratchoff.processors;

import android.graphics.Path;

import com.jackpocket.scratchoff.ScratchoffController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class InvalidationProcessor extends Processor {

    public interface Delegate {
        public void postInvalidateScratchableLayout();
    }

    private static final int SLEEP_DELAY = 15;

    private WeakReference<Delegate> delegate;
    private final List<Path> queuedEvents = new ArrayList<Path>();

    @SuppressWarnings("WeakerAccess")
    public InvalidationProcessor(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    @SuppressWarnings("WeakerAccess")
    public void addPaths(List<Path> paths) {
        synchronized (queuedEvents){
            queuedEvents.addAll(paths);
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
