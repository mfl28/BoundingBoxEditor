package boundingboxeditor.model.io.results;

import java.util.List;

public class ImageAnnotationExportResult extends IOResult {
    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     */
    public ImageAnnotationExportResult(int nrSuccessfullyProcessedItems,
                                       List<IOErrorInfoEntry> errorTableEntries) {
        super(OperationType.ANNOTATION_SAVING, nrSuccessfullyProcessedItems, errorTableEntries);
    }
}
