package BoundingboxEditor.ui.StatusEvents;

import BoundingboxEditor.model.io.IOResult;

/**
 * Represents the event of a successful image-annotation import.
 */
public class ImageAnnotationsImportingSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful completion of an image-annotation import.
     *
     * @param ioResult an {@link IOResult} object containing information about the import-operation
     */
    public ImageAnnotationsImportingSuccessfulEvent(IOResult ioResult) {
        super("Successfully imported annotations from "
                + ioResult.getNrSuccessfullyProcessedItems() + " files in "
                + String.format("%.3f", ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
