package boundingboxeditor.ui.statusevents;

import boundingboxeditor.model.io.IOResult;

/**
 * Represents the event of a successful image-annotation save.
 */
public class ImageAnnotationsSavingSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful completion
     * of an image-annotation saving operation.
     *
     * @param ioResult an {@link IOResult} object containing information about the save-operation
     */
    public ImageAnnotationsSavingSuccessfulEvent(IOResult ioResult) {
        super("Successfully saved " + ioResult.getNrSuccessfullyProcessedItems() + " image-annotation"
                + (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "") + " in "
                + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
