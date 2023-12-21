package com.jackpocket.scratchoff.views

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jackpocket.scratchoff.LoggingScratchoffController
import com.jackpocket.scratchoff.ScratchoffController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchableRelativeLayoutTests {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val layout = object: ScratchableRelativeLayout(context) {
        override fun createScratchoffController(): ScratchoffController {
            return LoggingScratchoffController(this)
        }
    }

    val controller = layout.scratchoffController as LoggingScratchoffController

    @Test
    fun testViewsDelegateOnDrawEvents() {
        assertEquals(0, controller.drawCount)

        layout.draw(Canvas())

        assertEquals(1, controller.drawCount)
    }

    @Test
    fun testViewSaveAndRestore() {
        controller
            .setStateRestorationEnabled(true)
            .attach()

        controller.onScratchableLayoutAvailable(10, 10)
        controller.onTouch(layout, MotionEvent.obtain(1, 2, MotionEvent.ACTION_MOVE, 1F, 2F, 0))

        val state = layout.onSaveInstanceState()

        val restoredLayout = object: ScratchableRelativeLayout(context) {
            override fun createScratchoffController(): ScratchoffController {
                return LoggingScratchoffController(this)
            }
        }
        restoredLayout.onRestoreInstanceState(state)
        restoredLayout.scratchoffController.onScratchableLayoutAvailable(10, 10)

        val restoredHistory = (restoredLayout.scratchoffController as LoggingScratchoffController).clonedHistory

        assertEquals(1, restoredHistory.size)
        assertEquals(1F, restoredHistory[0].x)
        assertEquals(2F, restoredHistory[0].y)
        assertEquals(MotionEvent.ACTION_MOVE, restoredHistory[0].action)
    }
}