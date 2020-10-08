package com.jackpocket.scratchoff.tools

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThresholdCalculatorTests {

    @Test
    fun testCalculatePercentScratchCompletedAlwaysBetweenZeroAndOne() {
        val calculator = ThresholdCalculator(0)
        val acceptedRange = 0.0..1.0

        assert(calculator.calculate(-10, 1, 1) in acceptedRange)
        assert(calculator.calculate(10, 1, 1) in acceptedRange)
    }

    @Test
    fun testCountColorMatches() {
        val calculator = ThresholdCalculator(0)
        val subject = intArrayOf(0, 0, 1, 1, 1)

        Assert.assertEquals(2, calculator.countMatching(subject))
    }

    @Test
    fun testCalculatePercentageOfBitmapScratched() {
        val calculator = ThresholdCalculator(Color.WHITE)
        val subject = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(subject)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(0f, 0f, 5f, 5f, Paint().apply({ this.color = Color.BLACK }))

        Assert.assertEquals(25, calculator.countNotMatching(subject))
        Assert.assertEquals(0.25f, calculator.calculate(subject))
    }
}