package com.jackpocket.scratchoff.processors;

public abstract class Processor implements Runnable {

    private long activeThreadId = 0;

    public void start() {
        this.stop();

        this.activeThreadId = System.nanoTime();

        new Thread(this)
                .start();
    }

    @Override
    public void run() {
        try {
            doInBackground(activeThreadId);
        }
        catch(Throwable e) { e.printStackTrace(); }
    }

    protected abstract void doInBackground(long id) throws Exception;

    @Deprecated()
    public void cancel() {
        this.stop();
    }

    @SuppressWarnings("WeakerAccess")
    public void stop() {
        this.activeThreadId = System.nanoTime();
    }

    protected boolean isActive(long id) {
        return id == activeThreadId;
    }
}
