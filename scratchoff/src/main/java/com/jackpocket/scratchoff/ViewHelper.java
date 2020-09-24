package com.jackpocket.scratchoff;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

@SuppressWarnings("WeakerAccess")
public class ViewHelper {

    @SuppressLint("NewApi")
    public static void disableHardwareAcceleration(View v) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB)
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public static void addGlobalLayoutRequest(final View v, final Runnable runnable) {
        v.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if(runnable != null)
                            runnable.run();

                        removeOnGlobalLayoutListener(v, this);
                    }
                });

        v.requestLayout();
    }

    @SuppressLint("NewApi")
    public static void removeOnGlobalLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < 16) {
            v.getViewTreeObserver()
                    .removeGlobalOnLayoutListener(listener);

            return;
        }

        v.getViewTreeObserver()
                .removeOnGlobalLayoutListener(listener);
    }

    public static int getPxFromDip(Context context, int dip) {
        return (int) (dip * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static Paint createBaseScratchoffPaint(int touchRadiusPx) {
        Paint markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStrokeCap(Paint.Cap.ROUND);
        markerPaint.setStrokeJoin(Paint.Join.ROUND);
        markerPaint.setStrokeWidth(touchRadiusPx * 2);

        return markerPaint;
    }
}
