package com.jackpocket.scratchoff.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.jackpocket.scratchoff.ScratchoffController;

public class ScratchableLinearLayout extends LinearLayout implements ScratchableLayout {

    private ScratchoffController controller;

    public ScratchableLinearLayout(Context context) {
        super(context);

        this.controller = new ScratchoffController(this);
    }

    public ScratchableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.controller = new ScratchoffController(this);
    }

    @SuppressLint("NewApi")
    public ScratchableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.controller = new ScratchoffController(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return controller.parcelize(super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        controller.restore(state);

        super.onRestoreInstanceState(state);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        controller.draw(canvas);
    }

    @Override
    public ScratchoffController getScratchoffController() {
        return controller;
    }
}