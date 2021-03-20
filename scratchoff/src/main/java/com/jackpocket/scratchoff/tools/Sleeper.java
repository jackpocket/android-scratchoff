package com.jackpocket.scratchoff.tools;

public class Sleeper {

    private long delayRunning;
    private long delaySleeping;
    private long sleepThresholdMs;

    private long lastTrigger = 0;

    private final Object lock = new Object();

    public Sleeper(long delayRunning, long delaySleeping, long sleepThresholdMs) {
        this.delayRunning = delayRunning;
        this.delaySleeping = delaySleeping;
        this.sleepThresholdMs = sleepThresholdMs;
    }

    public void sleep() throws InterruptedException {
        sleep(getDelayMs());
    }

    protected long getDelayMs() {
        synchronized (lock) {
            boolean beyondSleepThreshold = 0 < lastTrigger
                    && sleepThresholdMs < System.currentTimeMillis() - lastTrigger;

            return beyondSleepThreshold ? delaySleeping : delayRunning;
        }
    }

    protected void sleep(long delay) throws InterruptedException {
        Thread.sleep(delay);
    }

    public void notifyTriggered() {
        synchronized (lock) {
            this.lastTrigger = System.currentTimeMillis();
        }
    }

    public void reset() {
        synchronized (lock) {
            this.lastTrigger = 0;
        }
    }
}
