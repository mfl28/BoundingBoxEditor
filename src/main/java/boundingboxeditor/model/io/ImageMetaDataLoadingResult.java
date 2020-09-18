package boundingboxeditor.model.io;

import java.util.List;

public class ImageMetaDataLoadingResult extends IOResult {
    /**
     * Creates a new io-operation result.
     *
     * @param nrSuccessfullyProcessedItems the number of items (files/annotations) that
     *                                     were successfully processed
     * @param errorTableEntries            a list of objects of type {@link IOErrorInfoEntry} that contain information
     */
    public ImageMetaDataLoadingResult(int nrSuccessfullyProcessedItems,
                                      List<IOErrorInfoEntry> errorTableEntries) {
        super(OperationType.IMAGE_METADATA_LOADING, nrSuccessfullyProcessedItems, errorTableEntries);
    }
}
