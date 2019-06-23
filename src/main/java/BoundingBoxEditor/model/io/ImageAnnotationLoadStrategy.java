package BoundingBoxEditor.model.io;

import BoundingBoxEditor.model.Model;
import javafx.beans.property.DoubleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;

/**
 * The interface of an image annotation loading-strategy.
 */
public interface ImageAnnotationLoadStrategy {
    /**
     * Factory method for creating a concrete loading-strategy.
     *
     * @param type the strategy-type specifying the concrete loading-strategy to create
     * @return the loading-strategy with the provided type
     */
    static ImageAnnotationLoadStrategy createStrategy(Type type) {
        switch(type) {
            case PASCAL_VOC:
                return new PVOCLoadStrategy();
            default:
                throw new InvalidParameterException();
        }
    }

    /**
     * Loads image-annotation files from the provided directory path into the
     * the program.
     *
     * @param model    the model-component of the program to load the annotation into
     * @param path     the path of the directory containing the image-annotation files
     * @param progress the progress-property that will be updated during the loading-operation
     * @return an {@link IOResult} containing information about the finished loading
     * @throws IOException
     */
    IOResult load(Model model, Path path, DoubleProperty progress) throws IOException;

    enum Type {PASCAL_VOC}
}
