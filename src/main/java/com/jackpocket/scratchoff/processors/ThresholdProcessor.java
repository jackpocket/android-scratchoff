package com.jackpocket.scratchoff.processors;

import android.graphics.Bitmap;

import com.jackpocket.scratchoff.ScratchoffController;

public class ThresholdProcessor extends Processor {

    private static final int SLEEP_DELAY_WAITING = 50;
    private static final int SLEEP_DELAY_RUNNING = 500;
    private static final int SLEEP_DELAY_START = 1000;

    private static final int ALPHA_INDEX = 24;
    private static final int ALPHA_ZERO = 0x00;

    private ScratchoffController controller;

    private Bitmap currentBitmap;
    private boolean thresholdReached = false;

    public ThresholdProcessor(ScratchoffController controller) {
        this.controller = controller;
    }

    @Override
    public void start(){
        thresholdReached = false;

        safelyReleaseCurrentBitmap();

        super.start();
    }

    @Override
    protected void doInBackground() throws Exception {
        Thread.sleep(SLEEP_DELAY_START);

        while(isActive() && !controller.isThresholdReached()){
            getUpdatedDrawingCache();

            while(currentBitmap == null)
                sleep(SLEEP_DELAY_WAITING);

            processImage();

            sleep(SLEEP_DELAY_RUNNING);
        }
    }

    private void processImage(){
        double percentScratched = getScratchedCount(currentBitmap) / (currentBitmap.getWidth() * currentBitmap.getHeight());

        if(controller.getThresholdPercent() < percentScratched && !thresholdReached){
            thresholdReached = true;

            postThresholdReached();
        }

        safelyReleaseCurrentBitmap();
    }

    private double getScratchedCount(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        double scratched = 0;

        for(int pixel : pixels)
            if(pixel >> ALPHA_INDEX == ALPHA_ZERO)
                scratched++;

        return scratched;
    }

    private void getUpdatedDrawingCache() {
        controller.post(new Runnable() {
            public void run() {
                controller.getScratchImageLayout()
                        .setDrawingCacheEnabled(true);

                currentBitmap = Bitmap.createBitmap(controller.getScratchImageLayout()
                        .getDrawingCache());

                controller.getScratchImageLayout()
                        .setDrawingCacheEnabled(false);
            }
        });
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
            currentBitmap.recycle();
            currentBitmap = null;
        }
        catch(Exception e){ e.printStackTrace(); }
    }

}
