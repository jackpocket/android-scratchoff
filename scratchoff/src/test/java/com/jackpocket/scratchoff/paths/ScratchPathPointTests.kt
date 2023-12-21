package com.jackpocket.scratchoff.paths

import android.view.MotionEvent
import androidx.test.core.view.PointerCoordsBuilder
import androidx.test.core.view.PointerPropertiesBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.Mockito.`when` as WHEN
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class ScratchPathPointTests {

    @Test
    fun testMotionEventWithMultiplePointersMapsToScratchPathPoint() {
        val pointerCount = 10
        val expectedActions = listOf(
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_UP
        )

        val events = expectedActions
            .map({ action ->
                val properties = 0
                    .until(pointerCount)
                    .map({
                        PointerPropertiesBuilder
                            .newBuilder()
                            .setId(it)
                            .build()
                    })
                    .toTypedArray()

                val coordinates = 0
                    .until(pointerCount)
                    .map({
                        PointerCoordsBuilder
                            .newBuilder()
                            .setCoords(10f, 20f)
                            .build()
                    })
                    .toTypedArray()

                MotionEvent.obtain(0, 0, action, pointerCount, properties, coordinates, 0, 0, 0f, 0f, 0, 0, 0, 0)
            })
            .map(ScratchPathPoint::create)
            .flatten()

        events
            .chunked(pointerCount)
            .forEachIndexed({ actionIndex, groupedEvents ->
                groupedEvents.forEachIndexed({ index, event ->
                    val pointerIndex = index.rem(pointerCount)

                    assertEquals(pointerIndex, event.pointerIndex)
                    assertEquals(10f, event.x)
                    assertEquals(20f, event.y)
                    assertEquals(expectedActions[actionIndex], event.action)
                })
            })
    }

    @Test
    fun testMotionEventWithHistoricalDataInsertsScratchPathPointsBeforeLatest() {
        val event = mock<MotionEvent>()

        WHEN(event.actionMasked)
            .thenReturn(MotionEvent.ACTION_MOVE)

        WHEN(event.getX(any()))
            .thenReturn(1F)

        WHEN(event.getY(any()))
            .thenReturn(2F)

        WHEN(event.pointerCount)
            .thenReturn(1)

        WHEN(event.historySize)
            .thenReturn(2)

        WHEN(event.getHistoricalX(any(), any()))
            .thenReturn(3F)

        WHEN(event.getHistoricalY(any(), any()))
            .thenReturn(4F)

        val paths = ScratchPathPoint.create(event)

        assertEquals(3, paths.size)

        assertEquals(3F, paths[0].x)
        assertEquals(4F, paths[0].y)

        assertEquals(3F, paths[1].x)
        assertEquals(4F, paths[1].y)

        assertEquals(1F, paths[2].x)
        assertEquals(2F, paths[2].y)

        assertTrue(paths.all({ it.action == MotionEvent.ACTION_MOVE }))
    }

    @Test
    fun testEqualsWithNonScratchPathPointReturnsFalse() {
        val point = ScratchPathPoint(0, 1F, 2F, 0)

        assertFalse(point.equals(Object()))
    }

    @Test
    fun testEqualsReturnsTrueForMatchingPoints() {
        val point1 = ScratchPathPoint(0, 1F, 2F, 0)
        val point2 = ScratchPathPoint(0, 1F, 2F, 0)

        assertEquals(point1, point2)
    }

    @Test
    fun testEqualsReturnsFalseForPointsThatDoNotMatch() {
        val point1 = ScratchPathPoint(0, 1F, 2F, 0)
        val point2 = ScratchPathPoint(1, 2F, 1F, 0)

        assertNotEquals(point1, point2)
    }
}