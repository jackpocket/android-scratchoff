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

import java.util.ArrayList;
import java.util.List;

public class ScratchableLayoutDrawer<T extends View> {

    private T scratchView;
    private Bitmap imageMutable;

    private LayoutCallback gridListener;

    private List<Path> paths = new ArrayList<Path>();

    private Paint clearPaint;
    private boolean cleared = false;

    public ScratchableLayoutDrawer(){
        this.cleared = false;
        this.imageMutable = null;
        this.paths = new ArrayList<Path>();
    }

    public ScratchableLayoutDrawer attach(ScratchoffController controller, T scratchView, final View behindView){
        this.scratchView = scratchView;
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
        clearPaint.setStrokeWidth(controller.getTouchRadius() * 2);

        setBehindView(behindView);

        return this;
    }

    private void setBehindView(final View behindView) {
        ViewHelper.addGlobalLayoutRequest(behindView,
                new Runnable(){
                    public void run(){
                        initializeBehindView(behindView);

                        waitForDisplay();
                    }
                });
    }

    private void initializeBehindView(View view) {
        ViewGroup.LayoutParams params = scratchView.getLayoutParams();
        params.width = view.getWidth();
        params.height = view.getHeight();

        scratchView.setLayoutParams(params);
    }

    private void waitForDisplay() {
        ViewHelper.addGlobalLayoutRequest(scratchView,
                new Runnable(){
                    public void run(){
                        initializePostDisplay();
                    }
                });
    }

    private void initializePostDisplay() {
        scratchView.setDrawingCacheEnabled(true);
        scratchView.buildDrawingCache();

        Bitmap cached = scratchView.getDrawingCache();
        imageMutable = Bitmap.createBitmap(cached);

        scratchView.setDrawingCacheEnabled(false);

        scratchView.setBackgroundColor(Color.TRANSPARENT);

        hideChildren();

        gridListener.onScratchableLayoutAvailable(imageMutable.getWidth(),
                imageMutable.getHeight());
    }

    public void draw(Canvas canvas) {
        if(cleared || imageMutable == null)
            return;
        else{
            canvas.drawBitmap(imageMutable, 0, 0, null);

            for(Path path : getPaths())
                canvas.drawPath(path, clearPaint);
        }
    }
    public void addPaths(List<Path> paths) {
        getPaths()
                .addAll(paths);
    }

    private synchronized List<Path> getPaths() {
        return paths;
    }

    public void destroy() {
        if(imageMutable != null){
            imageMutable.recycle();
            imageMutable = null;
        }
    }

    public void clear(boolean fade) {
        this.cleared = false;

        if(fade)
            fadeOut();
        else scratchView.invalidate();
    }

    private void fadeOut() {
        AlphaAnimation anim = new AlphaAnimation(1f, 0f);
        anim.setDuration(1000);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) { }

            public void onAnimationRepeat(Animation animation) { }

            public void onAnimationEnd(Animation animation) {
                scratchView.setVisibility(View.GONE);

                showChildren();
            }
        });

        scratchView.startAnimation(anim);
    }

    private void hideChildren(){
        if(scratchView instanceof ViewGroup){
            ViewGroup group = (ViewGroup) scratchView;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.GONE);
        }
    }

    private void showChildren(){
        if(scratchView instanceof ViewGroup){
            ViewGroup group = (ViewGroup) scratchView;

            for(int i = 0; i < group.getChildCount(); i++)
                group.getChildAt(i)
                        .setVisibility(View.VISIBLE);
        }
    }

}
