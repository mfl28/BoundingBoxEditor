package BoundingboxEditor.model.io;

import BoundingboxEditor.model.Model;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;

public interface ImageAnnotationLoadStrategy {
    static ImageAnnotationLoadStrategy createStrategy(final Type type) {
        switch(type) {
            case PASCAL_VOC:
                return new PVOCLoadStrategy();
            default:
                throw new InvalidParameterException();
        }
    }

    ImageAnnotationLoader.LoadResult load(final Model model, final Path path) throws IOException;

    enum Type {PASCAL_VOC}
}
