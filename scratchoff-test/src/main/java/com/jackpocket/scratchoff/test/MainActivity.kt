package com.jackpocket.scratchoff.test

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jackpocket.scratchoff.ScratchoffController
import com.jackpocket.scratchoff.processors.ThresholdProcessor
import com.jackpocket.scratchoff.views.ScratchableLinearLayout
import java.lang.ref.WeakReference

class MainActivity: AppCompatActivity(), ThresholdProcessor.ScratchValueChangedListener {

    private lateinit var controller: ScratchoffController
    private var scratchPercentTitleView = WeakReference<TextView>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        this.scratchPercentTitleView = WeakReference(findViewById(R.id.scratch_value_title))

        this.controller = ScratchoffController(this)
                .setThresholdPercent(.4)
                .setFadeOnClear(true)
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
    }

    override fun onPause() {
        super.onPause()

        this.controller.onPause()
    }

    override fun onDestroy() {
        this.controller.onDestroy()

        super.onDestroy()
    }

    override fun onScratchPercentChanged(percentCompleted: Double) {
        scratchPercentTitleView.get()?.text = "Scratched ${percentCompleted * 100}%"
    }

    companion object {

        val TAG = "ScratchoffTest"
    }
}