package com.jackpocket.scratchoff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.jackpocket.scratchoff.ScratchoffController;

public class ScratchableRelativeLayout extends RelativeLayout implements ScratchableLayout {

    private ScratchoffController controller;

    public ScratchableRelativeLayout(Context context) {
        super(context);
    }

    public ScratchableRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    public ScratchableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void initialize(ScratchoffController controller) {
        this.controller = controller;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(controller != null)
            controller.draw(canvas);
    }

}