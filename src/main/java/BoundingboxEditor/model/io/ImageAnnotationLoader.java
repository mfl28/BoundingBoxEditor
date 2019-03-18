package BoundingboxEditor.model.io;

import java.nio.file.Path;

/**
 * Base interface for all image loader implementations. An ImageAnnotationLoader
 * is responsible for loading existing image annotation data from disk.
 */
public interface ImageAnnotationLoader {
    void load(final Path path);
}
