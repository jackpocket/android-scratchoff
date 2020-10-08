package com.jackpocket.scratchoff.tools;

import android.graphics.Bitmap;

public class ThresholdCalculator {

    private int unscratchedColor;

    public ThresholdCalculator(int unscratchedColor) {
        this.unscratchedColor = unscratchedColor;
    }

    public float calculate(Bitmap bitmap) {
        return calculate(
                countNotMatching(bitmap),
                bitmap.getWidth(),
                bitmap.getHeight());
    }

    public float calculate(int scratchedCount, int width, int height) {
        return Math.min(1, Math.max(0, ((float) scratchedCount) / (width * height)));
    }

    public int countNotMatching(Bitmap bitmap) {
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];

        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return pixelCount - countMatching(pixels);
    }

    int countMatching(int[] pixels) {
        int scratched = 0;

        for (int pixel : pixels) {
            if (pixel == unscratchedColor)
                scratched++;
        }

        return scratched;
    }
}
