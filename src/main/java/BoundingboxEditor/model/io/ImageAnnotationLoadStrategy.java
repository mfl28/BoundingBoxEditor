package BoundingboxEditor.model.io;

import BoundingboxEditor.model.Model;
import javafx.beans.property.DoubleProperty;

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

    IOResult load(final Model model, final Path path, final DoubleProperty progress) throws IOException;

    enum Type {PASCAL_VOC}
}
