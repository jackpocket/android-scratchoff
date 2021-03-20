package com.jackpocket.scratchoff.processors;

import java.util.concurrent.atomic.AtomicLong;

public abstract class Processor implements Runnable {

    protected static long THREAD_ID_INACTIVE = -1;

    private final AtomicLong activeThreadId = new AtomicLong(THREAD_ID_INACTIVE);
    private final AtomicLong claimedRunningThreadId = new AtomicLong(THREAD_ID_INACTIVE);

    public void start() {
        stop();
        obtainNewThreadId();
        startProcessorThread();
    }

    protected synchronized long obtainNewThreadId() {
        long id = System.nanoTime();

        this.activeThreadId.set(id);

        return id;
    }

    protected void startProcessorThread() {
        new Thread(this)
                .start();
    }

    @Override
    public void run() {
        long threadId = getCurrentThreadId();
        boolean claimed = false;

        try {
            claimActiveThread(threadId);

            claimed = true;

            if (isActive(threadId))
                doInBackground(threadId);
        }
        catch (IllegalThreadStateException e) { e.printStackTrace(); }
        catch (Throwable e) { e.printStackTrace(); }

        if (claimed && isActive(threadId))
            stop();
    }

    protected synchronized void claimActiveThread(long id) {
        if (claimedRunningThreadId.get() == id)
            throw new IllegalThreadStateException("Active processor thread ID already claimed!");

        if (id == THREAD_ID_INACTIVE)
            throw new IllegalThreadStateException("Processor cannot claim the inactive thread ID!");

        this.claimedRunningThreadId.set(id);
    }

    protected Long getCurrentThreadId() {
        return this.activeThreadId.get();
    }

    protected abstract void doInBackground(long id) throws Exception;

    @SuppressWarnings("WeakerAccess")
    public void stop() {
        this.activeThreadId.set(THREAD_ID_INACTIVE);
    }

    protected boolean isActive(long id) {
        if (activeThreadId.get() != id)
            return false;

        return isActive();
    }

    public boolean isActive() {
        return THREAD_ID_INACTIVE != activeThreadId.get();
    }
}
