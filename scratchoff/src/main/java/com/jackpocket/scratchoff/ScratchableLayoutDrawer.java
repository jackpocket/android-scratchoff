package com.jackpocket.scratchoff;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;
import com.jackpocket.scratchoff.processors.ScratchoffProcessor;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchableLayoutDrawer implements ScratchoffProcessor.Delegate {

    enum State {
        UNATTACHED,
        PREPARING,
        SCRATCHABLE,
        CLEARING,
        CLEARED
    }

    public interface Delegate {
        public void onScratchableLayoutAvailable(int width, int height);
    }

    private WeakReference<View> scratchView;

    private State state = State.UNATTACHED;
    private Canvas pathStrippedCanvas;
    private Bitmap pathStrippedImage;

    private WeakReference<Delegate> delegate = new WeakReference<Delegate>(null);

    private Paint clearPaint;

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs = 1000;

    private final ScratchPathManager pathManager = new ScratchPathManager();
    private final ScratchPathQueue queue = new ScratchPathQueue();

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer() { }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(
            ScratchoffController controller,
            View scratchView,
            final View behindView) {

        return attach(
                controller,
                controller.getTouchRadiusPx(),
                scratchView,
                behindView);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(
            Delegate delegate,
            int touchRadiusPx,
            final View scratchView,
            final View behindView) {

        synchronized (pathManager) {
            this.scratchView = new WeakReference<View>(scratchView);
            this.delegate = new WeakReference<Delegate>(delegate);
            this.state = State.PREPARING;

            scratchView.clearAnimation();
            scratchView.setVisibility(View.VISIBLE);
            scratchView.setWillNotDraw(false);

            showScratchableViewChildren();

            scratchView.invalidate();

            ViewHelper.disableHardwareAcceleration(scratchView);

            clearPaint = ViewHelper.createBaseScratchoffPaint(touchRadiusPx);
            clearPaint.setAlpha(0xFF);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

            enqueueViewInitializationOnGlobalLayout(scratchView, behindView);

            return this;
        }
    }

    protected void enqueueViewInitializationOnGlobalLayout(final View scratchView, final View behindView) {
        ViewHelper.addGlobalLayoutRequest(
                behindView,
                new Runnable() {
                    public void run() {
                        forceLayoutParamWidthHeightMatch(scratchView, behindView);
                        enqueueScratchableViewInitializationOnGlobalLayout(scratchView);
                    }
                });
    }

    protected void forceLayoutParamWidthHeightMatch(final View scratchView, final View behindView) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = behindView.getWidth();
        params.height = behindView.getHeight();

        scratchView.setLayoutParams(params);
    }

    protected void enqueueScratchableViewInitializationOnGlobalLayout(final View scratchView) {
        ViewHelper.addGlobalLayoutRequest(
                scratchView,
                new Runnable() {
                    public void run() {
                        initializeLaidOutScratchableView(scratchView);
                    }
                });
    }

    protected void initializeLaidOutScratchableView(final View scratchView) {
        synchronized (pathManager) {
            this.pathStrippedImage = createBitmapFromScratchableView(scratchView);
            this.pathStrippedCanvas = new Canvas(pathStrippedImage);

            scratchView.setBackgroundColor(Color.TRANSPARENT);

            hideScratchableViewChildren();

            Delegate delegate = this.delegate.get();

            if (delegate != null) {
                delegate.onScratchableLayoutAvailable(
                        pathStrippedImage.getWidth(),
                        pathStrippedImage.getHeight());
            }

            this.state = State.SCRATCHABLE;
        }
    }

    protected Bitmap createBitmapFromScratchableView(final View scratchView) {
        Bitmap bitmap = Bitmap.createBitmap(
                scratchView.getWidth(),
                scratchView.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        scratchView.draw(canvas);

        return bitmap;
    }

    @Override
    public void enqueueScratchMotionEvents(List<ScratchPathPoint> events) {
        synchronized (pathManager) {
            if (pathStrippedImage == null)
                return;

            queue.enqueue(events);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void draw(Canvas canvas) {
        synchronized (pathManager) {
            if (pathStrippedImage == null)
                return;

            switch (state) {
                case UNATTACHED:
                case PREPARING:
                case CLEARED:
                    return;
                default:
                    drawQueuedScratchMotionEvents();

                    canvas.drawBitmap(pathStrippedImage, 0, 0, null);
            }
        }
    }

    protected void drawQueuedScratchMotionEvents() {
        List<ScratchPathPoint> dequeuedEvents = queue.dequeue();

        if (dequeuedEvents.size() < 1)
            return;

        pathManager.addMotionEvents(dequeuedEvents);
        pathManager.draw(pathStrippedCanvas, clearPaint);
    }

    @SuppressWarnings("WeakerAccess")
    public void destroy() {
        synchronized (pathManager) {
            this.state = State.UNATTACHED;

            if (pathStrippedImage == null)
                return;

            pathStrippedImage.recycle();
            pathStrippedImage = null;

            pathStrippedCanvas = null;

            pathManager.clear();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void clear(boolean fade) {
        synchronized (pathManager) {
            if (fade) {
                performFadeOutClear();

                return;
            }

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    private void performFadeOutClear() {
        final View v = scratchView.get();

        if (v == null)
            return;

        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(clearAnimationDurationMs);
        anim.setInterpolator(clearAnimationInterpolator);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) { }

            public void onAnimationRepeat(Animation animation) { }

            public void onAnimationEnd(Animation animation) {
                fadeOutClearAnimationCompleted();
            }
        });

        v.startAnimation(anim);
    }

    private void fadeOutClearAnimationCompleted() {
        synchronized (pathManager) {
            if (ScratchableLayoutDrawer.this.state != State.CLEARING)
                return;

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    private void hideAndMarkScratchableSurfaceViewCleared() {
        this.state = State.CLEARED;

        final View view = scratchView.get();

        if (view == null)
            return;

        view.setVisibility(View.GONE);

        showScratchableViewChildren();
    }

    private void hideScratchableViewChildren() {
        final View v = scratchView.get();

        if (v == null)
            return;

        if (v instanceof ViewGroup){
            ViewGroup group = (ViewGroup) v;

            for (int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.GONE);
        }
    }

    private void showScratchableViewChildren() {
        final View v = scratchView.get();

        if (v == null)
            return;

        if (v instanceof ViewGroup){
            ViewGroup group = (ViewGroup) v;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer setClearAnimationDurationMs(long clearAnimationDurationMs) {
        this.clearAnimationDurationMs = clearAnimationDurationMs;

        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer setClearAnimationInterpolator(Interpolator clearAnimationInterpolator) {
        this.clearAnimationInterpolator = clearAnimationInterpolator;

        return this;
    }
}
