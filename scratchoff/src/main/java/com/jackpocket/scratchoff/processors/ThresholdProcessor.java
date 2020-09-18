package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.jackpocket.scratchoff.ViewHelper;
import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;

import java.lang.ref.WeakReference;
import java.util.List;

public class ThresholdProcessor extends Processor implements ScratchoffProcessor.Delegate {

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
    private Paint markerPaint;

    private double lastPercentScratched = PERCENT_SCRATCHED_UNTOUCHED;

    private final double completionThreshold;
    private boolean thresholdReached = false;

    private final ScratchPathManager pathManager = new ScratchPathManager();

    @SuppressWarnings("WeakerAccess")
    public ThresholdProcessor(int touchRadiusPx, double completionThreshold, Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
        this.completionThreshold = completionThreshold;

        this.markerPaint = ViewHelper.createBaseScratchoffPaint(touchRadiusPx);
        this.markerPaint.setColor(MARKER_SCRATCHED);
    }

    @Override
    public void start() {
        this.thresholdReached = false;

        safelyReleaseCurrentBitmap();

        super.start();
    }

    @Override
    public void postNewScratchedMotionEvents(List<ScratchPathPoint> events) {
        synchronized (pathManager) {
            if (currentBitmap == null)
                return;

            pathManager.addMotionEvents(events);
            pathManager.draw(canvas, markerPaint);
        }
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id) && currentBitmap == null) {
            Thread.sleep(SLEEP_DELAY_START);

            prepareCanvas();
        }

        while (isActive(id)) {
            synchronized (pathManager) {
                processImage();
            }

            Thread.sleep(SLEEP_DELAY_RUNNING);
        }
    }

    protected void prepareCanvas() {
        Delegate delegate = this.delegate.get();

        if (delegate == null)
            return;

        int[] layoutSize = delegate.getScratchableLayoutSize();

        if (layoutSize[0] < 1 || layoutSize[1] < 1)
            return;

        this.currentBitmap = Bitmap.createBitmap(layoutSize[0], layoutSize[1], Bitmap.Config.RGB_565);

        this.canvas = new Canvas(currentBitmap);
        this.canvas.drawColor(MARKER_UNTOUCHED);

        synchronized (pathManager) {
            pathManager.draw(canvas, markerPaint);
        }
    }

    protected void processImage() {
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

    protected void safelyReleaseCurrentBitmap() {
        try {
            synchronized (pathManager) {
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
