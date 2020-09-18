package boundingboxeditor.model.io;

import java.util.List;

public class ImageAnnotationImportResult extends IOResult {
    private final ImageAnnotationData imageAnnotationData;

    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     */
    public ImageAnnotationImportResult(int nrSuccessfullyProcessedItems,
                                       List<IOErrorInfoEntry> errorTableEntries,
                                       ImageAnnotationData imageAnnotationData) {
        super(OperationType.ANNOTATION_IMPORT, nrSuccessfullyProcessedItems, errorTableEntries);
        this.imageAnnotationData = imageAnnotationData;
    }

    public ImageAnnotationData getImageAnnotationData() {
        return imageAnnotationData;
    }
}
