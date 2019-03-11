package BoundingboxEditor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiConsumer;

public class ImageAnnotationsSaver {
    private final ImageAnnotationsSaveStrategy saveStrategy;

    public ImageAnnotationsSaver(final ImageAnnotationsSaveStrategy.SaveStrategy strategy) {
        saveStrategy = ImageAnnotationsSaveStrategy.createStrategy(strategy);
    }



    public void save(final Collection<ImageAnnotationDataElement> dataset, final Path saveFolderPath) throws Exception {
        saveStrategy.save(dataset, saveFolderPath);
    }
}
