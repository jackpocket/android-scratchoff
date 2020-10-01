package com.jackpocket.scratchoff;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.processors.InvalidationProcessor;
import com.jackpocket.scratchoff.processors.ScratchoffProcessor;
import com.jackpocket.scratchoff.processors.ThresholdProcessor;
import com.jackpocket.scratchoff.views.ScratchableLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScratchoffController implements OnTouchListener,
        ScratchableLayoutDrawer.Delegate,
        ThresholdProcessor.Delegate,
        InvalidationProcessor.Delegate {

    public interface Delegate {

        /**
         * Callback values for scratch percentages are in the range [0.0, 100.0] and will be
         * continuously called on the main Thread as the threshold value changes.
         * <br><br>
         * Updates will cease once the threshold has been reached.
         */
        public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted);

        /**
         * Called once the scratch threshold has been reached.
         */
        public void onScratchThresholdReached(ScratchoffController controller);
    }

    private WeakReference<View> scratchableLayout = new WeakReference<View>(null);
    private ScratchableLayoutDrawer layoutDrawer;

    private ScratchoffProcessor processor;
    private WeakReference<Delegate> delegate = new WeakReference<Delegate>(null);

    private WeakReference<View> behindView = new WeakReference<View>(null);

    private int touchRadiusPx;
    private ThresholdProcessor.Quality thresholdAccuracyQuality = ThresholdProcessor.Quality.HIGH;
    private boolean thresholdReached = false;
    private float thresholdPercent;

    private int[] gridSize;

    private boolean clearOnThresholdReached;
    private boolean clearAnimationEnabled;

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs;

    private boolean scratchableLayoutAvailable = false;

    private List<OnTouchListener> touchObservers = new ArrayList<OnTouchListener>();

    /**
     * Create a {@link ScratchoffController} instance.
     * <br><br>
     * You must manually call {@link #setDelegate(Delegate)} to receive scratch threshold updates and
     * completion events, or instantiate with {@link #ScratchoffController(Context,Delegate)}.
     */
    public ScratchoffController(Context context) {
        this(context, null);
    }

    /**
     * Create a {@link ScratchoffController} instance.
     * <br><br>
     * You must maintain a reference to the supplied {@link Delegate} as it will be weakly held.
     */
    @SuppressWarnings("WeakerAccess")
    public ScratchoffController(Context context, Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);

        Resources resources = context.getResources();

        this.touchRadiusPx = (int) resources.getDimension(R.dimen.scratch__touch_radius);
        this.thresholdPercent = resources.getInteger(R.integer.scratch__threshold_percent) / 100f;
        this.clearOnThresholdReached = resources.getBoolean(R.bool.scratch__clear_on_threshold_reached);
        this.clearAnimationDurationMs = resources.getInteger(R.integer.scratch__clear_animation_duration_ms);
        this.clearAnimationEnabled = resources.getBoolean(R.bool.scratch__clear_animation_enabled);
    }

    /**
     * Set a callback to be triggered when the percentage of scratched area changes
     * and the scratch threshold has been reached.<br>
     * <br><br>
     * Callback values for scratch percentages are in the range [0.0, 100.0].
     * <br><br>
     * You must maintain a reference to the supplied {@link Delegate} as it will be weakly held.
     */
    public ScratchoffController setDelegate(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);

        return this;
    }

    /**
     * Attach the controller to the specified Views
     *
     * @param scratchableLayout The View to scratch away. If this View is not an instance of {@link ScratchableLayout}, you must handle the calls to {@link #draw(Canvas)} manually.
     * @param behindView The View to be revealed
     */
    public ScratchoffController attach(View scratchableLayout, View behindView) {
        safelyStopProcessors();

        this.scratchableLayout = new WeakReference<View>(scratchableLayout);
        this.behindView = new WeakReference<View>(behindView);

        return reset();
    }

    /**
     * Reset the controller to its pre-scratched state.
     * <br><br>
     * Note: {@link #attach(View, View)) must have been called at least once before resetting.
     */
    public ScratchoffController reset() {
        View scratchableLayout = this.scratchableLayout.get();
        View behindView = this.behindView.get();

        if (scratchableLayout == null || behindView == null)
            throw new IllegalStateException("Cannot attach to a null View! Ensure you call attach(View, View) with valid Views!");

        safelyStopProcessors();

        this.layoutDrawer = new ScratchableLayoutDrawer()
                .setClearAnimationDurationMs(clearAnimationDurationMs)
                .setClearAnimationInterpolator(clearAnimationInterpolator)
                .attach(this, scratchableLayout, behindView);

        scratchableLayout.setOnTouchListener(this);

        this.processor = new ScratchoffProcessor(this);

        if (scratchableLayout instanceof ScratchableLayout)
            ((ScratchableLayout) scratchableLayout).initialize(this);

        return this;
    }

    @Override
    public void onScratchableLayoutAvailable(int width, int height) {
        this.gridSize = new int[] { width, height };

        this.scratchableLayoutAvailable = true;
        this.thresholdReached = false;

        safelyStartProcessors();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View view, MotionEvent event) {
        for (OnTouchListener observer : touchObservers) {
            observer.onTouch(view, event);
        }

        if (!scratchableLayoutAvailable)
            return false;

        List<ScratchPathPoint> events = ScratchPathPoint.create(event);

        enqueueLayoutDrawerEvents(events);
        enqueueProcessorEvents(events);

        return true;
    }

    protected void enqueueLayoutDrawerEvents(List<ScratchPathPoint> events) {
        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.enqueueScratchMotionEvents(events);
    }

    protected void enqueueProcessorEvents(List<ScratchPathPoint> events) {
        ScratchoffProcessor processor = this.processor;

        if (processor != null)
            processor.enqueue(events);
    }

    public void draw(Canvas canvas) {
        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.draw(canvas);
    }

    public ScratchoffController onPause() {
        safelyStopProcessors();

        return this;
    }

    public ScratchoffController onResume() {
        safelyStartProcessors();

        return this;
    }

    public ScratchoffController onDestroy() {
        safelyStopProcessors();
        removeTouchObservers();

        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.destroy();

        return this;
    }

    protected void onThresholdReached() {
        this.thresholdReached = true;

        if (clearOnThresholdReached)
            clear();

        Delegate delegate = this.delegate.get();

        if (delegate != null)
            delegate.onScratchThresholdReached(this);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchoffController clear() {
        this.scratchableLayoutAvailable = false;

        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.clear(clearAnimationEnabled);

        safelyStopProcessors();

        return this;
    }

    public View getScratchImageLayout() {
        return scratchableLayout.get();
    }

    public ScratchoffController setThresholdPercent(float thresholdPercent) {
        this.thresholdPercent = thresholdPercent;

        return this;
    }

    public ScratchoffController setClearOnThresholdReached(boolean clearOnThresholdReached) {
        this.clearOnThresholdReached = clearOnThresholdReached;

        return this;
    }

    public ScratchoffController setClearAnimationEnabled(boolean clearAnimationEnabled) {
        this.clearAnimationEnabled = clearAnimationEnabled;

        return this;
    }

    public ScratchoffController setClearAnimationDuration(long value, TimeUnit unit) {
        this.clearAnimationDurationMs = unit.toMillis(value);

        return this;
    }

    public ScratchoffController setClearAnimationInterpolator(Interpolator clearAnimationInterpolator) {
        this.clearAnimationInterpolator = clearAnimationInterpolator;

        return this;
    }

    /**
     * Set the radius, in DIP, of the circle to be scratched away on MotionEvents.
     * Must be greater than 0, or throws an IllegalStateException.
     */
    public ScratchoffController setTouchRadiusDip(Context context, int touchRadius) {
        return setTouchRadiusPx((int) ((touchRadius * context.getResources().getDisplayMetrics().density) + 0.5f));
    }

    /**
     * Set the radius, in pixels, of the circle to be scratched away on MotionEvents.
     * Must be greater than 0, or throws an IllegalStateException.
     */
    public ScratchoffController setTouchRadiusPx(int touchRadius) {
        if (touchRadius < 1)
            throw new IllegalArgumentException("touchRadius must be greater than 0");

        this.touchRadiusPx = touchRadius;

        return this;
    }

    public float getThresholdPercent() {
        return thresholdPercent;
    }

    public int getTouchRadiusPx() {
        return touchRadiusPx;
    }

    /**
     * Set the {@link ThresholdProcessor.Quality} for the underlying Threshold processor.
     * The default is {@link ThresholdProcessor.Quality#HIGH}, which implies no reduction in quality.
     * <br><br>
     * {@link ThresholdProcessor.Quality#MEDIUM} will attempt to reduce the quality to 50%, while
     * {@link ThresholdProcessor.Quality#LOW} will use the lowest-supported quality value at runtime
     * (1 / {@link #touchRadiusPx}).
     * <br><br>
     * This reduction is solely applied to elements of the {@link ThresholdProcessor}, and does not
     * affect the drawing quality in any way.
     * <br><br>
     * If the supplied quality value is below the runtime-calculated minimum (1 / touchRadius),
     * or above the maximum (1.0f), it will be ignored in favor of the minimum/maximum values.
     */
    public ScratchoffController setThresholdAccuracyQuality(ThresholdProcessor.Quality thresholdAccuracyQuality) {
        this.thresholdAccuracyQuality = thresholdAccuracyQuality;

        return this;
    }

    public ThresholdProcessor.Quality getThresholdAccuracyQuality() {
        return thresholdAccuracyQuality;
    }

    public boolean isThresholdReached() {
        return thresholdReached;
    }

    public View getViewBehind() {
        return behindView.get();
    }

    protected void safelyStartProcessors() {
        if (!scratchableLayoutAvailable)
            return;

        ScratchoffProcessor processor = this.processor;

        if (processor == null || processor.isActive())
            return;

        processor.start();
    }

    protected void safelyStopProcessors() {
        ScratchoffProcessor processor = this.processor;

        if (processor == null)
            return;

        processor.stop();
    }

    public ScratchableLayoutDrawer getLayoutDrawer(){
        return layoutDrawer;
    }

    /**
     * Add an OnTouchListener to observe MotionEvents as they are passed
     * into this ScratchoffController instance. Events will be forwarded regardless of
     * the ScratchoffController's enabled state, and all return values will be ignored.
     * <br><br>
     * If adding observers (in Activity.onResume), you should also call
     * {@link #removeTouchObservers} (in Activity.onPause).
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController addTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.add(touchListener);

        return this;
    }

    /**
     * Remove a OnTouchListener from this ScratchoffController instance.
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController removeTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.remove(touchListener);

        return this;
    }

    /**
     * Remove all touch observers from this ScratchoffController instance.
     */
    public void removeTouchObservers() {
        this.touchObservers.clear();
    }

    @Override
    public int[] getScratchableLayoutSize() {
        final int[] gridSize = this.gridSize;

        return gridSize == null
            ? new int[] { 0, 0 }
            : new int[] { gridSize[0], gridSize[1] };
    }

    public boolean isScratchableLayoutAvailable() {
        return scratchableLayoutAvailable;
    }

    @Override
    public void postScratchPercentChanged(final float percent) {
        final Delegate delegate = this.delegate.get();

        if (delegate == null)
            return;

        post(new Runnable() {
            public void run() {
                delegate.onScratchPercentChanged(ScratchoffController.this, percent);
            }
        });
    }

    @Override
    public void postScratchThresholdReached() {
        post(new Runnable() {
            public void run() {
                onThresholdReached();
            }
        });
    }

    @Override
    public void postInvalidateScratchableLayout() {
        getScratchImageLayout()
                .postInvalidate();
    }

    protected void post(Runnable runnable) {
        View layout = scratchableLayout.get();

        if (layout != null)
            layout.post(runnable);
    }
}
