package com.jackpocket.scratchoff

import android.content.Context
import android.view.AbsSavedState
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.processors.ScratchoffProcessor
import com.jackpocket.scratchoff.views.ScratchableLayout
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import com.jackpocket.scratchoff.views.ScratchableRelativeLayout
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

    @Test
    fun testMotionEventsNotEnqueuedBeforeLayoutAvailable() {
        var enqueueProcessorsCount: Int = 0
        var enqueueDrawerCount: Int = 0

        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0)

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun enqueueLayoutDrawerEvents(events: MutableList<ScratchPathPoint>?) {
                enqueueDrawerCount += 1

                super.enqueueLayoutDrawerEvents(events)
            }

            override fun enqueueProcessorEvents(events: MutableList<ScratchPathPoint>?) {
                enqueueProcessorsCount += 1

                super.enqueueProcessorEvents(events)
            }
        }
        controller.onTouch(View(context), event)

        assertEquals(0, enqueueDrawerCount)
        assertEquals(0, enqueueProcessorsCount)

        controller.onScratchableLayoutAvailable(1, 1)
        controller.onTouch(View(context), event)

        assertEquals(1, enqueueDrawerCount)
        assertEquals(1, enqueueProcessorsCount)
    }

    @Test
    fun testMotionEventsEnqueuedWhenReady() {
        var enqueueProcessorsCount: Int = 0
        var enqueueDrawerCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun enqueueLayoutDrawerEvents(events: MutableList<ScratchPathPoint>?) {
                enqueueDrawerCount += 1
            }

            override fun enqueueProcessorEvents(events: MutableList<ScratchPathPoint>?) {
                enqueueProcessorsCount += 1
            }
        }
        controller.onScratchableLayoutAvailable(10, 20)
        controller.onTouch(View(context), MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 0f, 0f, 0))

        assertEquals(1, enqueueDrawerCount)
        assertEquals(1, enqueueProcessorsCount)
    }

    @Test
    fun testLayoutAvailableStartsProcessorsAndAttemptsRestore() {
        var startCount: Int = 0
        var restoreAttemptCount: Int = 0
        var restoreCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun safelyStartProcessors() {
                startCount += 1
            }

            override fun performStateRestoration() {
                restoreAttemptCount += 1

                super.performStateRestoration()
            }

            override fun performStateRestoration(state: ScratchoffState) {
                restoreCount += 1
            }
        }
        controller.restore(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        false,
                        listOf()))
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, startCount)
        assertEquals(1, restoreAttemptCount)
        assertEquals(1, restoreCount)
    }

    @Test
    fun testParcelizedDataCanBeRestored() {
        var restoredState: ScratchoffState? = null
        var expectedPoint = ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN)

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun performStateRestoration(state: ScratchoffState) {
                restoredState = state

                super.performStateRestoration(state)
            }
        }
        controller.restore(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        false,
                        listOf(expectedPoint)))
        controller.onScratchableLayoutAvailable(10, 20)

        val state = controller.parcelize(AbsSavedState.EMPTY_STATE)

        assertEquals(10, state.layoutSize[0])
        assertEquals(20, state.layoutSize[1])
        assertEquals(false, state.isThresholdReached)

        controller.clear()
        controller.restore(state)
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(10, restoredState!!.layoutSize[0])
        assertEquals(20, restoredState!!.layoutSize[1])
        assertEquals(false, restoredState!!.isThresholdReached)

        assertEquals(expectedPoint, controller.clonedHistory[0])
    }

    @Test
    fun testRestoreNotAttemptedWhenPendingStateRemoved() {
        var startCount: Int = 0
        var restoreAttemptCount: Int = 0
        var restoreCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun safelyStartProcessors() {
                startCount += 1
            }

            override fun performStateRestoration() {
                restoreAttemptCount += 1

                super.performStateRestoration()
            }

            override fun performStateRestoration(state: ScratchoffState) {
                restoreCount += 1
            }
        }
        controller.removePendingStateRestoration()
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, startCount)
        assertEquals(1, restoreAttemptCount)
        assertEquals(0, restoreCount)
    }

    @Test
    fun testRestoreWithThresholdReachedClearsWithoutAnimation() {
        var startCount: Int = 0
        var clearCount: Int = 0
        var animationEnabled: Boolean? = null

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun safelyStartProcessors() {
                startCount += 1
            }

            override fun clearLayoutDrawer(clearAnimationEnabled: Boolean) {
                clearCount += 1
                animationEnabled = clearAnimationEnabled
            }
        }
        controller.setClearAnimationEnabled(true)
        controller.restore(
                ScratchoffState(
                        AbsSavedState.EMPTY_STATE,
                        intArrayOf(10, 20),
                        true,
                        listOf()))
        controller.onScratchableLayoutAvailable(10, 20)

        assertEquals(1, startCount)
        assertEquals(1, clearCount)
        assertEquals(false, animationEnabled)
    }

    @Test
    fun testFindHelperFunctionSearchesParentView() {
        testFindHelperFunctionSearchesParentView(ScratchableLinearLayout(context))
        testFindHelperFunctionSearchesParentView(ScratchableRelativeLayout(context))
    }

    private fun testFindHelperFunctionSearchesParentView(layout: ScratchableLayout) {
        val view = layout as View
        view.id = 100

        val expectedController = layout.scratchoffController

        val parent = FrameLayout(context)
        parent.addView(View(context))
        parent.addView(View(context))
        parent.addView(view)

        assertEquals(expectedController, ScratchoffController.findByViewId(parent, 100))
    }

    @Test
    fun testTouchObserversAreAddedAndRemoved() {
        val observer1 = View.OnTouchListener({ _, _ -> true })
        val observer2 = View.OnTouchListener({ _, _ -> true })

        val controller = ScratchoffController(mockScratchableLayout)
        controller.addTouchObserver(observer1)
        controller.addTouchObserver(observer2)

        assertEquals(2, controller.touchObservers.size)

        controller.removeTouchObserver(observer2)

        assertEquals(1, controller.touchObservers.size)
        assertEquals(observer1, controller.touchObservers[0])

        controller.addTouchObserver(observer2)

        assertEquals(2, controller.touchObservers.size)

        controller.removeTouchObservers()

        assertEquals(0, controller.touchObservers.size)
    }

    @Test
    fun testDelegateCallbacksPostRunnable() {
        var postCount: Int = 0

        val controller = object: ScratchoffController(mockScratchableLayout) {
            override fun post(runnable: Runnable?) {
                postCount += 1
            }
        }

        controller.setThresholdChangedListener(loggingDelegate)
        controller.postScratchPercentChanged(0f)
        controller.postScratchThresholdReached()

        assertEquals(2, postCount)
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