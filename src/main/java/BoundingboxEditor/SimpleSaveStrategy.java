package BoundingboxEditor;

import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiConsumer;

public class SimpleSaveStrategy implements ImageAnnotationsSaveStrategy {
    @Override
    public void save(final Collection<ImageAnnotationDataElement> dataset, final Path savePath) throws Exception {
        System.out.println("Saving with SimpleSave Strategy.");
    }
}
