package BoundingboxEditor.model.io;

import BoundingboxEditor.model.Model;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base interface for all image loader implementations. An ImageAnnotationLoader
 * is responsible for loading existing image annotation data from disk.
 */
public class ImageAnnotationLoader {
    private final ImageAnnotationLoadStrategy loadStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    public ImageAnnotationLoader(final ImageAnnotationLoadStrategy.Type strategy) {
        loadStrategy = ImageAnnotationLoadStrategy.createStrategy(strategy);
    }

    public IOResult load(final Model model, final Path saveFolderPath) throws IOException {
        return loadStrategy.load(model, saveFolderPath, progress);
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }
}
