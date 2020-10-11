package com.github.mfl28.boundingboxeditor.model.io.results;

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;

import java.util.List;

public class BoundingBoxPredictionResult extends IOResult {
    private final ImageAnnotationData imageAnnotationData;

    /**
     * Creates a new bounding box prediction result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     *                                     about where and which errors occurred.
     * @param imageAnnotationData the predicted annotation data
     */
    public BoundingBoxPredictionResult(int nrSuccessfullyProcessedItems,
                                          List<IOErrorInfoEntry> errorTableEntries,
                                          ImageAnnotationData imageAnnotationData) {
        super(OperationType.BOUNDING_BOX_PREDICTION, nrSuccessfullyProcessedItems, errorTableEntries);
        this.imageAnnotationData = imageAnnotationData;
    }

    public ImageAnnotationData getImageAnnotationData() {
        return imageAnnotationData;
    }
}
