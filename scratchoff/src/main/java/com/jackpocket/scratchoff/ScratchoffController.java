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
        public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted);
        public void onScratchThresholdReached(ScratchoffController controller);
    }

    private WeakReference<View> scratchableLayout = new WeakReference<View>(null);
    private ScratchableLayoutDrawer layoutDrawer;

    private ScratchoffProcessor processor;
    private WeakReference<Delegate> delegate = new WeakReference<Delegate>(null);

    private WeakReference<View> behindView = new WeakReference<View>(null);

    private int touchRadiusPx;
    private boolean thresholdReached = false;
    private double thresholdPercent;

    private int[] gridSize;

    private boolean clearOnThresholdReached;
    private boolean fadeOnClear;

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs;

    private boolean scratchableLayoutAvailable = false;

    private List<OnTouchListener> touchObservers = new ArrayList<OnTouchListener>();

    public ScratchoffController(Context context) {
        this(context, null);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchoffController(Context context, Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);

        Resources resources = context.getResources();

        this.touchRadiusPx = (int) resources.getDimension(R.dimen.scratch__touch_radius);
        this.thresholdPercent = resources.getInteger(R.integer.scratch__threshold_percent) / 100d;
        this.clearOnThresholdReached = resources.getBoolean(R.bool.scratch__clear_on_threshold_reached);
        this.clearAnimationDurationMs = resources.getInteger(R.integer.scratch__clear_animation_duration_ms);
        this.fadeOnClear = resources.getBoolean(R.bool.scratch__fade_on_clear);
    }

    /**
     * Attach the controller to the specified Views
     * @param scratchableLayout The View to scratch away. If not an instance of ScratchableLayout, you must handle the calls to ScratchoffController.draw(Canvas) manually.
     * @param behindView The View to be revealed
     */
    public ScratchoffController attach(View scratchableLayout, View behindView) {
        safelyStopProcessors();

        this.scratchableLayout = new WeakReference<View>(scratchableLayout);
        this.behindView = new WeakReference<View>(behindView);

        return reset();
    }

    /**
     * Reset the controller to its pre-scratched state. attach(View, View) must be called prior to resetting.
     */
    public ScratchoffController reset() {
        View layout = scratchableLayout.get();

        if (layout == null)
            throw new IllegalStateException("Cannot attach to a null View! Ensure you call attach(View, View) with valid Views!");

        safelyStopProcessors();

        this.layoutDrawer = new ScratchableLayoutDrawer()
                .setClearAnimationDurationMs(clearAnimationDurationMs)
                .setClearAnimationInterpolator(clearAnimationInterpolator)
                .attach(this, layout, behindView.get());

        layout.setOnTouchListener(this);

        this.processor = new ScratchoffProcessor(this);

        if (layout instanceof ScratchableLayout)
            ((ScratchableLayout) layout).initialize(this);

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
        processor.enqueue(events);

        return true;
    }

    protected void enqueueLayoutDrawerEvents(List<ScratchPathPoint> events) {
        ScratchableLayoutDrawer layoutDrawer = this.layoutDrawer;

        if (layoutDrawer != null)
            layoutDrawer.enqueueScratchMotionEvents(events);
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

        if (layoutDrawer != null)
            layoutDrawer.clear(fadeOnClear);

        safelyStopProcessors();

        return this;
    }

    public View getScratchImageLayout() {
        return scratchableLayout.get();
    }

    public ScratchoffController setThresholdPercent(double thresholdPercent) {
        this.thresholdPercent = thresholdPercent;

        return this;
    }

    public ScratchoffController setClearOnThresholdReached(boolean clearOnThresholdReached) {
        this.clearOnThresholdReached = clearOnThresholdReached;

        return this;
    }

    public ScratchoffController setFadeOnClear(boolean fadeOnClear) {
        this.fadeOnClear = fadeOnClear;

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

    public ScratchoffController setTouchRadiusPx(int touchRadius) {
        this.touchRadiusPx = touchRadius;

        return this;
    }

    public ScratchoffController setTouchRadiusDip(Context context, int touchRadius) {
        this.touchRadiusPx = (int) ((touchRadius * context.getResources().getDisplayMetrics().density) + 0.5f);

        return this;
    }

    /**
     * Set a callback to be triggered when the percentage of scratched area changes
     * and the scratch threshold has been reached.
     *
     * Callback values for scratch percentages are in the range [0.0, 100.0].
     *
     * You must maintain a reference to the supplied ScratchControllerDelegate.
     */
    public ScratchoffController setDelegate(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);

        return this;
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public int getTouchRadiusPx() {
        return touchRadiusPx;
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

        if (processor == null || processor.isActive())
            return;

        processor.start();
    }

    protected void safelyStopProcessors() {
        if (processor == null)
            return;

        processor.stop();
    }

    public ScratchableLayoutDrawer getLayoutDrawer(){
        return layoutDrawer;
    }

    /**
     * Add an OnTouchListener to observe MotionEvents as they are passed
     * into the ScratchoffController. Events will be forwarded regardless of
     * the ScratchoffController's enabled state, and all return values will be ignored.
     *
     * If adding observers (in Activity.onResume), you should also call
     * ScratchoffController.removeTouchObservers (in Activity.onPause).
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController addTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.add(touchListener);

        return this;
    }

    /**
     * Remove a OnTouchListener from the ScratchoffController.
     *
     * @param touchListener a non-null OnTouchListener
     */
    public ScratchoffController removeTouchObserver(OnTouchListener touchListener) {
        this.touchObservers.remove(touchListener);

        return this;
    }

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
