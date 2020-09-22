package com.jackpocket.scratchoff.processors

import android.view.MotionEvent
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class InvalidationProcessorTests {

    @Test
    fun testCallsDelegateOnNewDataOncePerBackgroundLoopSegmentWithInvalidationRequired() {
        var invalidationCallCount: Int = 0

        val processor = InvalidationProcessor(InvalidationProcessor.Delegate {
            invalidationCallCount += 1
        })

        assertEquals(0, invalidationCallCount)
        assertEquals(false, processor.isInvalidationRequired)

        processor.enqueueScratchMotionEvents(listOf(ScratchPathPoint(0f, 0f, MotionEvent.ACTION_DOWN)))

        assertEquals(true, processor.isInvalidationRequired)

        processor.performBackgroundInvalidationLoopSegment()

        assertEquals(1, invalidationCallCount)

        processor.enqueueScratchMotionEvents(listOf(
                ScratchPathPoint(0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0f, 0f, MotionEvent.ACTION_DOWN)
        ))
        processor.performBackgroundInvalidationLoopSegment()
        processor.performBackgroundInvalidationLoopSegment()
        processor.performBackgroundInvalidationLoopSegment()

        assertEquals(2, invalidationCallCount)
        assertEquals(false, processor.isInvalidationRequired)
    }
}