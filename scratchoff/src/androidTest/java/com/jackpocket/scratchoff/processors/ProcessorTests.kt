package com.jackpocket.scratchoff.processors

import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.RuntimeException

class ProcessorTests {

    @Test
    fun testInactiveWithoutActiveThreadId() {
        val processor = LoggingProcessor()

        assertEquals(false, processor.isActive)

        processor.run()

        assertEquals(0, processor.doInBackgroundCalls)
        assertEquals(false, processor.isActive)

        processor.obtainNewThreadId()

        assertEquals(true, processor.isActive)
    }

    @Test
    fun testActiveRunCompletionStopsProcessor() {
        val processor = LoggingProcessor()
        processor.obtainNewThreadId()

        assertEquals(true, processor.isActive)

        processor.run()

        assertEquals(1, processor.doInBackgroundCalls)
        assertEquals(false, processor.isActive)
    }

    @Test
    fun testStopDeactivatesActiveThread() {
        val processor = LoggingProcessor()
        val originalThreadId = processor.obtainNewThreadId()

        assertEquals(true, processor.isActive(originalThreadId))

        processor.stop()

        assertEquals(false, processor.isActive(originalThreadId))
    }

    @Test(expected = RuntimeException::class)
    fun testDuplicateClaimingThrowsRuntimeException() {
        val processor = LoggingProcessor()
        val threadId = processor.obtainNewThreadId()

        processor.claimActiveThread(threadId)
        processor.claimActiveThread(threadId)
    }

    @Test
    fun testRunWithDuplicateClaimDoesNothing() {
        val processor = LoggingProcessor()
        val threadId = processor.obtainNewThreadId()

        processor.claimActiveThread(threadId)
        processor.run()

        assertEquals(0, processor.doInBackgroundCalls)
        assertEquals(threadId, processor.currentThreadId)
    }

    @Test
    fun testRunWithoutObtainingIDDoesNothing() {
        val processor = LoggingProcessor()
        processor.run()

        assertEquals(0, processor.doInBackgroundCalls)
        assertEquals(Processor.THREAD_ID_INACTIVE, processor.currentThreadId)
    }

    private class LoggingProcessor: Processor() {

        var doInBackgroundCalls: Int = 0
            private set

        var startProcessorThreadCalls: Int = 0
            private set

        override fun startProcessorThread() {
            startProcessorThreadCalls += 1
        }

        override fun doInBackground(id: Long) {
            doInBackgroundCalls += 1
        }
    }
}