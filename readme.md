# Scratchoff

A simple library for implementing scratchable Views.

![Scratchoff Sample](https://github.com/jackpocket/android_scratchoff/raw/master/scratchoff.gif)

# Installation

```groovy
repositories {
    jcenter()
}

dependencies {
    compile('com.jackpocket:scratchoff:2.0.0')
}
```

# Usage

### Layout Setup

First, you need a parent `ViewGroup` that supports vertical stacking, like a `RelativeLayout`. That parent `ViewGroup` should consist of 2 sub-layouts: 

1. a behind-View to be revealed
2. a foreground-View to be scratched away

Here is a simple example using the `ScratchableLinearLayout`:

```xml
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

Note: be careful with the dimensions of both the behind-View and the foreground-View. The `ScratchableLayoutDrawer` will attempt to set the foreground-View's LayoutParam width and height attributes to match that of the behind-View so that their dimensions are equal.

### Scratch Threshold Changed / Threshold Reached Callback Setup

The `ScratchoffController` will call its delegate's methods when the scratched threshold has changed or has been reached, but you need to maintain a strong reference to the supplied `ScratchoffController.Delegate` instance.

```java
public class MainActivity extends Activity implements ScratchoffController.Delegate {

    public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted) {
        // This will be called on the main thread any time the scratch threshold has changed.
        // The values will be between [0.0, 100.0]
    }

    public void onScratchThresholdReached(ScratchoffController controller) {
        // This is called on the main thread the moment we know the scratched threshold has been reached.
        // If the fade-on-clear animation is enabled, it will already have been started, but not completed.
    }
}

```

### Attaching the `ScratchoffController`

Once we have a layout and delegate setup, we can attach the `ScratchoffController` to it:

```java
// context: android.content.Context
// delegate: ScratchoffController.Delegate

ScratchoffController controller = new ScratchoffController(context)
    .setDelegate(delegate)
    .setThresholdPercent(0.40f)
    .setTouchRadiusDip(context, 30)
    .setClearAnimationEnabled(true)
    .setClearOnThresholdReached(true)
    .attach(findViewById(R.id.scratch_view), findViewById(R.id.scratch_view_behind));
```

In this example, only the constructor and the final `attach(View, View)` method are required to enable scratching. 

Since the foreground `View` in our example is a `ScratchableLinearLayout` (which implements `ScratchableLayout`), the `ScratchoffController` will automatically attach itself to the `View` and drawing will work without any additional setup (the same goes for the `ScratchableRelativeLayout`).

Note: If you're not using one of the supplied `ScratchableLayouts`, you must manually call `ScratchoffController.draw(Canvas)` from your custom View's `onDraw(Canvas)` method.

### Re-using the `ScratchoffController`

The `ScratchoffController` can be reset using `ScratchController.reset()`, but you **must** set the background color of your `ScratchableLayout` back to something opaque before calling it, as the `ScratchableLayoutDrawer` must set it to transparent afterwards in order to efficiently process scratched paths. e.g.

```java
public void onScratchThresholdReached(ScratchoffController controller) {
    // Make sure to set the background of the foreground-View. 
    // Don't worry, it's hidden if it cleared or still clearing.
    findViewById(R.id.scratch_view)
        .setBackgroundColor(0xFF3C9ADF);

    // Reset after a delay, as the clearing animation may still be running at this point
    new Handler(Looper.getMainLooper())
        .postDelayed(() -> controller.reset(), 2000);
}
```

### Lifecycle

Ensure you call the correct lifecycle methods for `onPause()`, `onResume()`, and `onDestroy()`, so that the processors will stop/restart without running needlessly in the background. e.g.

```java
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
```

### Extra: Observing MotionEvents

You can add an `OnTouchListener` to the `ScratchoffController` to observe `MotionEvents` as they come in, regardless of enabled state. When adding these observers, it'd be a good idea to remove them in the appropriate lifecycle methods.

```java
@Override
public void onPause(){
    ...
    controller.removeTouchObservers()

    super.onPause()
}

@Override
public void onResume(){
    super.onResume()

    controller.addTouchObserver((view, event) -> {
        // Do something on a particular MotionEvent?
    });
    ...
}
```

**Note 1**: the return values of `onTouch` will be ignored, as the `ScratchoffController` must maintain control of the touch event collection.
**Note 2**: all touch observers will automatically be removed when calling `ScratchoffController.onDestroy()`

# Upgrading from Version 1.x to Version 2.0.0

Follow the [upgrade guide](https://github.com/jackpocket/android_scratchoff/raw/master/upgrade_1.x-2.0.md).

# Additional credits:
+ [scratchoff-test/src/res/drawable/ic_touch_indicator.xml](https://www.svgrepo.com/svg/9543/touch)