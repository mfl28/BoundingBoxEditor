package BoundingboxEditor.model.io;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

public interface ImageAnnotationLoadStrategy {
    static ImageAnnotationLoadStrategy createStrategy(final Type type) throws ParserConfigurationException {
        switch(type) {
            case PASCAL_VOC:
                return new PVOCLoadStrategy();
            default:
                throw new InvalidParameterException();
        }
    }

    List<ImageAnnotationDataElement> load(final Set<String> fileNamesToLoad, final Path path) throws IOException;

    enum Type {PASCAL_VOC}
}
