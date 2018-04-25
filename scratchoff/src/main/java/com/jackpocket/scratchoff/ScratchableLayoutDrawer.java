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

import java.lang.ref.WeakReference;
import java.util.List;

public class ScratchableLayoutDrawer<T extends View> {

    private WeakReference<T> scratchView;

    private Canvas pathStrippedCanvas;
    private Bitmap pathStrippedImage;

    private LayoutCallback gridListener;

    private Paint clearPaint;
    private boolean cleared = false;

    public ScratchableLayoutDrawer(){
        this.cleared = false;
        this.pathStrippedImage = null;
    }

    public ScratchableLayoutDrawer attach(ScratchoffController controller, T scratchView, final View behindView){
        this.scratchView = new WeakReference<T>(scratchView);
        this.gridListener = controller;

        scratchView.setWillNotDraw(false);

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

    private void setBehindView(final T scratchView, final View behindView) {
        ViewHelper.addGlobalLayoutRequest(behindView,
                new Runnable(){
                    public void run(){
                        initializeBehindView(scratchView, behindView);

                        waitForDisplay(scratchView);
                    }
                });
    }

    private void initializeBehindView(final T scratchView, final View behindView) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = behindView.getWidth();
        params.height = behindView.getHeight();

        scratchView.setLayoutParams(params);
    }

    private void waitForDisplay(final T scratchView) {
        ViewHelper.addGlobalLayoutRequest(scratchView,
                new Runnable(){
                    public void run(){
                        initializePostDisplay(scratchView);
                    }
                });
    }

    private void initializePostDisplay(final T scratchView) {
        scratchView.setDrawingCacheEnabled(true);
        scratchView.buildDrawingCache();

        Bitmap cached = scratchView.getDrawingCache();
        pathStrippedImage = Bitmap.createBitmap(cached);

        pathStrippedCanvas = new Canvas(pathStrippedImage);

        scratchView.setDrawingCacheEnabled(false);

        scratchView.setBackgroundColor(Color.TRANSPARENT);

        hideChildren();

        gridListener.onScratchableLayoutAvailable(pathStrippedImage.getWidth(),
                pathStrippedImage.getHeight());
    }

    public void draw(Canvas canvas) {
        if(cleared || pathStrippedImage == null)
            return;
        else
            canvas.drawBitmap(pathStrippedImage, 0, 0, null);
    }

    public void addPaths(List<Path> paths) {
        if(pathStrippedImage == null)
            return;

        synchronized(pathStrippedImage){
            for(Path path : paths)
                pathStrippedCanvas.drawPath(path, clearPaint);
        }
    }

    public void destroy() {
        if(pathStrippedImage != null){
            pathStrippedImage.recycle();
            pathStrippedImage = null;

            pathStrippedCanvas = null;
        }
    }

    public void clear(boolean fade) {
        this.cleared = false;

        if(fade)
            fadeOut();
        else{

            final View v = scratchView.get();

            if(v != null)
                v.invalidate();
        }
    }

    private void fadeOut() {
        final View v = scratchView.get();

        if(v == null)
            return;

        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(1000);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) { }

            public void onAnimationRepeat(Animation animation) { }

            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);

                showChildren();
            }
        });

        v.startAnimation(anim);
    }

    private void hideChildren(){
        final View v = scratchView.get();

        if(v == null)
            return;

        if(v instanceof ViewGroup){
            ViewGroup group = (ViewGroup) v;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.GONE);
        }
    }

    private void showChildren(){
        final View v = scratchView.get();

        if(v == null)
            return;

        if(v instanceof ViewGroup){
            ViewGroup group = (ViewGroup) v;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.VISIBLE);
        }
    }

    public Bitmap getPathStrippedImage(){
        return pathStrippedImage;
    }

}
