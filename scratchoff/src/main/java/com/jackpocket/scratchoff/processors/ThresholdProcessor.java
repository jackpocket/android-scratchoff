package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;
import com.jackpocket.scratchoff.tools.Sleeper;

import java.lang.ref.WeakReference;
import java.util.List;

public class ThresholdProcessor extends Processor implements ScratchoffProcessor.Delegate {

    public interface Delegate {
        public int[] getScratchableLayoutSize();
        public void postScratchPercentChanged(float percent);
        public void postScratchThresholdReached();
    }

    public enum Quality {
        LOW,
        MEDIUM,
        HIGH
    }

    private static final int SLEEP_DELAY_START = 100;

    private static final int MARKER_UNTOUCHED = 0xFFFFFFFF;
    private static final int MARKER_SCRATCHED = 0xFF000000;

    private static final int PERCENT_SCRATCHED_UNTOUCHED = -1;

    private WeakReference<Delegate> delegate;
    private Bitmap currentBitmap;

    private Canvas canvas;
    private Paint markerPaint;

    private double lastPercentScratched = PERCENT_SCRATCHED_UNTOUCHED;

    private final float completionThreshold;
    private boolean thresholdReached = false;

    private final ScratchPathManager pathManager = new ScratchPathManager();
    private final ScratchPathQueue queue = new ScratchPathQueue();

    private int originalTouchRadius;
    private Quality accuracyQuality;

    private final Sleeper sleeper = new Sleeper(15, 50, 3000);

    @SuppressWarnings("WeakerAccess")
    public ThresholdProcessor(
            int touchRadiusPx,
            float completionThreshold,
            Quality accuracyQuality,
            Delegate delegate) {

        this.originalTouchRadius = touchRadiusPx;
        this.completionThreshold = completionThreshold;
        this.accuracyQuality = accuracyQuality;
        this.delegate = new WeakReference<Delegate>(delegate);

        this.markerPaint = ScratchPathManager.createBaseScratchoffPaint(touchRadiusPx);
        this.markerPaint.setColor(MARKER_SCRATCHED);
        this.markerPaint.setAntiAlias(false);
    }

    @Override
    public void enqueueScratchMotionEvents(List<ScratchPathPoint> events) {
        queue.enqueue(events);
    }

    @Override
    protected void doInBackground(long id) throws Exception {
        while (isActive(id) && currentBitmap == null) {
            Thread.sleep(SLEEP_DELAY_START);

            prepareBitmapAndCanvasForDrawing();
        }

        boolean processedAnything = false;

        while (isActive(id)) {
            if (processedAnything && drawQueuedScratchMotionEvents()) {
                sleeper.sleep();

                continue;
            }

            processScratchedImagePercent();

            if (!isActive(id))
                return;

            processedAnything = true;

            sleeper.notifyTriggered();
            sleeper.sleep();
        }
    }

    protected void prepareBitmapAndCanvasForDrawing() {
        Delegate delegate = this.delegate.get();

        if (delegate == null)
            return;

        int[] layoutSize = delegate.getScratchableLayoutSize();

        if (layoutSize[0] < 1 || layoutSize[1] < 1)
            return;

        float accuracyQuality = constrainAccuracyQuality(
                this.originalTouchRadius,
                this.accuracyQuality);

        float width = layoutSize[0] * accuracyQuality;
        float aspectRatio = layoutSize[1] / (float) layoutSize[0];
        float height = width * aspectRatio;
        float touchRadius = originalTouchRadius * accuracyQuality;

        this.currentBitmap = Bitmap.createBitmap(
                (int) width,
                (int) height,
                Bitmap.Config.RGB_565);

        this.markerPaint.setStrokeWidth(touchRadius * 2);
        this.pathManager.setScale(accuracyQuality);

        this.canvas = new Canvas(currentBitmap);
        this.canvas.drawColor(MARKER_UNTOUCHED);

        // The continuous drawing of incomplete Path elements affects the
        // drawing at the pixel-level, and the result may differ from the single-redraw
        // performed when restoring the Path elements from the history.
        // Thus, we cannot expect the threshold calculated from a historical reload
        // to be exactly equal to the original, without emulating the original drawing
        // of the MotionEvents as they came in. Since that would be super-inefficient,
        // I have no more tears to give this problem, the ThresholdProcessor instances are
        // not re-used after resets, and the loss is limited to less than 0.001%,
        // we can just pretend that doesn't really happen and move on with our lives...
        pathManager.draw(canvas, markerPaint);
    }

    protected static float constrainAccuracyQuality(
            int touchRadius,
            Quality quality) {

        switch (quality) {
            case LOW:
                return constrainAccuracyQuality(touchRadius, 0.01f);
            case MEDIUM:
                return constrainAccuracyQuality(touchRadius, 0.5f);
            default:
                return 1.0f;
        }
    }

    protected static float constrainAccuracyQuality(
            int touchRadius,
            float accuracyQuality) {

        float minimumAccuracyQuality = 1 / (float) touchRadius;

        return Math.min(1f, Math.max(minimumAccuracyQuality, accuracyQuality));
    }

    protected boolean drawQueuedScratchMotionEvents() {
        List<ScratchPathPoint> dequeuedEvents = queue.dequeue();

        if (dequeuedEvents.size() < 1)
            return false;

        pathManager.addMotionEvents(dequeuedEvents);
        pathManager.draw(canvas, markerPaint);

        return true;
    }

    protected void processScratchedImagePercent() {
        Delegate delegate = this.delegate.get();
        Bitmap currentBitmap = this.currentBitmap;

        if (delegate == null || currentBitmap == null || thresholdReached)
            return;

        float percentScratched = calculatePercentScratched(currentBitmap);

        if (this.lastPercentScratched < percentScratched) {
            delegate.postScratchPercentChanged(percentScratched);
        }

        if (completionThreshold <= percentScratched) {
            this.thresholdReached = true;

            delegate.postScratchThresholdReached();
        }

        this.lastPercentScratched = percentScratched;
    }

    private float calculatePercentScratched(Bitmap bitmap) {
        return calculatePercentScratched(getScratchedCount(bitmap), bitmap.getWidth(), bitmap.getHeight());
    }

    public static float calculatePercentScratched(int scratchedCount, int width, int height) {
        return Math.min(1, Math.max(0, ((float) scratchedCount) / (width * height)));
    }

    private int getScratchedCount(Bitmap bitmap) {
        int pixelCount = bitmap.getWidth() * bitmap.getHeight();
        int[] pixels = new int[pixelCount];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        return pixelCount - countColorMatches(MARKER_UNTOUCHED, pixels);
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

        sleeper.reset();

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
