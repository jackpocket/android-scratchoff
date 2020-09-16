package com.jackpocket.scratchoff.test

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jackpocket.scratchoff.ScratchoffController
import com.jackpocket.scratchoff.processors.ThresholdProcessor
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MainActivity: AppCompatActivity(), ThresholdProcessor.ScratchValueChangedListener, View.OnTouchListener {

    private lateinit var controller: ScratchoffController
    private var scratchPercentTitleView = WeakReference<TextView>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        this.scratchPercentTitleView = WeakReference(findViewById(R.id.scratch_value_title))

        this.controller = ScratchoffController(this)
                .setThresholdPercent(.4)
                .setFadeOnClear(true)
                .setClearAnimationDuration(1, TimeUnit.SECONDS)
                .setClearAnimationInterpolator(LinearInterpolator())
                .setTouchRadiusDip(this, 25)
                .setCompletionCallback({
                    findViewById<ScratchableLinearLayout>(R.id.scratch_view)
                            .setBackgroundColor(-0xc36521)

                    Handler(Looper.getMainLooper())
                            .postDelayed({ controller.reset() }, 2000)
                })
                .setScratchValueChangedListener(this)
                .attach(findViewById(R.id.scratch_view), findViewById(R.id.scratch_view_behind))
    }

    override fun onResume() {
        super.onResume()

        this.controller.onResume()
        this.controller.addTouchObserver(this)
    }

    override fun onPause() {
        super.onPause()

        this.controller.onPause()
        this.controller.removeTouchObservers()
    }

    override fun onDestroy() {
        this.controller.onDestroy()

        super.onDestroy()
    }

    override fun onScratchPercentChanged(percentCompleted: Double) {
        scratchPercentTitleView.get()?.text = "Scratched ${percentCompleted * 100}%"
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> Log.d(TAG, "Observed ACTION_DOWN")
            MotionEvent.ACTION_UP -> Log.d(TAG, "Observed ACTION_UP")
        }

        // Our return is ignored here
        return false
    }

    companion object {

        val TAG = "ScratchoffTest"
    }
}