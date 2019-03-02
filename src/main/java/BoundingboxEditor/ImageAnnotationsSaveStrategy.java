package BoundingboxEditor;

import java.nio.file.Path;
import java.security.InvalidParameterException;

public interface ImageAnnotationsSaveStrategy {
    enum SaveStrategy {PASCAL_VOC, SIMPLE_SAVE}

    void save(final ImageAnnotationsDataset dataset, final Path savePath) throws Exception;

    static ImageAnnotationsSaveStrategy createStrategy(final SaveStrategy saveStrategy){
        ImageAnnotationsSaveStrategy strategy;

        switch (saveStrategy){
            case PASCAL_VOC:
                strategy = new PVOCSaveStrategy();
                break;
            case SIMPLE_SAVE:
                strategy =  new SimpleSaveStrategy();
                break;
            default:
                throw new InvalidParameterException();
        }

        return strategy;
    }
}
