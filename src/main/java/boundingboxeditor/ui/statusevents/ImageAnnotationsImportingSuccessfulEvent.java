package boundingboxeditor.ui.statusevents;

import boundingboxeditor.model.io.IOResult;

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
                + ioResult.getNrSuccessfullyProcessedItems() + " file" + (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "")
                + " in "
                + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
