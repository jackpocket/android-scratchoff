package com.jackpocket.scratchoff.tools;

import java.util.concurrent.atomic.AtomicLong;

public class Sleeper {

    private final long delayRunning;
    private final long delaySleeping;
    private final long sleepThresholdMs;

    private final AtomicLong lastTrigger = new AtomicLong(0L);

    public Sleeper(long delayRunning, long delaySleeping, long sleepThresholdMs) {
        this.delayRunning = delayRunning;
        this.delaySleeping = delaySleeping;
        this.sleepThresholdMs = sleepThresholdMs;
    }

    public void sleep() throws InterruptedException {
        sleep(getDelayMs());
    }

    protected long getDelayMs() {
        long last = lastTrigger.get();

        boolean beyondSleepThreshold = 0 < last
                && sleepThresholdMs < System.currentTimeMillis() - last;

        return beyondSleepThreshold ? delaySleeping : delayRunning;
    }

    protected void sleep(long delay) throws InterruptedException {
        Thread.sleep(delay);
    }

    public void notifyTriggered() {
        this.lastTrigger.set(System.currentTimeMillis());
    }

    public void reset() {
        this.lastTrigger.set(0L);
    }
}
