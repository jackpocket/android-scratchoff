package com.jackpocket.scratchoff

import android.content.Context
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalStateException

@RunWith(AndroidJUnit4::class)
class ScratchoffControllerTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    @Test(expected = IllegalStateException::class)
    fun testThrowsIllegalStateExceptionOnResetWithoutView() {
        val controller = ScratchoffController(context)
        controller.reset()
    }

    @Test
    fun testStopsProcessorsAndSetsViewsOnAttach() {
        val mockBehindView = View(context)
        val mockScratchableView = View(context)
        var stopCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun reset(): ScratchoffController {
                return this
            }

            override fun safelyStopProcessors() {
                stopCount += 1
            }
        }

        controller.attach(mockScratchableView, mockBehindView)

        assertEquals(1, stopCount)
        assertEquals(mockBehindView, controller.viewBehind)
        assertEquals(mockScratchableView, controller.scratchImageLayout)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithoutClearingEnabled() {
        var completionCount: Int = 0
        var clearCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setClearOnThresholdReached(false)
        controller.setCompletionCallback({
            completionCount += 1
        })
        controller.onThresholdReached()

        assertEquals(1, completionCount)
        assertEquals(0, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithClearingEnabled() {
        var completionCount: Int = 0
        var clearCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setClearOnThresholdReached(true)
        controller.setCompletionCallback({
            completionCount += 1
        })
        controller.onThresholdReached()

        assertEquals(1, completionCount)
        assertEquals(1, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testClearStopsProcessors() {
        var stopCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun safelyStopProcessors() {
                stopCount += 1
            }
        }

        controller.clear()

        assertEquals(1, stopCount)
        assert(controller.isScratchableLayoutAvailable.not())
    }

    @Test
    fun testPauseCallsStopProcessors() {
        var stopCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun safelyStopProcessors() {
                stopCount += 1
            }
        }
        controller.onPause()

        assertEquals(1, stopCount)
    }

    @Test
    fun testDestroyCallsStopProcessors() {
        var stopCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun safelyStopProcessors() {
                stopCount += 1
            }
        }
        controller.onDestroy()

        assertEquals(1, stopCount)
    }

    @Test
    fun testResumeCallsStartProcessors() {
        var startCount: Int = 0

        val controller = object: ScratchoffController(context) {
            override fun safelyStartProcessors() {
                startCount += 1
            }
        }
        controller.onResume()

        assertEquals(1, startCount)
    }
}