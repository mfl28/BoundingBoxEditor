package com.github.mfl28.boundingboxeditor.ui.statusevents;

import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;

public class BoundingBoxPredictionSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful completion of a bounding box prediction.
     *
     * @param ioResult an {@link IOResult} object containing information about the import-operation
     */
    public BoundingBoxPredictionSuccessfulEvent(IOResult ioResult) {
        super("Successfully predicted bounding boxes for "
                      + ioResult.getNrSuccessfullyProcessedItems() + " image" +
                      (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "")
                      + " in "
                      + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
