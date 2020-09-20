package boundingboxeditor.model.data;

import java.io.File;

/**
 * Metadata related to IO operations.
 */
public class IoMetaData {
    private File defaultImageLoadingDirectory = null;
    private File defaultAnnotationSavingDirectory = null;
    private File defaultAnnotationLoadingDirectory = null;

    /**
     * Get the currently set default directory for image loading.
     *
     * @return the directory
     */
    public File getDefaultImageLoadingDirectory() {
        return defaultImageLoadingDirectory;
    }

    /**
     * Set the current default directory for image loading.
     *
     * @param defaultImageLoadingDirectory the directory
     */
    public void setDefaultImageLoadingDirectory(File defaultImageLoadingDirectory) {
        this.defaultImageLoadingDirectory = defaultImageLoadingDirectory;
    }

    /**
     * Get the currently set default directory for annotation saving.
     *
     * @return the directory
     */
    public File getDefaultAnnotationSavingDirectory() {
        return defaultAnnotationSavingDirectory;
    }

    /**
     * Set the current default directory for annotation saving.
     *
     * @param defaultAnnotationSavingDirectory the directory
     */
    public void setDefaultAnnotationSavingDirectory(File defaultAnnotationSavingDirectory) {
        this.defaultAnnotationSavingDirectory = defaultAnnotationSavingDirectory;
    }

    /**
     * Get the currently set default directory for annotation loading.
     *
     * @return the directory
     */
    public File getDefaultAnnotationLoadingDirectory() {
        return defaultAnnotationLoadingDirectory;
    }

    /**
     * Set the current default directory for annotation loading.
     *
     * @param defaultAnnotationLoadingDirectory the directory
     */
    public void setDefaultAnnotationLoadingDirectory(File defaultAnnotationLoadingDirectory) {
        this.defaultAnnotationLoadingDirectory = defaultAnnotationLoadingDirectory;
    }
}
