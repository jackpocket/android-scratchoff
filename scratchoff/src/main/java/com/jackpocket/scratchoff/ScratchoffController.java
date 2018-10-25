package com.jackpocket.scratchoff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.jackpocket.scratchoff.processors.ScratchoffProcessor;
import com.jackpocket.scratchoff.processors.ThresholdProcessor;
import com.jackpocket.scratchoff.views.ScratchableLayout;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchoffController implements OnTouchListener, LayoutCallback {

    private WeakReference<View> scratchableLayout = new WeakReference<View>(null);
    private ScratchableLayoutDrawer layoutDrawer;

    private ScratchoffProcessor processor;
    private ThresholdProcessor.ScratchValueChangedListener scratchValueChangedListener;

    private Runnable completionCallback;

    private WeakReference<View> behindView = new WeakReference<View>(null);

    private int touchRadiusPx;
    private boolean thresholdReached = false;
    private double thresholdPercent = 0.65d;

    private int totalGridItemsCount;

    private boolean clearOnThresholdReached = true;
    private boolean fadeOnClear = true;

    private boolean enabled = true;

    private long lastTouchEvent = 0;

    public ScratchoffController(Context context) {
        this(context, null);
    }

    public ScratchoffController(Context context, Runnable completionCallback) {
        this.completionCallback = completionCallback;

        this.touchRadiusPx = (int) context.getResources()
                .getDimension(R.dimen.scratch__touch_radius);

        this.thresholdPercent = context.getResources()
                .getInteger(R.integer.scratch__threshold_percent) / 100d;

        this.clearOnThresholdReached = context.getResources()
                .getBoolean(R.bool.scratch__clear_on_threshold_reached);

        this.fadeOnClear = context.getResources()
                .getBoolean(R.bool.scratch__fade_on_clear);
    }

    /**
     * Attach the controller to the specified Views
     * @param scratchableLayout The View to scratch away. If not an instance of ScratchableLayout, you must handle the calls to ScratchoffController.draw(Canvas) manually.
     * @param behindView The View to be revealed
     */
    public ScratchoffController attach(View scratchableLayout, View behindView){
        safelyStopProcessors();

        this.scratchableLayout = new WeakReference<View>(scratchableLayout);
        this.behindView = new WeakReference<View>(behindView);

        return reset();
    }

    /**
     * Reset the controller to its pre-scratched state. attach(View, View) must be called prior to resetting.
     */
    public ScratchoffController reset(){
        View layout = scratchableLayout.get();

        if(layout == null)
            throw new IllegalStateException("Cannot attach to a null View! Ensure you call attach(View, View) with valid Views!");

        safelyStopProcessors();

        if(layout != null){
            layout.clearAnimation();
            layout.setVisibility(View.VISIBLE);
            layout.invalidate();

            this.layoutDrawer = new ScratchableLayoutDrawer()
                    .attach(this, layout, behindView.get());

            layout.setOnTouchListener(this);
        }

        this.processor = new ScratchoffProcessor(this);
        this.processor.setScratchValueChangedListener(scratchValueChangedListener);

        if(layout instanceof ScratchableLayout)
            ((ScratchableLayout) layout).initialize(this);

        return this;
    }

    @Override
    public void onScratchableLayoutAvailable(int width, int height) {
        this.totalGridItemsCount = width * height;

        this.enabled = true;
        this.thresholdReached = false;

        safelyStartProcessors();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if(!enabled)
            return false;

        processor.onReceiveMotionEvent(event, event.getAction() == MotionEvent.ACTION_DOWN);

        this.lastTouchEvent = System.currentTimeMillis();

        return true;
    }

    public void draw(Canvas canvas){
        if(layoutDrawer != null)
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

        if(layoutDrawer != null)
            layoutDrawer.destroy();

        return this;
    }

    public void onThresholdReached() {
        this.thresholdReached = true;

        if(clearOnThresholdReached)
            clear();

        if(completionCallback != null)
            completionCallback.run();
    }

    public ScratchoffController clear() {
        this.enabled = false;

        if(layoutDrawer != null)
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

    public ScratchoffController setClearOnThresholdReached(boolean clearOnThresholdReached){
        this.clearOnThresholdReached = clearOnThresholdReached;
        return this;
    }

    public ScratchoffController setFadeOnClear(boolean fadeOnClear) {
        this.fadeOnClear = fadeOnClear;
        return this;
    }

    public ScratchoffController setTouchRadiusPx(int touchRadius) {
        this.touchRadiusPx = touchRadius;
        return this;
    }

    public ScratchoffController setTouchRadiusDip(Context context, int touchRadius) {
        this.touchRadiusPx = ViewHelper.getPxFromDip(context, touchRadius);
        return this;
    }

    /**
     * Set a Runnable to be triggered when the percentage of scratched area exceeds the threshold.
     */
    public ScratchoffController setCompletionCallback(Runnable completionCallback){
        this.completionCallback = completionCallback;
        return this;
    }

    /**
     * Set a callback to be triggered when the percentage of scratched area changes.
     * Callback values are in the range [0, 100]
     */
    public ScratchoffController setScratchValueChangedListener(ThresholdProcessor.ScratchValueChangedListener scratchValueChangedListener) {
        this.scratchValueChangedListener = scratchValueChangedListener;

        if (processor != null)
            this.processor.setScratchValueChangedListener(scratchValueChangedListener);

        return this;
    }

    public void addPaths(List<Path> paths) {
        if(layoutDrawer != null)
            layoutDrawer.addPaths(paths);
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

    public boolean isProcessingAllowed() {
        return !thresholdReached
                && scratchableLayout.get() != null
                && ViewHelper.isAttachedToWindow(scratchableLayout.get());
    }

    public int getTotalGridItemsCount() {
        return totalGridItemsCount;
    }

    public View getViewBehind() {
        return behindView.get();
    }

    private void safelyStartProcessors(){
        if(enabled && !(processor == null || processor.isActive()))
            processor.start();
    }

    private void safelyStopProcessors(){
        if(processor != null && processor.isActive())
            processor.cancel();
    }

    public ScratchableLayoutDrawer getLayoutDrawer(){
        return layoutDrawer;
    }

    public void post(Runnable runnable){
        View layout = scratchableLayout.get();

        if(layout != null)
            layout.post(runnable);
    }
}
