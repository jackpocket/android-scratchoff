package com.jackpocket.scratchoff.paths

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchPathQueueTests {

    private val events = listOf(
            ScratchPathPoint(0, 1f, 1f, MotionEvent.ACTION_DOWN),
            ScratchPathPoint(0, 2f, 2f, MotionEvent.ACTION_MOVE),
            ScratchPathPoint(0, 3f, 3f, MotionEvent.ACTION_MOVE),
            ScratchPathPoint(0, 4f, 4f, MotionEvent.ACTION_DOWN),
            ScratchPathPoint(0, 5f, 5f, MotionEvent.ACTION_MOVE)
    )

    @Test
    fun testEnqueueDequeue() {
        val queue = ScratchPathQueue()

        Assert.assertEquals(0, queue.size())

        queue.enqueue(events)

        Assert.assertEquals(5, queue.size())

        events.assertMatches(queue.dequeue())

        Assert.assertEquals(0, queue.size())
    }

    @Test
    fun testEnqueueCopy() {
        val queue = ScratchPathQueue()

        Assert.assertEquals(0, queue.size())

        queue.enqueue(events)

        Assert.assertEquals(5, queue.size())

        events.assertMatches(queue.copy())

        Assert.assertEquals(5, queue.size())
    }

    private fun List<ScratchPathPoint>.assertMatches(another: List<ScratchPathPoint>) {
        this.forEachIndexed({ index, item ->
            Assert.assertEquals(item, another[index])
        })
    }
}