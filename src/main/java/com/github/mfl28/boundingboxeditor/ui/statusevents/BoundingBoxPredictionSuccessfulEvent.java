package com.github.mfl28.boundingboxeditor.ui.statusevents;

import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;

public class BoundingBoxPredictionSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful completion of a bounding box prediction.
     *
     * @param ioResult an {@link IOResult} object containing information about the import-operation
     */
    public BoundingBoxPredictionSuccessfulEvent(BoundingBoxPredictionResult ioResult) {
        super(createMessage(ioResult));
    }

    private static String createMessage(BoundingBoxPredictionResult ioResult) {
        int predictedBoundingBoxes = ioResult.getImageAnnotationData()
                                             .getCategoryNameToBoundingShapeCountMap()
                                             .values()
                                             .stream()
                                             .reduce(Integer::sum)
                                             .orElse(0);

        return "Successfully predicted "
                + predictedBoundingBoxes
                + " bounding box" + (predictedBoundingBoxes != 1 ? "es" : "") + " for "
                + ioResult.getNrSuccessfullyProcessedItems() + " image" +
                (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "")
                + " in "
                + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.";
    }
}
