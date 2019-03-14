package BoundingboxEditor;

import java.nio.file.Path;
import java.util.Collection;

public class ImageAnnotationsSaver {
    private final ImageAnnotationsSaveStrategy saveStrategy;

    public ImageAnnotationsSaver(final ImageAnnotationsSaveStrategy.SaveStrategy strategy) {
        saveStrategy = ImageAnnotationsSaveStrategy.createStrategy(strategy);
    }


    public void save(final Collection<ImageAnnotationDataElement> dataset, final Path saveFolderPath) throws Exception {
        //FIXME: Sometimes no save files are created.
        saveStrategy.save(dataset, saveFolderPath);
    }
}
