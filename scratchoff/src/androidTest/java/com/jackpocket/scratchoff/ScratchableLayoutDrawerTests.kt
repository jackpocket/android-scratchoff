package com.jackpocket.scratchoff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jackpocket.scratchoff.paths.ScratchPathManager
import com.jackpocket.scratchoff.paths.ScratchPathPoint
import com.jackpocket.scratchoff.tools.ThresholdCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchableLayoutDrawerTests {

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testSetsUpAndDrawsCorrectlyThenStopsDrawingAfterDestroy() {
        val result = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val resultCanvas = Canvas(result)

        val view = View(context)
        view.layout(0, 0, 10, 10)
        view.setBackgroundColor(Color.WHITE)

        val drawer = object: ScratchableLayoutDrawer(null) {
            override fun createClearPaint(touchRadiusPx: Int): Paint {
                return ScratchPathManager.createBaseScratchoffPaint(touchRadiusPx)
                        .apply({ this.color = Color.BLACK })
            }

            override fun enqueueViewInitializationOnGlobalLayout(scratchView: View?, behindView: View?) {
                // Pretend we did this
            }
        }
        drawer.attach(1, view, null)
        drawer.enqueueScratchMotionEvents(listOf(
                ScratchPathPoint(0, 0f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 0f, 10f, MotionEvent.ACTION_MOVE)
        ))
        drawer.initializeLaidOutScratchableView(view)
        drawer.draw(resultCanvas)

        assertEquals(0.1f, ThresholdCalculator(Color.WHITE).calculate(result))

        drawer.destroy()

        drawer.enqueueScratchMotionEvents(listOf(
                ScratchPathPoint(0, 10f, 0f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 10f, 10f, MotionEvent.ACTION_MOVE)
        ))
        drawer.draw(resultCanvas)

        assertEquals(0.1f, ThresholdCalculator(Color.WHITE).calculate(result))
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
}