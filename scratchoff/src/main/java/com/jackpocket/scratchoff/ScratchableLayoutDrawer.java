package com.jackpocket.scratchoff;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchableLayoutDrawer {

    enum State {
        UNATTACHED,
        PREPARING,
        SCRATCHABLE,
        CLEARING,
        CLEARED
    }

    private WeakReference<View> scratchView;

    private State state = State.UNATTACHED;
    private Canvas pathStrippedCanvas;
    private Bitmap pathStrippedImage;

    private LayoutCallback gridListener;

    private Paint clearPaint;

    private Interpolator clearAnimationInterpolator = new LinearInterpolator();
    private long clearAnimationDurationMs = 1000;

    private final Boolean lock = true;

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer() { }

    @SuppressWarnings("WeakerAccess")
    public ScratchableLayoutDrawer attach(ScratchoffController controller, View scratchView, final View behindView) {
        synchronized (lock) {
            this.scratchView = new WeakReference<View>(scratchView);
            this.gridListener = controller;
            this.state = State.PREPARING;

            scratchView.clearAnimation();
            scratchView.setVisibility(View.VISIBLE);
            scratchView.setWillNotDraw(false);

            showChildren();

            scratchView.invalidate();

            ViewHelper.disableHardwareAcceleration(scratchView);

            clearPaint = new Paint();
            clearPaint.setAlpha(0xFF);
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            clearPaint.setStyle(Paint.Style.STROKE);
            clearPaint.setStrokeCap(Paint.Cap.ROUND);
            clearPaint.setStrokeJoin(Paint.Join.ROUND);
            clearPaint.setAntiAlias(true);
            clearPaint.setStrokeWidth(controller.getTouchRadiusPx() * 2);

            setBehindView(scratchView, behindView);

            return this;
        }
    }

    private void setBehindView(final View scratchView, final View behindView) {
        ViewHelper.addGlobalLayoutRequest(
                behindView,
                new Runnable(){
                    public void run(){
                        initializeBehindView(scratchView, behindView);

                        waitForDisplay(scratchView);
                    }
                });
    }

    private void initializeBehindView(final View scratchView, final View behindView) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = behindView.getWidth();
        params.height = behindView.getHeight();

        scratchView.setLayoutParams(params);
    }

    private void waitForDisplay(final View scratchView) {
        ViewHelper.addGlobalLayoutRequest(
                scratchView,
                new Runnable(){
                    public void run(){
                        initializePostDisplay(scratchView);
                    }
                });
    }

    private void initializePostDisplay(final View scratchView) {
        synchronized (lock) {
            this.pathStrippedImage = createBitmapFromScratchableView(scratchView);
            this.pathStrippedCanvas = new Canvas(pathStrippedImage);

            scratchView.setBackgroundColor(Color.TRANSPARENT);

            hideChildren();

            gridListener.onScratchableLayoutAvailable(
                    pathStrippedImage.getWidth(),
                    pathStrippedImage.getHeight());

            this.state = State.SCRATCHABLE;
        }
    }

    private Bitmap createBitmapFromScratchableView(final View scratchView) {
        Bitmap bitmap = Bitmap.createBitmap(
                scratchView.getWidth(),
                scratchView.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        scratchView.draw(canvas);

        return bitmap;
    }

    @SuppressWarnings("WeakerAccess")
    public void draw(Canvas canvas) {
        synchronized (lock) {
            if (pathStrippedImage == null)
                return;

            switch (state) {
                case UNATTACHED:
                case PREPARING:
                case CLEARED:
                    return;
                default:
                    canvas.drawBitmap(pathStrippedImage, 0, 0, null);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void addPaths(List<Path> paths) {
        synchronized (lock) {
            if (pathStrippedImage == null)
                return;

            for (Path path : paths)
                pathStrippedCanvas.drawPath(path, clearPaint);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void destroy() {
        synchronized (lock) {
            this.state = State.UNATTACHED;

            if (pathStrippedImage == null)
                return;

            pathStrippedImage.recycle();
            pathStrippedImage = null;

            pathStrippedCanvas = null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void clear(boolean fade) {
        synchronized (lock) {
            if (fade) {
                fadeOut();

                return;
            }

            hideAndMarkScratchableSurfaceViewCleared();
        }
    }

    private void fadeOut() {
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
                synchronized (lock) {
                    if (ScratchableLayoutDrawer.this.state != State.CLEARING)
                        return;

                    hideAndMarkScratchableSurfaceViewCleared();
                }
            }
        });

        v.startAnimation(anim);
    }

    private void hideAndMarkScratchableSurfaceViewCleared() {
        this.state = State.CLEARED;

        final View view = scratchView.get();

        if (view == null)
            return;

        view.setVisibility(View.GONE);

        showChildren();
    }

    private void hideChildren() {
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

    private void showChildren() {
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

    public Bitmap getPathStrippedImage() {
        synchronized (lock) {
            return pathStrippedImage;
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
