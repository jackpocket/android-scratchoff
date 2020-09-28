package com.jackpocket.scratchoff.processors;

public abstract class Processor implements Runnable {

    protected static long THREAD_ID_INACTIVE = -1;

    private long activeThreadId = THREAD_ID_INACTIVE;
    private long claimedRunningThreadId = THREAD_ID_INACTIVE;
    private final Boolean lock = false;

    public void start() {
        stop();
        obtainNewThreadId();
        startProcessorThread();
    }

    protected long obtainNewThreadId() {
        synchronized (lock) {
            this.activeThreadId = System.nanoTime();

            return Long.valueOf(activeThreadId);
        }
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
        catch(Throwable e) { e.printStackTrace(); }

        if (claimed && isActive(threadId))
            stop();
    }

    protected void claimActiveThread(long id) {
        synchronized (lock) {
            if (claimedRunningThreadId == id)
                throw new RuntimeException("Active processor thread ID already claimed!");

            if (id == THREAD_ID_INACTIVE)
                throw new RuntimeException("Processor cannot claim the inactive thread ID!");

            this.claimedRunningThreadId = id;
        }
    }

    protected Long getCurrentThreadId() {
        synchronized (lock) {
            return Long.valueOf(this.activeThreadId);
        }
    }

    protected abstract void doInBackground(long id) throws Exception;

    @SuppressWarnings("WeakerAccess")
    public void stop() {
        synchronized (lock) {
            this.activeThreadId = THREAD_ID_INACTIVE;
        }
    }

    protected boolean isActive(long id) {
        synchronized (lock) {
            if (id != activeThreadId)
                return false;
        }

        return isActive();
    }

    public boolean isActive() {
        synchronized (lock) {
            return activeThreadId != THREAD_ID_INACTIVE;
        }
    }
}
