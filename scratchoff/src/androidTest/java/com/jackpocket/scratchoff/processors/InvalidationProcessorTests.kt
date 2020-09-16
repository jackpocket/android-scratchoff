package com.jackpocket.scratchoff.processors

import android.graphics.Path
import org.junit.Assert.assertEquals
import org.junit.Test

class InvalidationProcessorTests {

    @Test
    fun testCountColorMatches() {
        val subject = intArrayOf(0, 0, 1, 1, 1)

        assertEquals(2, ThresholdProcessor.countColorMatches(0, subject))
        assertEquals(3, ThresholdProcessor.countColorMatches(1, subject))
    }

    @Test
    fun testInvalidationProcessorCallsDelegateOnNewData() {
        var invalidationCallCount: Int = 0
        val delegate = InvalidationProcessor.Delegate {
            invalidationCallCount += 1
        }

        val processor = object: InvalidationProcessor(delegate) {
            override fun isActive(id: Long): Boolean {
                return invalidationCallCount == 0
            }
        }

        assertEquals(0, invalidationCallCount)

        processor.addPaths(listOf(Path()))
        processor.run()

        assertEquals(1, invalidationCallCount)
    }
}