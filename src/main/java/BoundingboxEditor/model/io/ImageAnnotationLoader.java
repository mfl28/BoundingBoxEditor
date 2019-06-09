package BoundingboxEditor.model.io;

import BoundingboxEditor.model.Model;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Responsible for loading image-annotation files from a directory into the model-component of
 * the program. This class wraps an object of a class that implements {@link ImageAnnotationLoadStrategy} interface and
 * this class determines the actual way of loading the annotations (e.g. different file-types).
 */
public class ImageAnnotationLoader {
    private final ImageAnnotationLoadStrategy loadStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    /**
     * Creates a new image-annotation loader using a {@link ImageAnnotationLoadStrategy} specified
     * by a {@link ImageAnnotationLoadStrategy.Type}.
     *
     * @param strategy the type specifying the concrete strategy to use for the loading
     */
    public ImageAnnotationLoader(final ImageAnnotationLoadStrategy.Type strategy) {
        loadStrategy = ImageAnnotationLoadStrategy.createStrategy(strategy);
    }

    /**
     * Loads image-annotation files as specified by the wrapped {@link ImageAnnotationLoadStrategy}
     * into the model-component of the program.
     *
     * @param model                 the model-component of the program
     * @param annotationsFolderPath the path of the folder containing the image-annotation files
     * @return an {@link IOResult} containing information about the finished loading
     * @throws IOException
     */
    public IOResult load(final Model model, final Path annotationsFolderPath) throws IOException {
        return loadStrategy.load(model, annotationsFolderPath, progress);
    }

    /**
     * Returns a property representing the progress of the loading-operation which can be bound
     * to update the progress of a {@link javafx.concurrent.Service} performing the loading.
     *
     * @return the progress property
     */
    public DoubleProperty progressProperty() {
        return progress;
    }
}
