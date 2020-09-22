package com.jackpocket.scratchoff.paths

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScratchPathQueueTests {

    @Test
    fun testEnqueueDequeue() {
        val queue = ScratchPathQueue()

        Assert.assertEquals(0, queue.size())

        val events = listOf(
                ScratchPathPoint(0, 1f, 1f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 2f, 2f, MotionEvent.ACTION_MOVE),
                ScratchPathPoint(0, 3f, 3f, MotionEvent.ACTION_MOVE),
                ScratchPathPoint(0, 4f, 4f, MotionEvent.ACTION_DOWN),
                ScratchPathPoint(0, 5f, 5f, MotionEvent.ACTION_MOVE)
        )

        queue.enqueue(events)

        Assert.assertEquals(5, queue.size())

        val dequeued = queue.dequeue()

        Assert.assertEquals(0, queue.size())

        events.forEachIndexed({ index, item ->
            Assert.assertEquals(item, dequeued[index])
        })
    }
}