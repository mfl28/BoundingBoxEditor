import java.nio.file.Path;

public class SimpleSaveStrategy implements ImageAnnotationsSaveStrategy {
    @Override
    public void save(final ImageAnnotationsDataset dataset, final Path savePath) throws Exception {
        System.out.println("Saving with SimpleSave Strategy.");
    }
}
