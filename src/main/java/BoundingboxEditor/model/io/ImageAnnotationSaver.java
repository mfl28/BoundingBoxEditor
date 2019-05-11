package BoundingboxEditor.model.io;

import java.nio.file.Path;
import java.util.Collection;

public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy saveStrategy;

    public ImageAnnotationSaver(final ImageAnnotationSaveStrategy.SaveStrategy strategy) {
        saveStrategy = ImageAnnotationSaveStrategy.createStrategy(strategy);
    }

    public void save(final Collection<ImageAnnotationDataElement> dataset, final Path saveFolderPath) throws Exception {
        //FIXME: Sometimes no save files are created.
        saveStrategy.save(dataset, saveFolderPath);
    }
}
