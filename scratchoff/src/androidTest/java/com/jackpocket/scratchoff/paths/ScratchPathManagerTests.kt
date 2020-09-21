package com.jackpocket.scratchoff.paths

import android.graphics.Path
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchPathManagerTests {

    @Test
    fun testActionDownAlwaysCreatesNewPath() {
        val manager = ScratchPathManager();

        assertEquals(0, manager.paths.size)

        manager.handleTouchDown(0f, 0f)
        manager.handleTouchDown(0f, 0f)
        manager.handleTouchDown(0f, 0f)

        assertEquals(3, manager.paths.size)
    }

    @Test
    fun testActionMoveChangesActivePathAndDoesNotCreateNewPath() {
        val manager = ScratchPathManager();
        manager.handleTouchDown(0f, 0f)

        assertEquals(1, manager.paths.size)
        manager.paths[0].assertEmpty(true)

        manager.handleTouchMove(1f, 1f)

        assertEquals(1, manager.paths.size)
        manager.paths[0].assertEmpty(false)
    }

    private fun Path.assertEmpty(value: Boolean) {
        val pathBounds = RectF()

        this.computeBounds(pathBounds, true)

        assertEquals(value, pathBounds.isEmpty)
    }
}