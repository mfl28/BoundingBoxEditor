package BoundingboxEditor.model.io;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Collection;

public interface ImageAnnotationSaveStrategy {
    static ImageAnnotationSaveStrategy createStrategy(final SaveStrategy saveStrategy) {
        ImageAnnotationSaveStrategy strategy;

        switch(saveStrategy) {
            case PASCAL_VOC:
                strategy = new PVOCSaveStrategy();
                break;
            default:
                throw new InvalidParameterException();
        }

        return strategy;
    }

    void save(final Collection<ImageAnnotationDataElement> dataset, final Path savePath) throws Exception;

    enum SaveStrategy {PASCAL_VOC}
}
