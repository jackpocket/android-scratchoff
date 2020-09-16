package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.jackpocket.scratchoff.ScratchoffController;

import java.util.List;

public class ThresholdProcessor extends Processor {

    public interface ScratchValueChangedListener {
        public void onScratchPercentChanged(double percentCompleted);
    }

    private static final int SLEEP_DELAY_RUNNING = 50;
    private static final int SLEEP_DELAY_START = 100;

    private static final int MARKER_UNTOUCHED = 0xFFFFFFFF;
    private static final int MARKER_SCRATCHED = 0xFF000000;

    private ScratchoffController controller;

    private Bitmap currentBitmap;

    private Canvas canvas;
    private Paint markerPaint = new Paint();

    private ScratchValueChangedListener valueChangedListener;
    private double lastPercentScratched = -1;
    private boolean thresholdReached = false;

    private final Boolean lock = true;

    @SuppressWarnings("WeakerAccess")
    public ThresholdProcessor(ScratchoffController controller) {
        this.controller = controller;

        this.markerPaint.setAntiAlias(true);
        this.markerPaint.setStyle(Paint.Style.STROKE);
        this.markerPaint.setStrokeCap(Paint.Cap.ROUND);
        this.markerPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @SuppressWarnings("WeakerAccess")
    public ThresholdProcessor setScratchValueChangedListener(ScratchValueChangedListener valueChangedListener) {
        this.valueChangedListener = valueChangedListener;

        return this;
    }

    @Override
    public void start() {
        this.markerPaint.setStrokeWidth(controller.getTouchRadiusPx() * 2);
        this.thresholdReached = false;

        safelyReleaseCurrentBitmap();

        super.start();
    }

    @SuppressWarnings("WeakerAccess")
    public void addPaths(List<Path> paths) {
        synchronized (lock) {
            if (currentBitmap == null)
                return;

            for (Path path : paths)
                canvas.drawPath(path, markerPaint);
        }
    }

    @Override
    protected void doInBackground() throws Exception {
        Thread.sleep(SLEEP_DELAY_START);

        if(controller.isProcessingAllowed())
            prepareCanvas();

        while(isActive() && controller.isProcessingAllowed()){
            processImage();

            Thread.sleep(SLEEP_DELAY_RUNNING);
        }

        safelyReleaseCurrentBitmap();
    }

    private void prepareCanvas() {
        this.currentBitmap = Bitmap.createBitmap(controller.getLayoutDrawer()
                .getPathStrippedImage());

        this.canvas = new Canvas(currentBitmap);
        this.canvas.drawColor(MARKER_UNTOUCHED);
    }

    private void processImage() {
        if (thresholdReached)
            return;

        double percentScratched = Math.min(1, ((double) getScratchedCount(currentBitmap)) / (currentBitmap.getWidth() * currentBitmap.getHeight()));

        if (percentScratched != this.lastPercentScratched) {
            postScratchValueChanged(percentScratched);
        }

        if (controller.getThresholdPercent() < percentScratched) {
            this.thresholdReached = true;

            postThresholdReached();
        }

        this.lastPercentScratched = percentScratched;
    }

    private int getScratchedCount(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int scratched = 0;

        for (int pixel : pixels) {
            if (pixel == MARKER_SCRATCHED)
                scratched++;
        }

        return scratched;
    }

    private void postThresholdReached() {
        controller.post(new Runnable() {
            public void run() {
                controller.onThresholdReached();
            }
        });
    }

    private void postScratchValueChanged(final double value) {
        final ScratchValueChangedListener valueChangedListener = this.valueChangedListener;

        if (valueChangedListener == null)
            return;

        controller.post(new Runnable() {
            public void run() {
                valueChangedListener.onScratchPercentChanged(value);
            }
        });
    }

    @Override
    public void cancel() {
        super.cancel();

        safelyReleaseCurrentBitmap();
    }

    private void safelyReleaseCurrentBitmap() {
        try{
            synchronized (lock) {
                if (currentBitmap == null)
                    return;

                currentBitmap.recycle();
                currentBitmap = null;

                canvas = null;
            }
        }
        catch(Exception e){ e.printStackTrace(); }
    }
}
