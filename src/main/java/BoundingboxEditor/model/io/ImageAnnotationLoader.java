package BoundingboxEditor.model.io;

import java.nio.file.Path;
import java.util.List;

/**
 * Base interface for all image loader implementations. An ImageAnnotationLoader
 * is responsible for loading existing image annotation data from disk.
 */
public interface ImageAnnotationLoader {
    List<ImageAnnotationDataElement> load(final Path path) throws Exception;
}
