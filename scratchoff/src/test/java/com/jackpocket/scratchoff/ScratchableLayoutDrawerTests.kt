package com.jackpocket.scratchoff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jackpocket.scratchoff.paths.ScratchPathManager
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.tools.ThresholdCalculator
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScratchableLayoutDrawerTests {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy() {
        testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy(
            usePreDrawListener = false,
            keepListeningUntilLaidOut = false
        )
    }

    @Test
    fun testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroyPreDraw() {
        testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy(
            usePreDrawListener = true,
            keepListeningUntilLaidOut = false
        )
    }

    @Test
    fun testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroyPreDrawAndKeepListening() {
        testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy(
            usePreDrawListener = true,
            keepListeningUntilLaidOut = true
        )
    }

    private fun testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy(
        usePreDrawListener: Boolean,
        keepListeningUntilLaidOut: Boolean
    ) {

        val result = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)
        val fullSizeRegion = ThresholdCalculator.createFullSizeThresholdRegion(result)

        val view = View(context)
        view.setBackgroundColor(Color.WHITE)

        if (keepListeningUntilLaidOut) {
            view.layout(0, 0, 0, 10)
        }
        else {
            view.layout(0, 0, 10, 10)
        }

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun createClearPaint(touchRadiusPx: Int): Paint {
                return ScratchPathManager
                    .createBaseScratchoffPaint(touchRadiusPx)
                    .apply({ this.color = Color.BLACK })
            }
        }
        drawer.setUsePreDrawOverGlobalLayoutEnabled(usePreDrawListener)
        drawer.setKeepListeningForDrawUntilValidSizeDiscovered(keepListeningUntilLaidOut)
        drawer.attach(1, view, null)

        if (usePreDrawListener) {
            view.viewTreeObserver.dispatchOnPreDraw()
        }
        else {
            view.viewTreeObserver.dispatchOnGlobalLayout()
        }

        // We can remove this to confirm the tests fail as the
        // view will never have laid out with a valid size
        if (keepListeningUntilLaidOut) {
            view.layout(0, 0, 10, 10)

            view.viewTreeObserver.dispatchOnPreDraw()
        }

        drawer.addScratchPathPoints(
            listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 10f, MotionEvent.ACTION_MOVE)
            )
        )
        drawer.draw(resultCanvas)

        assertEquals(0.1f, ThresholdCalculator(Color.WHITE).calculate(result, fullSizeRegion))

        drawer.destroy()

        drawer.addScratchPathPoints(
            listOf(
                ScratchPathPoint(0, 10f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 10f, 10f, MotionEvent.ACTION_MOVE)
            )
        )
        drawer.draw(resultCanvas)

        assertEquals(0.1f, ThresholdCalculator(Color.WHITE).calculate(result, fullSizeRegion))
    }

    @Test
    fun testRemovesGlobalLayoutInitListenerOnDestroy() {
        testRemovesInitListenerOnDestroy(
            usePreDrawListener = false
        )
    }

    @Test
    fun testRemovesPreDrawInitListenerOnDestroy() {
        testRemovesInitListenerOnDestroy(
            usePreDrawListener = true
        )
    }

    private fun testRemovesInitListenerOnDestroy(
        usePreDrawListener: Boolean
    ) {

        val result = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)
        val fullSizeRegion = ThresholdCalculator.createFullSizeThresholdRegion(result)

        val view = View(context)
        view.setBackgroundColor(Color.WHITE)
        view.layout(0, 0, 10, 10)

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun createClearPaint(touchRadiusPx: Int): Paint {
                return ScratchPathManager
                    .createBaseScratchoffPaint(touchRadiusPx)
                    .apply({ this.color = Color.BLACK })
            }
        }
        drawer.setUsePreDrawOverGlobalLayoutEnabled(usePreDrawListener)
        drawer.attach(1, view, null)
        drawer.destroy()

        if (usePreDrawListener) {
            view.viewTreeObserver.dispatchOnPreDraw()
        }
        else {
            view.viewTreeObserver.dispatchOnGlobalLayout()
        }

        drawer.addScratchPathPoints(
            listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 10f, MotionEvent.ACTION_MOVE)
            )
        )
        drawer.draw(resultCanvas)

        assertEquals(1.0f, ThresholdCalculator(Color.WHITE).calculate(result, fullSizeRegion))
    }

    @Test
    fun testClearWithAnimationDisabledCallsHideAndMarkCleared() {
        var hideAndMarkClearedCount: Int = 0

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun hideAndMarkScratchableSurfaceViewCleared() {
                hideAndMarkClearedCount += 1

                super.hideAndMarkScratchableSurfaceViewCleared()
            }
        }
        drawer.clear(false)

        assertEquals(1, hideAndMarkClearedCount)
    }

    @Test
    fun testClearWithAnimationEnabledCallsFadeOutFlow() {
        var fadeOutCount: Int = 0

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun performFadeOutClear() {
                fadeOutCount += 1

                super.performFadeOutClear()
            }
        }
        drawer.clear(true)

        assertEquals(1, fadeOutCount)
    }

    @Test
    fun testClearAnimationCompletedCallsHideAndMarkClearedWhenTagMatches() {
        val view = ScratchableLinearLayout(context)
        var hideAndMarkClearedCount: Int = 0

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun enqueueViewInitializationOnGlobalLayout(
                scratchView: View?,
                behindView: View?,
            ) {
                // Pretend this happened
            }

            override fun hideAndMarkScratchableSurfaceViewCleared() {
                hideAndMarkClearedCount += 1

                super.hideAndMarkScratchableSurfaceViewCleared()
            }
        }
        drawer.attach(1, view, null)
        drawer.clear(true)

        drawer.claimClearAnimation(view, 1)

        view.setTag(R.id.scratch__clear_animation_tag, 0)

        drawer.onAnimationEnd(null)
        assertEquals(0, hideAndMarkClearedCount)

        drawer.claimClearAnimation(view, 0)
        drawer.onAnimationEnd(null)

        assertEquals(1, hideAndMarkClearedCount)
    }

    @Test
    fun testLayoutParamMatchesWidthAndHeightOnGlobalLayout() {
        val behindView = View(context)
        behindView.layout(0, 0, 10, 20)

        val scratchView = ScratchableLinearLayout(context)
        scratchView.layoutParams = ViewGroup.LayoutParams(1, 1)

        val drawer = ScratchableLayoutDrawer(null)
        drawer.performLayoutDimensionMatching(scratchView, behindView)

        assertEquals(10, scratchView.layoutParams.width)
        assertEquals(20, scratchView.layoutParams.height)
    }

    @Test
    fun testTriggerOrPostRunnableOnLaidOutTriggersImmediatelyWhenAttemptPostDisabledAndWidthHeightZero() {
        val delegate = mock<() -> Unit>()

        val view = View(context)
        view.layout(0, 0, 0, 0)

        val drawer = ScratchableLayoutDrawer(null)
        drawer.triggerOrPostRunnableOnLaidOut(delegate, false)

        verify(delegate, times(1))
            .invoke()
    }

    @Test
    fun testTriggerOrPostRunnableOnLaidOutTriggersImmediatelyWhenAttemptPostEnabledAndWidthHeightNotZero() {
        val delegate = mock<() -> Unit>()

        val view = View(context)
        view.layout(0, 0, 1, 1)

        val drawer = ScratchableLayoutDrawer(null)
        drawer.setAttemptLastDitchPostForLayoutResolutionFailure(true)
        drawer.triggerOrPostRunnableOnLaidOut(delegate, true)

        verify(delegate, times(1))
            .invoke()
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    fun testTriggerOrPostRunnableOnLaidOutTriggersDelayedWhenAttemptPostEnabledAndWidthHeightZero() {
        val delegate = mock<() -> Unit>()

        val view = View(context)
        view.layout(0, 0, 0, 0)

        val drawer = ScratchableLayoutDrawer(null)
        drawer.setAttemptLastDitchPostForLayoutResolutionFailure(true)
        drawer.triggerOrPostRunnableOnLaidOut(delegate, false)

        verify(delegate, never())
            .invoke()

        ShadowLooper.runUiThreadTasks()

        verify(delegate, times(1))
            .invoke()
    }
}