package BoundingboxEditor.model.io;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Base interface for all image loader implementations. An ImageAnnotationLoader
 * is responsible for loading existing image annotation data from disk.
 */
public class ImageAnnotationLoader {
    private final ImageAnnotationLoadStrategy loadStrategy;

    public ImageAnnotationLoader(final ImageAnnotationLoadStrategy.Type strategy) throws ParserConfigurationException {
        loadStrategy = ImageAnnotationLoadStrategy.createStrategy(strategy);
    }


    public List<ImageAnnotationDataElement> load(final Set<String> fileNamesToLoad, final Path saveFolderPath) throws IOException {
        return loadStrategy.load(fileNamesToLoad, saveFolderPath);
    }
}
