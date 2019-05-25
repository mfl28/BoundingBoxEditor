package BoundingboxEditor.model.io;

import javafx.beans.property.DoubleProperty;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Collection;

public interface ImageAnnotationSaveStrategy {
    static ImageAnnotationSaveStrategy createStrategy(final Type type) {
        ImageAnnotationSaveStrategy strategy;

        switch(type) {
            case PASCAL_VOC:
                strategy = new PVOCSaveStrategy();
                break;
            default:
                throw new InvalidParameterException();
        }

        return strategy;
    }

    IOResult save(final Collection<ImageAnnotationDataElement> dataset, final Path savePath, final DoubleProperty progress);

    enum Type {PASCAL_VOC}
}
