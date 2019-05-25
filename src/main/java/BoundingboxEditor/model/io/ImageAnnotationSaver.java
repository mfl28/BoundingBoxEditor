package BoundingboxEditor.model.io;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.nio.file.Path;
import java.util.Collection;

public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy saveStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    public ImageAnnotationSaver(final ImageAnnotationSaveStrategy.Type strategy) {
        saveStrategy = ImageAnnotationSaveStrategy.createStrategy(strategy);
    }

    public IOResult save(final Collection<ImageAnnotationDataElement> annotations, final Path saveFolderPath) {
        return saveStrategy.save(annotations, saveFolderPath, progress);
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }
}
