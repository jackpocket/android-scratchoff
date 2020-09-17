package com.jackpocket.scratchoff.processors;

public abstract class Processor implements Runnable {

    private static long THREAD_ID_INACTIVE = -1;

    private long activeThreadId = THREAD_ID_INACTIVE;

    public void start() {
        this.stop();

        this.activeThreadId = System.nanoTime();

        new Thread(this)
                .start();
    }

    @Override
    public void run() {
        long threadId = Long.valueOf(this.activeThreadId);

        try {
            doInBackground(threadId);
        }
        catch(Throwable e) { e.printStackTrace(); }

        if (isActive(threadId))
            stop();
    }

    protected abstract void doInBackground(long id) throws Exception;

    @Deprecated()
    public void cancel() {
        this.stop();
    }

    @SuppressWarnings("WeakerAccess")
    public void stop() {
        this.activeThreadId = THREAD_ID_INACTIVE;
    }

    protected boolean isActive(long id) {
        return id == activeThreadId && isActive();
    }

    public boolean isActive() {
        return activeThreadId != THREAD_ID_INACTIVE;
    }
}
