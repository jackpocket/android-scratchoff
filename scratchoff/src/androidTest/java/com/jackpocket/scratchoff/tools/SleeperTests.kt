package com.jackpocket.scratchoff.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class SleeperTests {

    @Test
    fun testFirstSleepAndResetStartsWithRunningSleepDelay() {
        val sleeper = LoggingSleeper(1, 2, 0)

        assertEquals(0, sleeper.currentSleep)

        sleeper.sleep()

        assertEquals(1, sleeper.currentSleep)

        Thread.sleep(1)

        sleeper.reset()
        sleeper.sleep()

        assertEquals(1, sleeper.currentSleep)
    }

    @Test
    fun testSleepAfterThresholdWithoutTriggerSleepsWithSleepingSleepDelay() {
        val sleeper = LoggingSleeper(1, 2, 0)
        sleeper.notifyTriggered()
        sleeper.sleep()

        Thread.sleep(1)

        sleeper.sleep()

        assertEquals(2, sleeper.currentSleep)
    }

    @Test
    fun testSleepAfterThresholdWithTriggerSleepsWithRunningSleepDelay() {
        val sleeper = LoggingSleeper(1, 2, 0)
        sleeper.sleep()

        Thread.sleep(1)

        sleeper.notifyTriggered()
        sleeper.sleep()

        assertEquals(1, sleeper.currentSleep)
    }

    private class LoggingSleeper(
            running: Long,
            sleeping: Long,
            threshold: Long): Sleeper(running, sleeping, threshold) {

        var currentSleep: Long = 0

        override fun sleep(delay: Long) {
            this.currentSleep = delay
        }
    }
}