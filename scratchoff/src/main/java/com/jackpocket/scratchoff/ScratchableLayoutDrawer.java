package com.jackpocket.scratchoff;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpocket.scratchoff.paths.ScratchPathManager;
import com.jackpocket.scratchoff.paths.ScratchPathPoint;
import com.jackpocket.scratchoff.paths.ScratchPathPointsAggregator;
import com.jackpocket.scratchoff.tools.ViewGroupVisibilityController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ScratchableLayoutDrawer implements ScratchPathPointsAggregator, Animation.AnimationListener {

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

    private WeakReference<View> scratchView = new WeakReference<>(null);

    private State state = State.UNATTACHED;
    private Canvas pathStrippedCanvas;
    private Bitmap pathStrippedImage;

    private final WeakReference<Delegate> delegate;

    private Paint clearPaint = new Paint();

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs = 1000;

    private final ViewGroupVisibilityController visibilityController = new ViewGroupVisibilityController();

    private final ArrayList<ScratchPathPoint> pendingPathPoints = new ArrayList<ScratchPathPoint>();
    private final ScratchPathManager pathManager = new ScratchPathManager();

    private Long activeClearTag = 0L;

    private boolean usePreDrawOverGlobalLayoutEnabled = false;
    private boolean attemptLastDitchPostForLayoutResolutionFailure = false;
    private boolean keepListeningForDrawUntilValidSizeDiscovered = false;

    private WeakReference<View> initializeLayoutTarget = new WeakReference<>(null);
    private ViewTreeObserver.OnGlobalLayoutListener initializationGlobalLayoutListener;
    private ViewTreeObserver.OnPreDrawListener initializationPreDrawListener;

    public ScratchableLayoutDrawer(Delegate delegate) {
        this.delegate = new WeakReference<>(delegate);
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(
        ScratchoffController controller,
        View scratchView,
        View behindView
    ) {

        return attach(
            controller.getTouchRadiusPx(),
            scratchView,
            behindView
        );
    }

    public ScratchableLayoutDrawer attach(
        int touchRadiusPx,
        final View scratchView,
        final View behindView
    ) {

        synchronized (pathManager) {
            this.scratchView = new WeakReference<>(scratchView);
            this.state = State.PREPARING;
            this.clearPaint = createClearPaint(touchRadiusPx);
            this.activeClearTag = System.currentTimeMillis();

            scratchView.setTag(R.id.scratch__clear_animation_tag, 0L);
            scratchView.clearAnimation();
            scratchView.setVisibility(View.VISIBLE);
            scratchView.setWillNotDraw(false);

            visibilityController.showChildren(scratchView);

            scratchView.invalidate();

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

        deferRunnableUntilViewIsLaidOut(
            behindView,
            new Runnable() {
                public void run() {
                    performLayoutDimensionMatching(scratchView, behindView);
                    enqueueScratchableViewInitializationOnGlobalLayout(scratchView);
                }
            }
        );
    }

    @SuppressWarnings("WeakerAccess")
    protected void performLayoutDimensionMatching(final View scratchView, final View behindView) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = behindView.getWidth();
        params.height = behindView.getHeight();

        scratchView.setLayoutParams(params);
    }

    @SuppressWarnings("WeakerAccess")
    protected void enqueueScratchableViewInitializationOnGlobalLayout(final View scratchView) {
        deferRunnableUntilViewIsLaidOut(
            scratchView,
            new Runnable() {
                public void run() {
                    initializeLaidOutScratchableView(scratchView);
                }
            }
        );
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
                    pathStrippedImage.getHeight()
                );
            }

            this.state = State.SCRATCHABLE;

            addPendingScratchPathPointsAndClear();
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected Bitmap createBitmapFromScratchableView(final View scratchView) {
        Bitmap bitmap = Bitmap.createBitmap(
            scratchView.getWidth(),
            scratchView.getHeight(),
            Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        scratchView.draw(canvas);

        return bitmap;
    }

    protected void addPendingScratchPathPointsAndClear() {
        final List<ScratchPathPoint> pendingPoints;

        synchronized (pathManager) {
            if (this.pendingPathPoints.isEmpty()) {
                return;
            }

            pendingPoints = new ArrayList<ScratchPathPoint>(this.pendingPathPoints);

            this.pendingPathPoints.clear();
        }

        addScratchPathPoints(pendingPoints);
    }

    @Override
    public void addScratchPathPoints(Collection<ScratchPathPoint> events) {
        final State state;
        final Bitmap pathStrippedImage;
        final Canvas pathStrippedCanvas;

        synchronized (pathManager) {
            state = this.state;
            pathStrippedImage = this.pathStrippedImage;
            pathStrippedCanvas = this.pathStrippedCanvas;
        }

        switch (state) {
            case UNATTACHED:
            case CLEARED:
                break;
            case PREPARING:
                synchronized (pathManager) {
                    this.pendingPathPoints.addAll(events);
                }

                break;
            default:
                pathManager.addScratchPathPoints(events);
                pathManager.drawAndReset(pathStrippedCanvas, clearPaint);

                pathStrippedImage.prepareToDraw();
        }
    }

    public void draw(Canvas canvas) {
        final State state;
        final Bitmap pathStrippedImage;

        synchronized (pathManager) {
            state = this.state;
            pathStrippedImage = this.pathStrippedImage;
        }

        if (pathStrippedImage == null) {
            return;
        }

        switch (state) {
            case UNATTACHED:
            case PREPARING:
            case CLEARED:
                return;
            default:
                canvas.drawBitmap(pathStrippedImage, 0, 0, null);
        }
    }

    public void destroy() {
        removeInitializationViewTreeObservers();

        synchronized (pathManager) {
            this.state = State.UNATTACHED;

            if (pathStrippedImage == null) {
                return;
            }

            pathStrippedImage.recycle();
            pathStrippedImage = null;

            pathStrippedCanvas = null;

            pendingPathPoints.clear();
            pathManager.clear();
        }
    }

    private void removeInitializationViewTreeObservers() {
        View layoutTarget = this.initializeLayoutTarget.get();

        if (layoutTarget == null) {
            return;
        }

        ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = this.initializationGlobalLayoutListener;

        if (globalLayoutListener != null) {
            removeGlobalLayoutListener(layoutTarget, globalLayoutListener);

            this.initializationGlobalLayoutListener = null;
        }

        ViewTreeObserver.OnPreDrawListener preDrawListener = this.initializationPreDrawListener;

        if (preDrawListener != null) {
            layoutTarget
                .getViewTreeObserver()
                .removeOnPreDrawListener(preDrawListener);

            this.initializationPreDrawListener = null;
        }
    }

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
        final View view = scratchView.get();

        if (view == null) {
            return;
        }

        this.state = State.CLEARING;

        claimClearAnimation(view, System.currentTimeMillis());
        performFadeOutClear(view);
    }

    protected void claimClearAnimation(View view, long id) {
        this.activeClearTag = id;

        view.setTag(R.id.scratch__clear_animation_tag, activeClearTag);
    }

    protected void performFadeOutClear(View view) {
        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(clearAnimationDurationMs);
        anim.setInterpolator(clearAnimationInterpolator);
        anim.setFillAfter(true);
        anim.setAnimationListener(this);

        view.startAnimation(anim);
    }

    public void onAnimationStart(Animation animation) {

    }

    public void onAnimationRepeat(Animation animation) {

    }

    public void onAnimationEnd(Animation animation) {
        final View view = scratchView.get();

        if (view == null) {
            return;
        }

        if (!activeClearTag.equals(view.getTag(R.id.scratch__clear_animation_tag))) {
            return;
        }

        synchronized (pathManager) {
            if (ScratchableLayoutDrawer.this.state != State.CLEARING) {
                return;
            }

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    protected void hideAndMarkScratchableSurfaceViewCleared() {
        this.state = State.CLEARED;

        View view = scratchView.get();

        visibilityController.hide(view);
        visibilityController.showChildren(view);
    }

    private void deferRunnableUntilViewIsLaidOut(final View view, final Runnable runnable) {
        this.initializeLayoutTarget = new WeakReference<>(view);

        if (usePreDrawOverGlobalLayoutEnabled) {
            deferRunnableWithPreDrawListener(view, runnable);
        }
        else {
            deferRunnableWithGlobalLayoutListener(view, runnable);
        }

        view.requestLayout();
    }

    private void deferRunnableWithPreDrawListener(final View view, final Runnable runnable) {
        ViewTreeObserver.OnPreDrawListener preDrawListenerForInit = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                boolean sizeValid = isViewSizeValidForInitialization(view);

                if (keepListeningForDrawUntilValidSizeDiscovered && !sizeValid) {
                    return true;
                }

                ScratchableLayoutDrawer.this.initializationPreDrawListener = null;

                triggerOrPostRunnableOnLaidOut(runnable, sizeValid);

                view
                    .getViewTreeObserver()
                    .removeOnPreDrawListener(this);

                return true;
            }
        };

        view
            .getViewTreeObserver()
            .addOnPreDrawListener(preDrawListenerForInit);

        this.initializationPreDrawListener = preDrawListenerForInit;
    }

    private void deferRunnableWithGlobalLayoutListener(final View view, final Runnable runnable) {
        ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                boolean sizeValid = isViewSizeValidForInitialization(view);

                if (keepListeningForDrawUntilValidSizeDiscovered && !sizeValid) {
                    return;
                }

                ScratchableLayoutDrawer.this.initializationGlobalLayoutListener = null;

                triggerOrPostRunnableOnLaidOut(runnable, sizeValid);
                removeGlobalLayoutListener(view, this);
            }
        };

        view
            .getViewTreeObserver()
            .addOnGlobalLayoutListener(globalLayoutListener);

        this.initializationGlobalLayoutListener = globalLayoutListener;
    }

    /**
     * This function will be removed in 4.x, given {@link attemptLastDitchPostForLayoutResolutionFailure}
     * did more harm than good, and replaced with directly calling ``run`` on the ``Runnable``.
     */
    protected void triggerOrPostRunnableOnLaidOut(
        Runnable runnable,
        boolean sizeValidForInitialization
    ) {

        if (runnable == null) {
            return;
        }

        if (attemptLastDitchPostForLayoutResolutionFailure) {
            if (!sizeValidForInitialization) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(runnable);

                return;
            }
        }

        runnable.run();
    }

    private boolean isViewSizeValidForInitialization(View view) {
        return 0 < view.getWidth() && 0 < view.getHeight();
    }

    @SuppressWarnings({ "deprecation", "RedundantSuppression" })
    private void removeGlobalLayoutListener(View view, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < 16) {
            view.getViewTreeObserver()
                .removeGlobalOnLayoutListener(listener);

            return;
        }

        view.getViewTreeObserver()
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

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer setUsePreDrawOverGlobalLayoutEnabled(boolean usePreDrawOverGlobalLayoutEnabled) {
        this.usePreDrawOverGlobalLayoutEnabled = usePreDrawOverGlobalLayoutEnabled;

        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer setAttemptLastDitchPostForLayoutResolutionFailure(
        boolean attemptLastDitchPostForLayoutResolutionFailure
    ) {

        this.attemptLastDitchPostForLayoutResolutionFailure = attemptLastDitchPostForLayoutResolutionFailure;

        return this;
    }

    public ScratchableLayoutDrawer setKeepListeningForDrawUntilValidSizeDiscovered(
        boolean keepListeningForDrawUntilValidSizeDiscovered
    ) {

        this.keepListeningForDrawUntilValidSizeDiscovered = keepListeningForDrawUntilValidSizeDiscovered;

        return this;
    }
}
