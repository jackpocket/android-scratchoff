# Upgrading from version 1.x to version 2.0.0

Version 1.x | Version 2.0.0
--- | --- 
`new ScratchoffController(Context, Runnable)` | `new ScratchoffController(Context, ScratchoffController.Delegate)`
`ScratchoffController.setCompletionCallback(Runnable)` | `ScratchoffController.setDelegate(ScratchoffController.Delegate)`
`ScratchoffController.setScratchValuedChangedListener(ScratchValueChangedListener)` | `ScratchoffController.setDelegate(ScratchoffController.Delegate)`
`ScratchoffController.addPaths(List<Path>`) | `ScratchoffController.enqueueScratchMotionEvents(List<ScratchPathPoint>)`
`ScratchoffController.isProcessingAllowed()` | None
`int ScratchoffController.getTotalGridItemsCount()` | `int[] ScratchoffController.getScratchableLayoutSize()`
`ScratchValueChangedListener.onScratchPercentChanged(double)` | `ScratchoffController.Delegate.onScratchPercentChanged(ScratchoffController, float)`

The `ScratchoffController`'s threshold completed/changed callbacks have been merged into a weakly-held `ScratchoffController.Delegate`