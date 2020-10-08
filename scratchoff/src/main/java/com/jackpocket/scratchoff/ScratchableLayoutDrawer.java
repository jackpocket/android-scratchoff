package com.jackpocket.scratchoff;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathQueue;
import com.jackpocket.scratchoff.processors.ScratchoffProcessor;
import com.jackpocket.scratchoff.tools.ViewGroupVisibilityController;

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

    private WeakReference<View> scratchView = new WeakReference<View>(null);

    private State state = State.UNATTACHED;
    private Canvas pathStrippedCanvas;
    private Bitmap pathStrippedImage;

    private WeakReference<Delegate> delegate;

    private Paint clearPaint = new Paint();

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs = 1000;

    private ViewGroupVisibilityController visibilityController = new ViewGroupVisibilityController();

    private final ScratchPathManager pathManager = new ScratchPathManager();
    private final ScratchPathQueue queue = new ScratchPathQueue();

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer(Delegate delegate) {
        this.delegate = new WeakReference<Delegate>(delegate);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(
            ScratchoffController controller,
            View scratchView,
            View behindView) {

        return attach(
                controller.getTouchRadiusPx(),
                scratchView,
                behindView);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(
            int touchRadiusPx,
            final View scratchView,
            final View behindView) {

        synchronized (pathManager) {
            this.scratchView = new WeakReference<View>(scratchView);
            this.state = State.PREPARING;
            this.clearPaint = createClearPaint(touchRadiusPx);

            scratchView.setTag(R.id.scratch__clear_animation_tag, 0L);
            scratchView.clearAnimation();
            scratchView.setVisibility(View.VISIBLE);
            scratchView.setWillNotDraw(false);

            visibilityController.showChildren(scratchView);

            scratchView.invalidate();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
                scratchView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

            enqueueViewInitializationOnGlobalLayout(scratchView, behindView);

            return this;
        }
    }

    protected Paint createClearPaint(int touchRadiusPx) {
        Paint paint = ScratchPathManager.createBaseScratchoffPaint(touchRadiusPx);
        paint.setAlpha(0xFF);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        return paint;
    }

    protected void enqueueViewInitializationOnGlobalLayout(final View scratchView, final View behindView) {
        if (behindView == null) {
            enqueueScratchableViewInitializationOnGlobalLayout(scratchView);

            return;
        }

        addGlobalLayoutRequest(
                behindView,
                new Runnable() {
                    public void run() {
                        performLayoutDimensionMatching(scratchView, behindView);
                        enqueueScratchableViewInitializationOnGlobalLayout(scratchView);
                    }
                });
    }

    protected void performLayoutDimensionMatching(final View scratchView, final View behindView) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = behindView.getWidth();
        params.height = behindView.getHeight();

        scratchView.setLayoutParams(params);
    }

    protected void enqueueScratchableViewInitializationOnGlobalLayout(final View scratchView) {
        addGlobalLayoutRequest(
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

            visibilityController.hideChildren(scratchView);

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
        queue.enqueue(events);
    }

    @SuppressWarnings("WeakerAccess")
    public void draw(Canvas canvas) {
        synchronized (pathManager) {
            Bitmap pathStrippedImage = this.pathStrippedImage;

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
    public void clear(boolean animationEnabled) {
        synchronized (pathManager) {
            if (animationEnabled) {
                performFadeOutClear();

                return;
            }

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    protected void performFadeOutClear() {
        final View v = scratchView.get();

        if (v == null)
            return;

        this.state = State.CLEARING;

        final Long activeClearTag = System.currentTimeMillis();

        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(clearAnimationDurationMs);
        anim.setInterpolator(clearAnimationInterpolator);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) { }

            public void onAnimationRepeat(Animation animation) { }

            public void onAnimationEnd(Animation animation) {
                if (!activeClearTag.equals(v.getTag(R.id.scratch__clear_animation_tag)))
                    return;

                fadeOutClearAnimationCompleted();
            }
        });

        v.setTag(R.id.scratch__clear_animation_tag, activeClearTag);
        v.startAnimation(anim);
    }

    private void fadeOutClearAnimationCompleted() {
        synchronized (pathManager) {
            if (ScratchableLayoutDrawer.this.state != State.CLEARING)
                return;

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    protected void hideAndMarkScratchableSurfaceViewCleared() {
        this.state = State.CLEARED;

        View view = scratchView.get();

        visibilityController.hide(view);
        visibilityController.showChildren(view);
    }

    private void addGlobalLayoutRequest(final View v, final Runnable runnable) {
        v.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if(runnable != null)
                            runnable.run();

                        removeGlobalLayoutListener(v, this);
                    }
                });

        v.requestLayout();
    }

    @SuppressLint("NewApi")
    private void removeGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < 16) {
            v.getViewTreeObserver()
                    .removeGlobalOnLayoutListener(listener);

            return;
        }

        v.getViewTreeObserver()
                .removeOnGlobalLayoutListener(listener);
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
