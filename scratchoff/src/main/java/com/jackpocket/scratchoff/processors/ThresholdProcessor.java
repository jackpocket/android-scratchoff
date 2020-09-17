package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ThresholdProcessor extends Processor {

    public interface ScratchValueChangedListener {
        public void onScratchPercentChanged(double percentCompleted);
    }

    public interface Delegate {
        public int[] getScratchableLayoutSize();
        public void postScratchPercentChanged(double percent);
        public void postScratchThresholdReached();
    }

    private static final int SLEEP_DELAY_RUNNING = 50;
    private static final int SLEEP_DELAY_START = 100;

    private static final int MARKER_UNTOUCHED = 0xFFFFFFFF;
    private static final int MARKER_SCRATCHED = 0xFF000000;

    private static final int PERCENT_SCRATCHED_UNTOUCHED = -1;

    private WeakReference<Delegate> delegate;
    private Bitmap currentBitmap;

    private Canvas canvas;
    private Paint markerPaint = new Paint();

    private double lastPercentScratched = PERCENT_SCRATCHED_UNTOUCHED;

    private final double completionThreshold;
    private boolean thresholdReached = false;

    private final Boolean evaluatorLock = true;

    private final ArrayList<Path> pathHistory = new ArrayList<Path>();

    @SuppressWarnings("WeakerAccess")
    public ThresholdProcessor(int touchRadiusPx, double completionThreshold, Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
        this.completionThreshold = completionThreshold;

        this.markerPaint.setAntiAlias(true);
        this.markerPaint.setStyle(Paint.Style.STROKE);
        this.markerPaint.setStrokeCap(Paint.Cap.ROUND);
        this.markerPaint.setStrokeJoin(Paint.Join.ROUND);
        this.markerPaint.setStrokeWidth(touchRadiusPx * 2);
    }

    @Override
    public void start() {
        this.thresholdReached = false;

        safelyReleaseCurrentBitmap();

        super.start();
    }

    @SuppressWarnings("WeakerAccess")
    public void addPaths(List<Path> paths) {
        synchronized (evaluatorLock) {
            pathHistory.addAll(paths);

            if (currentBitmap == null)
                return;

            for (Path path : paths)
                canvas.drawPath(path, markerPaint);
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id) && currentBitmap == null) {
            Thread.sleep(SLEEP_DELAY_START);

            prepareCanvas();
        }

        while (isActive(id)) {
            synchronized (evaluatorLock) {
                processImage();
            }

            Thread.sleep(SLEEP_DELAY_RUNNING);
        }
    }

    private void prepareCanvas() {
        Delegate delegate = this.delegate.get();

        if (delegate == null)
            return;

        int[] layoutSize = delegate.getScratchableLayoutSize();

        if (layoutSize[0] < 1 || layoutSize[1] < 1)
            return;

        this.currentBitmap = Bitmap.createBitmap(layoutSize[0], layoutSize[1], Bitmap.Config.ARGB_8888);

        this.canvas = new Canvas(currentBitmap);
        this.canvas.drawColor(MARKER_UNTOUCHED);

        synchronized (evaluatorLock) {
            for (Path path : pathHistory)
                canvas.drawPath(path, markerPaint);
        }
    }

    private void processImage() {
        Delegate delegate = this.delegate.get();
        Bitmap currentBitmap = this.currentBitmap;

        if (delegate == null || currentBitmap == null || thresholdReached)
            return;

        double percentScratched = calculatePercentScratched(currentBitmap);

        if (percentScratched != this.lastPercentScratched) {
            delegate.postScratchPercentChanged(percentScratched);
        }

        if (completionThreshold < percentScratched) {
            this.thresholdReached = true;

            delegate.postScratchThresholdReached();
        }

        this.lastPercentScratched = percentScratched;
    }

    private double calculatePercentScratched(Bitmap bitmap) {
        return calculatePercentScratched(getScratchedCount(bitmap), bitmap.getWidth(), bitmap.getHeight());
    }

    public static double calculatePercentScratched(int scratchedCount, int width, int height) {
        return Math.min(1, Math.max(0, ((double) scratchedCount) / (width * height)));
    }

    private int getScratchedCount(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return countColorMatches(MARKER_SCRATCHED, pixels);
    }

    static int countColorMatches(int color, int[] pixels) {
        int scratched = 0;

        for (int pixel : pixels) {
            if (pixel == color)
                scratched++;
        }

        return scratched;
    }

    @Override
    public void stop() {
        super.stop();

        safelyReleaseCurrentBitmap();
    }

    private void safelyReleaseCurrentBitmap() {
        try {
            synchronized (evaluatorLock) {
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
