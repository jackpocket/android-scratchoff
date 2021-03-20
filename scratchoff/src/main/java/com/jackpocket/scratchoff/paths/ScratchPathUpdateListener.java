package com.jackpocket.scratchoff.paths;

import java.util.List;

public interface ScratchPathUpdateListener {
    public void enqueuePathUpdates(List<ScratchPathPoint> events);
}
