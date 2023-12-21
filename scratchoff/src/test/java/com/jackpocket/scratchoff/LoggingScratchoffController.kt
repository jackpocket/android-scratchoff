package com.jackpocket.scratchoff

import android.graphics.Canvas
import android.view.View
import com.jackpocket.scratchoff.paths.ScratchPathPoint

class LoggingScratchoffController(view: View): ScratchoffController(view) {

    var drawCount: Int = 0
        private set

    override fun draw(canvas: Canvas?) {
        drawCount += 1
    }

    public override fun getClonedHistory(): MutableList<ScratchPathPoint> {
        return super.getClonedHistory()
    }
}