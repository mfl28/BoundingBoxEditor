package BoundingboxEditor.model.io;

import java.nio.file.Path;
import java.util.Collection;

public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy saveStrategy;

    public ImageAnnotationSaver(final ImageAnnotationSaveStrategy.SaveStrategy strategy) {
        saveStrategy = ImageAnnotationSaveStrategy.createStrategy(strategy);
    }

    public void save(final Collection<ImageAnnotationDataElement> annotations, final Path saveFolderPath) throws Exception {
        saveStrategy.save(annotations, saveFolderPath);
    }
}
