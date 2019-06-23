package BoundingBoxEditor.model.io;

import javafx.beans.property.DoubleProperty;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Collection;

/**
 * The interface of an image annotation saving-strategy.
 */
public interface ImageAnnotationSaveStrategy {
    /**
     * Factory method for creating a saving-strategy.
     *
     * @param type the strategy-type specifying the concrete saving-strategy to create
     * @return the saving-strategy with the provided type
     */
    static ImageAnnotationSaveStrategy createStrategy(Type type) {
        switch(type) {
            case PASCAL_VOC:
                return new PVOCSaveStrategy();
            default:
                throw new InvalidParameterException();
        }
    }

    /**
     * Saves image-annotations to the provided folder-path.
     *
     * @param annotations    the collection of image-annotations to save
     * @param saveFolderPath the path of the directory to which the annotations will be saved
     * @param progress       the progress-property that will be updated during the saving-operation
     * @return an {@link IOResult} containing information about the finished saving
     */
    IOResult save(Collection<ImageAnnotation> annotations, Path saveFolderPath, DoubleProperty progress);

    enum Type {PASCAL_VOC}
}
