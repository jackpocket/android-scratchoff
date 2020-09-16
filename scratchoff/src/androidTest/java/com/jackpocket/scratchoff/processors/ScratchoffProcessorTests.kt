package com.jackpocket.scratchoff.processors

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchoffProcessorTests {

    @Test
    fun testScratchoffProcessorCorrectlyAddsPaths() {
        val processor = ScratchoffProcessor(null, null, null)

        assertEquals(0, processor.queuedEvents.size)

        processor.onReceiveMotionEvent(1, 1, true)
        processor.onReceiveMotionEvent(2, 2, false)
        processor.onReceiveMotionEvent(3, 3, false)
        processor.onReceiveMotionEvent(4, 4, true)
        processor.onReceiveMotionEvent(5, 5, false)

        assertEquals(3, processor.queuedEvents.size)

        processor.computeAndAssertBounds(0, RectF(1f, 1f, 2f, 2f))
        processor.computeAndAssertBounds(1, RectF(2f, 2f, 3f, 3f))
        processor.computeAndAssertBounds(2, RectF(4f, 4f, 5f, 5f))
    }

    @Test
    fun testScratchoffProcessorSendsPathsToSubProcessors() {
        val expectedResult = RectF(1f, 1f, 2f, 2f)
        val collectedPaths = mutableListOf<Path>()

        val processor = object: ScratchoffProcessor(
                ScratchoffProcessor.Delegate {
                    collectedPaths.addAll(it)
                },
                object: ThresholdProcessor(0, 0.0, object: ThresholdProcessor.Delegate {
                    override fun postScratchPercentChanged(percent: Double) { }
                    override fun postScratchThresholdReached() { }
                    override fun getScratchableLayoutSize(): IntArray {
                        return intArrayOf()
                    }
                }) {
                    override fun addPaths(paths: MutableList<Path>) {
                        collectedPaths.addAll(paths)
                    }
                },
                object: InvalidationProcessor(Delegate {  }) {
                    override fun addPaths(paths: MutableList<Path>) {
                        collectedPaths.addAll(paths)
                    }
                }) {
            override fun isActive(id: Long): Boolean {
                return collectedPaths.isEmpty()
            }
        }

        assertEquals(0, collectedPaths.size)

        processor.onReceiveMotionEvent(1, 1, true)
        processor.onReceiveMotionEvent(2, 2, false)
        processor.run()

        assertEquals(0, processor.queuedEvents.size)
        assertEquals(3, collectedPaths.size)

        collectedPaths.forEach({
            it.computeAndAssertBounds(expectedResult)
        })
    }

    private fun ScratchoffProcessor.computeAndAssertBounds(index: Int, expected: RectF) {
        queuedEvents[index].computeAndAssertBounds(expected)
    }

    private fun Path.computeAndAssertBounds(expected: RectF) {
        val bounds: RectF = RectF()

        this.computeBounds(bounds, true)

        assertEquals(expected, bounds)
    }
}