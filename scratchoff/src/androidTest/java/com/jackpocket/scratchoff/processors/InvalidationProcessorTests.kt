package com.jackpocket.scratchoff.processors

import android.view.MotionEvent
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class InvalidationProcessorTests {

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

        processor.enqueueScratchMotionEvents(listOf(ScratchPathPoint(0f, 0f, MotionEvent.ACTION_DOWN)))
        processor.run()

        assertEquals(1, invalidationCallCount)
    }
}