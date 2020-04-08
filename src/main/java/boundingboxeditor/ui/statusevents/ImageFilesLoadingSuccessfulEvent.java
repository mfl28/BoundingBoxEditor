package boundingboxeditor.ui.statusevents;

import boundingboxeditor.model.io.IOResult;

import java.io.File;

/**
 * Represents the event of a successful loading of image-files from a folder.
 */
public class ImageFilesLoadingSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful loading of image-files.
     *
     * @param ioResult             result of operation
     * @param loadedImageDirectory the directory from which the image-files were loaded
     */
    public ImageFilesLoadingSuccessfulEvent(IOResult ioResult, File loadedImageDirectory) {
        super("Successfully loaded " + ioResult.getNrSuccessfullyProcessedItems() + " image-file" +
                (ioResult.getNrSuccessfullyProcessedItems() != 1 ? "s" : "") + " from folder " + loadedImageDirectory.getPath()
                + " in "
                + secondsFormat.format(ioResult.getTimeTakenInMilliseconds() / 1000.0) + " sec.");
    }
}
