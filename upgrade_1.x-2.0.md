# Upgrading from version 1.x to version 2.0.0

Version 1.x | Version 2.0.0
--- | --- 
`new ScratchoffController(Context, Runnable)` | `new ScratchoffController(Context, ScratchoffController.Delegate)`
`ScratchoffController.setCompletionCallback(Runnable)` | `ScratchoffController.setDelegate(ScratchoffController.Delegate)`
`ScratchoffController.setScratchValuedChangedListener(ScratchValueChangedListener)` | `ScratchoffController.setDelegate(ScratchoffController.Delegate)`
`ScratchoffController.setThresholdPercent(double)` | `ScratchoffController.setThresholdPercent(float)`
`ScratchoffController.setFadeOnClear(boolean)` | `ScratchoffController.setClearAnimationEnabled(float)`
`ScratchoffController.addPaths(List<Path>`) | None
`ScratchoffController.isProcessingAllowed()` | None
`int ScratchoffController.getTotalGridItemsCount()` | `int[] ScratchoffController.getScratchableLayoutSize()`
`ScratchValueChangedListener.onScratchPercentChanged(double)` | `ScratchoffController.Delegate.onScratchPercentChanged(ScratchoffController, float)`
`R.bool.scratch__fade_on_clear` | `R.bool.scratch__clear_animation_enabled`

The `ScratchoffController`'s threshold completed/changed callbacks have been merged into a weakly-held `ScratchoffController.Delegate`. The `Delegate` instance can be defined when instantiating the `ScratchoffController`, or by calling `setDelegate` on the instance.