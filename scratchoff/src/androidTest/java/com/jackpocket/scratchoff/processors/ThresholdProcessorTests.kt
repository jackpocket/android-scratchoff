package com.jackpocket.scratchoff.processors

import android.graphics.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThresholdProcessorTests {

    @Test
    fun testThresholdProcessorMatchesFromHistoryLoad() {
        var loops: Int = 0
        var scratchPercent: Double = 0.0

        val processor = object: ThresholdProcessor(5, 1.0, object: Delegate {
            override fun postScratchThresholdReached() { }

            override fun postScratchPercentChanged(percent: Double) {
                scratchPercent = percent
            }

            override fun getScratchableLayoutSize(): IntArray {
                return intArrayOf(10, 10)
            }
        }) {
            override fun isActive(id: Long): Boolean {
                loops += 1

                return loops == 1
            }
        }

        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(0f, 10f)

        processor.addPaths(listOf(path))
        processor.run()

        assertEquals(0.5, scratchPercent, 0.001)
    }
}