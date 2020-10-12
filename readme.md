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

First, you need a parent `ViewGroup` that can vertically stack: 

1. a behind-View to be revealed
2. a foreground-View to be scratched away

Here is an example using the `ScratchableLinearLayout`:

```xml
<RelativeLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent" >

    <RelativeLayout
        android:id="@+id/scratch_view_behind"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#818B8D"
        android:padding="25dip" >

        <ImageView
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:adjustViewBounds="true"
            android:src="@drawable/some_drawable_to_be_revealed" />

    </RelativeLayout>

    <com.jackpocket.scratchoff.views.ScratchableLinearLayout
        android:id="@+id/scratch_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#3C9ADF"
        android:padding="25dip" >

        <ImageView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:adjustViewBounds="true"
            android:gravity="center"
            android:src="@drawable/some_top_drawable" />

    </com.jackpocket.scratchoff.views.ScratchableLinearLayout>
</RelativeLayout>
```

By default, the `ScratchoffController` will not adjust the width or height of the scratchable layout's `LayoutParams`. To enable this behavior, call `setMatchLayoutWithBehindView(View)` with the behind-View whose width/height should be matched with before `attach()`.

### Scratch Threshold Changed / Threshold Reached Callback Setup

The `ScratchoffController` will call its listener's methods when the scratched threshold has changed or has been reached, but you need to maintain a strong reference to the supplied `ScratchoffController.ThresholdChangedListener` instance.

```java
public class MainActivity extends Activity implements ScratchoffController.ThresholdChangedListener {

    @Override
    public void onScratchPercentChanged(ScratchoffController controller, float percentCompleted) {
        // This will be called on the main thread any time the scratch threshold has changed.
        // The values will be between [0.0, 100.0]
    }

    @Override
    public void onScratchThresholdReached(ScratchoffController controller) {
        // This is called on the main thread the moment we know the scratched threshold has been reached.
        // If the fade-on-clear animation is enabled, it will already have been started, but not completed.
    }
}

```

### Attaching the `ScratchoffController`

Once we have a layout and listeners setup, we can attach the `ScratchoffController` to it to start scratching away:

```java
// activity: android.app.Activity
// listener: ScratchoffController.ThresholdChangedListener

ScratchoffController.findByViewId(activity, R.id.scratch_view)
    .setThresholdChangedListener(listener)
    .setThresholdPercent(0.40f)
    .setTouchRadiusDip(context, 30)
    .setClearAnimationEnabled(true)
    .setClearOnThresholdReached(true)
    .attach();
```

In this example, only the final `attach()` method is required to enable scratching, but you must set a `ThresholdChangedListener` to receive updates. 

If you choose not to utilize the `ScratchableLinearLayout` or the `ScratchableRelativeLayout`, you will need to manually handle delegation to `ScratchoffController.onDraw(Canvas)`.

### Re-using the `ScratchoffController`

The `ScratchoffController` can be reset with the same call that started it: `ScratchController.attach()`. However, you **must** manually set the background color of your scratchable layout back to something opaque before calling it, as the `ScratchableLayoutDrawer` will set it to transparent afterwards in order to efficiently process scratched paths. e.g.

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