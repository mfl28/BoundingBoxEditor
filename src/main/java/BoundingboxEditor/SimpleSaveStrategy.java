package BoundingboxEditor;

import jdk.jshell.spi.ExecutionControl;

import java.nio.file.Path;
import java.util.Collection;

public class SimpleSaveStrategy implements ImageAnnotationsSaveStrategy {
    @Override
    public void save(final Collection<ImageAnnotationDataElement> dataSet, final Path savePath) throws Exception {
        throw new ExecutionControl.NotImplementedException("not implemented");
    }
}
