package boundingboxeditor.model.io;

import boundingboxeditor.model.data.ObjectCategory;
import boundingboxeditor.model.io.results.IOResult;
import boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.DoubleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Set;

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
        if(type.equals(Type.PASCAL_VOC)) {
            return new PVOCLoadStrategy();
        } else if(type.equals(Type.YOLO)) {
            return new YOLOLoadStrategy();
        } else if(type.equals(Type.JSON)) {
            return new JSONLoadStrategy();
        } else {
            throw new InvalidParameterException();
        }
    }

    /**
     * Loads image-annotation files from the provided directory path into the
     * the program.
     *
     * @param path        the path of the directory containing the image-annotation files
     * @param filesToLoad the set of files whose annotations can be imported
     * @param progress    the progress-property that will be updated during the loading-operation
     * @return an {@link IOResult} containing information about the finished loading
     * @throws IOException if the directory denoted by the path could not be opened
     */
    ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                     Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                     DoubleProperty progress) throws IOException;

    enum Type {PASCAL_VOC, YOLO, JSON}

    @SuppressWarnings("serial")
    class InvalidAnnotationFormatException extends RuntimeException {
        InvalidAnnotationFormatException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    class AnnotationToNonExistentImageException extends RuntimeException {
        AnnotationToNonExistentImageException(String message) {
            super(message);
        }
    }
}
