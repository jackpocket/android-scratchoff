package com.jackpocket.scratchoff.processors;

public abstract class Processor implements Runnable {

    private boolean active = false;
    private Thread activeThread;

    public void start() {
        this.stop();

        this.active = true;

        this.activeThread = new Thread(this);
        this.activeThread.start();
    }

    @Override
    public void run() {
        try {
            doInBackground();
        }
        catch(Throwable e){ e.printStackTrace(); }

        this.active = false;
        this.activeThread = null;
    }

    protected abstract void doInBackground() throws Exception;

    public void cancel() {
        this.active = false;
        this.stop();
    }

    public void stop() {
        try {
            if (activeThread != null)
                activeThread.stop();
        }
        catch(Exception e){ }

        this.activeThread = null;
    }

    public boolean isActive(){
        return active;
    }
}
