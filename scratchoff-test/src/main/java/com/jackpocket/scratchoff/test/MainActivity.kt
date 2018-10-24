package com.jackpocket.scratchoff.test

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.jackpocket.scratchoff.ScratchoffController
import com.jackpocket.scratchoff.views.ScratchableLinearLayout

class MainActivity: AppCompatActivity() {

    private lateinit var controller: ScratchoffController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        this.controller = ScratchoffController(this)
                .setThresholdPercent(.3)
                .setFadeOnClear(true)
                .setTouchRadiusDip(this, 25)
                .setCompletionCallback({
                    findViewById<ScratchableLinearLayout>(R.id.scratch_view)
                            .setBackgroundColor(-0xc36521)

                    Handler(Looper.getMainLooper())
                            .postDelayed({ controller.reset() }, 2000)
                })
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

    companion object {

        val TAG = "ScratchoffTest"
    }
}