package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.jackpocket.scratchoff.ScratchoffController;

import java.util.List;

public class ThresholdProcessor extends Processor {

    private static final int SLEEP_DELAY_RUNNING = 500;
    private static final int SLEEP_DELAY_START = 1000;

    private static final int MARKER_UNTOUCHED = 0xFFFFFFFF;
    private static final int MARKER_SCRATCHED = 0xFF000000;

    private ScratchoffController controller;

    private Bitmap currentBitmap;

    private Canvas canvas;
    private Paint markerPaint = new Paint();

    private boolean thresholdReached = false;

    public ThresholdProcessor(ScratchoffController controller) {
        this.controller = controller;

        this.markerPaint.setAntiAlias(true);
        this.markerPaint.setStyle(Paint.Style.STROKE);
        this.markerPaint.setStrokeCap(Paint.Cap.ROUND);
        this.markerPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void start(){
        this.markerPaint.setStrokeWidth(controller.getTouchRadiusPx() * 2);
        this.thresholdReached = false;

        safelyReleaseCurrentBitmap();

        super.start();
    }

    public void addPaths(List<Path> paths){
        if(currentBitmap == null)
            return;

        synchronized(currentBitmap){
            for(Path path : paths)
                canvas.drawPath(path, markerPaint);
        }
    }

    @Override
    protected void doInBackground() throws Exception {
        Thread.sleep(SLEEP_DELAY_START);

        if(controller.isProcessingAllowed())
            prepareCanvas();

        while(isActive() && controller.isProcessingAllowed()){
            processImage();

            sleep(SLEEP_DELAY_RUNNING);
        }

        safelyReleaseCurrentBitmap();
    }

    private void prepareCanvas(){
        this.currentBitmap = Bitmap.createBitmap(controller.getLayoutDrawer()
                .getPathStrippedImage());

        this.canvas = new Canvas(currentBitmap);
        this.canvas.drawColor(MARKER_UNTOUCHED);
    }

    private void processImage(){
        double percentScratched = Math.min(1, getScratchedCount(currentBitmap) / (currentBitmap.getWidth() * currentBitmap.getHeight()));

        if(controller.getThresholdPercent() < percentScratched && !thresholdReached){
            thresholdReached = true;

            postThresholdReached();
        }
    }

    private double getScratchedCount(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        double scratched = 0;

        for(int pixel : pixels)
            if(pixel == MARKER_SCRATCHED)
                scratched++;

        return scratched;
    }

    private void postThresholdReached() {
        controller.post(new Runnable() {
            public void run() {
                controller.onThresholdReached();
            }
        });
    }

    @Override
    public void cancel(){
        super.cancel();

        safelyReleaseCurrentBitmap();
    }

    private void safelyReleaseCurrentBitmap(){
        try{
            if(currentBitmap != null){
                currentBitmap.recycle();
                currentBitmap = null;

                canvas = null;
            }
        }
        catch(Exception e){ e.printStackTrace(); }
    }

}
