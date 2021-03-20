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

    @Test
    fun testStartCallsCreateProcessorThread() {
        val processor = LoggingProcessor()

        assertEquals(0, processor.startProcessorThreadCalls)

        processor.start()

        assertEquals(1, processor.startProcessorThreadCalls)

        Processor.stop(processor)
        Processor.startNotActive(processor)

        assertEquals(2, processor.startProcessorThreadCalls)
    }

    @Test
    fun testStartNotActiveDoesNotCallsCreateProcessorThreadWhenNullOrActive() {
        val processor = LoggingProcessor()
        processor.obtainNewThreadId()

        assertEquals(0, processor.startProcessorThreadCalls)

        Processor.startNotActive(null)
        Processor.startNotActive(processor)

        assertEquals(0, processor.startProcessorThreadCalls)
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