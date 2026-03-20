# Upgrading from version 3.x to version 4.0.0

Version 3.x | Version 4.0.0
--- | ---
`ScratchoffController.setUsePreDrawOverGlobalLayoutEnabled(boolean)` | None
`ScratchoffController.setAttemptLastDitchPostForLayoutResolutionFailure(boolean)` | None
`ScratchoffController.setKeepListeningForDrawUntilValidSizeDiscovered(boolean)` | None

With 4.x, it's also important that `ScratchoffController.onDestroy` is properly called to ensure the strongly-referenced `OnGlobalLayoutListener` is removed from the `ViewTreeObserver`.