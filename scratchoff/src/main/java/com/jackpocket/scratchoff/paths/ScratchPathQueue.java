package com.jackpocket.scratchoff.paths;

import java.util.ArrayList;
import java.util.List;

public class ScratchPathQueue {

    private final ArrayList<ScratchPathPoint> events = new ArrayList<ScratchPathPoint>();

    public ScratchPathQueue() { }

    public void enqueue(ScratchPathPoint event) {
        synchronized (this.events) {
            this.events.add(event);
        }
    }

    public void enqueue(List<ScratchPathPoint> events) {
        synchronized (this.events) {
            this.events.addAll(events);
        }
    }

    public int size() {
        synchronized (events) {
            return events.size();
        }
    }

    public List<ScratchPathPoint> dequeue() {
        synchronized (events) {
            List<ScratchPathPoint> tempEvents = new ArrayList<ScratchPathPoint>(events);

            events.clear();

            return tempEvents;
        }
    }

    public void clear() {
        synchronized (events) {
            events.clear();
        }
    }
}
