# Scratchoff

A simple library for implementing scratchable Views.

![Scratchoff Sample](https://github.com/jackpocket/android_scratchoff/raw/master/scratchoff.gif)

# Installation

```
    repositories {
        jcenter()
    }

    dependencies {
        compile('com.jackpocket:scratchoff:1.0.1')
    }
```

# Usage

The goal of this library is to create a scratchoff interface. By storing and manipulating the drawing cache of a View, we can create the effect of scratching it away to reveal what's hidden below. 

First, you need a RelativeLayout (to align layouts on top of one another) consisting of 2 sub-layouts, a behind-View and the View to be scratched away from the foreground. Here is a simple example using the **ScratchableLinearLayout**:

```
    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent" >

        <RelativeLayout
            android:id="@+id/scratch_view_behind"
            android:layout_width="fill_parent"
            android:layout_height="fill_parent"
            android:background="#818B8D" >

            <ImageView
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:layout_centerInParent="true"
                android:layout_margin="25dip"
                android:adjustViewBounds="true"
                android:src="@drawable/some_drawable_to_be_revealed" />
        </RelativeLayout>

        <com.jackpocket.scratchoff.views.ScratchableLinearLayout
            android:id="@+id/scratch_view"
            android:layout_width="fill_parent"
            android:layout_height="fill_parent"
            android:background="#3C9ADF" >

            <ImageView
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:adjustViewBounds="true"
                android:gravity="center"
                android:src="@drawable/some_top_drawable" />
        </com.jackpocket.scratchoff.views.ScratchableLinearLayout>
    </RelativeLayout>
```

Note: be careful with the dimensions of both the behind-View and the foreground View. The **ScratchableLayoutDrawer** will attempt to set the foreground View's LayoutParam width and height attributes to match that of the behind-View so that their dimensions line up perfectly. 

Now that you have a layout, we need to attach the **ScratchoffController** to it:

```
    ScratchoffController controller = new ScratchoffController(context)
            .setThresholdPercent(0.40d)
            .setTouchRadiusDip(context, 30)
            .setFadeOnClear(true)
            .setClearOnThresholdReached(true)
            .setCompletionCallback(() -> {  })
            .attach(findViewById(R.id.scratch_view), findViewById(R.id.scratch_view_behind));
```

In this example, you only *need* the constructor and the **attach(View, View)** method to enable scratching. The default values for all the other methods are configurable by overriding the appropriate resources.

Since the foreground View in our example is a **ScratchableLinearLayout** (which implements **ScratchableLayout**), the ScratchoffController will automatically attach itself to the View and drawing will work correctly (the same goes for the **ScratchableRelativeLayout**).

Please note: If you're not using one of the supplied ScratchableLayouts, you must manually call **ScratchoffController.draw(Canvas)** from your custom View's **onDraw(Canvas)** method.

As a final note, if using the ScratchoffController in the context of an Activity, you may want to also ensure you call the correct lifecycle methods for *onPause()*, *onResume()*, and *onDestroy()* as needed, to ensure the processors will stop/restart and not run needlessly in the background. e.g.

    @Override
    public void onPause(){
        controller.onPause();
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        controller.onResume();
    }

    @Override
    public void onDestroy(){
        controller.onDestroy();
        super.onDestroy();
    }

