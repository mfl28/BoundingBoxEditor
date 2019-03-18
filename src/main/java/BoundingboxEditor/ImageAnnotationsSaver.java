package BoundingboxEditor;

import java.nio.file.Path;
import java.util.Collection;

class ImageAnnotationsSaver {
    private final ImageAnnotationsSaveStrategy saveStrategy;

    ImageAnnotationsSaver(final ImageAnnotationsSaveStrategy.SaveStrategy strategy) {
        saveStrategy = ImageAnnotationsSaveStrategy.createStrategy(strategy);
    }


    void save(final Collection<ImageAnnotationDataElement> dataset, final Path saveFolderPath) throws Exception {
        //FIXME: Sometimes no save files are created.
        saveStrategy.save(dataset, saveFolderPath);
    }
}
