package BoundingboxEditor.model.io;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Responsible for saving image-annotations.
 * This class wraps an object of a class that implements {@link ImageAnnotationSaveStrategy} interface and
 * this class determines the actual way of saving the annotations (e.g. different file-types).
 */
public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy saveStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    /**
     * Creates a new image-annotation saver using a {@link ImageAnnotationSaveStrategy} specified
     * by a {@link ImageAnnotationSaveStrategy.Type}.
     *
     * @param strategy the type specifying the concrete strategy to use for saving
     */
    public ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type strategy) {
        saveStrategy = ImageAnnotationSaveStrategy.createStrategy(strategy);
    }

    /**
     * Saves the provided annotation as specified by the wrapped {@link ImageAnnotationSaveStrategy}.
     *
     * @param annotations    the annotations to save
     * @param saveFolderPath the path of the destination folder
     * @return an {@link IOResult} containing information about the finished saving
     */
    public IOResult save(final Collection<ImageAnnotation> annotations, final Path saveFolderPath) {
        return saveStrategy.save(annotations, saveFolderPath, progress);
    }

    /**
     * Returns a property representing the progress of thesaving-operation which can be bound
     * to update the progress of a {@link javafx.concurrent.Service} performing the saving.
     *
     * @return the progress property
     */
    public DoubleProperty progressProperty() {
        return progress;
    }
}
