package com.jackpocket.scratchoff;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.jackpocket.scratchoff.processors.ScratchoffProcessor;
import com.jackpocket.scratchoff.views.ScratchableLayout;

import java.util.List;

public class ScratchoffController implements OnTouchListener, LayoutCallback {

    private View scratchableLayout;
    private ScratchableLayoutDrawer layoutDrawer;

    private ScratchoffProcessor processor;

    private Runnable completionCallback;

    private View behindView;

    private int touchRadius;
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

        this.touchRadius = (int) context.getResources()
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
        stopProcessors();

        this.scratchableLayout = scratchableLayout;
        this.behindView = behindView;

        this.layoutDrawer = new ScratchableLayoutDrawer()
                .attach(this, scratchableLayout, behindView);

        scratchableLayout.setOnTouchListener(this);

        return reset();
    }

    public ScratchoffController reset() {
        if(scratchableLayout == null)
            throw new IllegalStateException("Cannot attach to a null View! Ensure you call attach(View, View) with valid Views!");

        stopProcessors();

        this.processor = new ScratchoffProcessor(this);

        scratchableLayout.clearAnimation();

        scratchableLayout.setVisibility(View.VISIBLE);

        scratchableLayout.invalidate();

        if(scratchableLayout instanceof ScratchableLayout)
            ((ScratchableLayout) scratchableLayout).initialize(this);

        return this;
    }

    @Override
    public void onScratchableLayoutAvailable(int width, int height) {
        this.totalGridItemsCount = width * height;

        this.enabled = true;
        this.thresholdReached = false;

        processor.start();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if(!enabled)
            return false;

        processor.onReceieveMotionEvent(event,
                event.getAction() == MotionEvent.ACTION_DOWN);

        this.lastTouchEvent = System.currentTimeMillis();

        return true;
    }

    public void draw(Canvas canvas){
        if(layoutDrawer != null)
            layoutDrawer.draw(canvas);
    }

    public ScratchoffController onPause() {
        stopProcessors();

        return this;
    }

    public ScratchoffController onResume() {
        if(enabled && !(processor == null || processor.isActive()))
            processor.start();

        return this;
    }

    public ScratchoffController onDestroy() {
        if(processor != null && processor.isActive())
            processor.cancel();

        if(layoutDrawer != null)
            layoutDrawer.destroy();

        return this;
    }

    public void onThresholdReached() {
        thresholdReached = true;

        if(clearOnThresholdReached)
            clear();

        if(completionCallback != null)
            completionCallback.run();
    }

    public ScratchoffController clear() {
        enabled = false;

        if(layoutDrawer != null)
            layoutDrawer.clear(fadeOnClear);

        return this;
    }

    public View getScratchImageLayout() {
        return scratchableLayout;
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

    public ScratchoffController setCompletionCallback(Runnable completionCallback){
        this.completionCallback = completionCallback;
        return this;
    }

    public void addPaths(List<Path> paths) {
        if(layoutDrawer != null)
            layoutDrawer.addPaths(paths);
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public int getTouchRadius() {
        return touchRadius;
    }

    public boolean isThresholdReached() {
        return thresholdReached;
    }

    public int getTotalGridItemsCount() {
        return totalGridItemsCount;
    }

    public View getViewBehind() {
        return behindView;
    }

    private void stopProcessors(){
        if(processor != null)
            processor.cancel();
    }

    public ScratchableLayoutDrawer getLayoutDrawer(){
        return layoutDrawer;
    }

    public void post(Runnable runnable){
        if(scratchableLayout != null)
            scratchableLayout.post(runnable);
    }

}
