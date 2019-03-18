package BoundingboxEditor.model.io;

import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Collection;

public interface ImageAnnotationsSaveStrategy {
    static ImageAnnotationsSaveStrategy createStrategy(final SaveStrategy saveStrategy) {
        ImageAnnotationsSaveStrategy strategy;

        switch(saveStrategy) {
            case PASCAL_VOC:
                strategy = new PVOCSaveStrategy();
                break;
            case SIMPLE_SAVE:
                strategy = new SimpleSaveStrategy();
                break;
            default:
                throw new InvalidParameterException();
        }

        return strategy;
    }

    void save(final Collection<ImageAnnotationDataElement> dataset, final Path savePath) throws Exception;

    enum SaveStrategy {PASCAL_VOC, SIMPLE_SAVE}
}
