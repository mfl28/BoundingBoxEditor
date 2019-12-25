package boundingboxeditor.ui.statusevents;

import java.io.File;

/**
 * Represents the event of a successful loading of image-files from a folder.
 */
public class ImageFilesLoadingSuccessfulEvent extends StatusEvent {
    /**
     * Creates a new status-event signifying the successful loading of image-files.
     *
     * @param nrFilesLoaded        the number of successfully loaded files
     * @param loadedImageDirectory the directory from which the image-files were loaded
     */
    public ImageFilesLoadingSuccessfulEvent(int nrFilesLoaded, File loadedImageDirectory) {
        super("Successfully loaded " + nrFilesLoaded + " image-file" +
                (nrFilesLoaded != 1 ? "s" : "") + " from folder " + loadedImageDirectory.getPath() + ".");
    }
}
