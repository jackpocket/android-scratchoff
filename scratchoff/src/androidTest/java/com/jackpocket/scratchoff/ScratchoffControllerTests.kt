package com.jackpocket.scratchoff

import android.content.Context
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jackpocket.scratchoff.processors.ScratchoffProcessor
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchoffControllerTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    private val mockScratchableLayout: ScratchableLinearLayout by lazy {
        ScratchableLinearLayout(context)
    }

    private val loggingDelegate = LoggingThresholdChangedListener()

    @Test
    fun testStopsProcessorsInstantiatesRequirementsOnAttach() {
        var stopCount: Int = 0
        var createProcessorCount: Int = 0
        var createLayoutDrawerCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun safelyStopProcessors() {
                stopCount += 1
            }

            override fun createLayoutDrawer(): ScratchableLayoutDrawer {
                createLayoutDrawerCount += 1

                return super.createLayoutDrawer()
            }

            override fun createScratchoffProcessor(): ScratchoffProcessor {
                createProcessorCount += 1

                return super.createScratchoffProcessor()
            }
        }

        assertEquals(0, stopCount)
        assertEquals(0, createProcessorCount)
        assertEquals(0, createLayoutDrawerCount)

        controller.attach()

        assertEquals(1, stopCount)
        assertEquals(1, createProcessorCount)
        assertEquals(1, createLayoutDrawerCount)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithoutClearingEnabled() {
        var clearCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setThresholdChangedListener(loggingDelegate)
        controller.setClearOnThresholdReachedEnabled(false)
        controller.onThresholdReached()

        assertEquals(1, loggingDelegate.completions)
        assertEquals(0, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testThresholdReachedTriggersCompletionCallbackWithClearingEnabled() {
        var completionCount: Int = 0
        var clearCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun clear(): ScratchoffController {
                clearCount += 1

                return this
            }
        }
        controller.setThresholdChangedListener(loggingDelegate)
        controller.setClearOnThresholdReachedEnabled(true)
        controller.onThresholdReached()

        assertEquals(1, loggingDelegate.completions)
        assertEquals(1, clearCount)
        assert(controller.isThresholdReached)
    }

    @Test
    fun testClearStopsProcessors() {
        var stopCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
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

        val controller = object: ScratchoffController(mockScratchableLayout) {
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

        val controller = object: ScratchoffController(mockScratchableLayout) {
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

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun safelyStartProcessors() {
                startCount += 1
            }
        }
        controller.onResume()

        assertEquals(1, startCount)
    }

    private class LoggingThresholdChangedListener: ScratchoffController.ThresholdChangedListener {

        var threshold: Float = 0f
            private set

        var completions: Int = 0
            private set

        override fun onScratchPercentChanged(controller: ScratchoffController, percentCompleted: Float) {
            threshold = percentCompleted
        }

        override fun onScratchThresholdReached(controller: ScratchoffController) {
            completions += 1
        }
    }
}