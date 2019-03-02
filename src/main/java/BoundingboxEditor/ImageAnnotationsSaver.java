package BoundingboxEditor;

import java.nio.file.Path;

public class ImageAnnotationsSaver {
    private final ImageAnnotationsSaveStrategy saveStrategy;

    public ImageAnnotationsSaver(final ImageAnnotationsSaveStrategy.SaveStrategy strategy){
        saveStrategy = ImageAnnotationsSaveStrategy.createStrategy(strategy);
    }

    public void save(final ImageAnnotationsDataset dataset, final Path saveFolderPath) throws Exception{
        saveStrategy.save(dataset, saveFolderPath);
    }
}
