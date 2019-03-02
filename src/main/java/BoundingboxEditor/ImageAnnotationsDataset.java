package BoundingboxEditor;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class ImageAnnotationsDataset implements Iterable<ImageAnnotationDataElement> {
    private final List<ImageAnnotationDataElement> dataElements;

    public ImageAnnotationsDataset(final List<ImageAnnotationDataElement> dataElements) {
        this.dataElements = dataElements;
    }

    @Override
    public Iterator<ImageAnnotationDataElement> iterator() {
        return dataElements.iterator();
    }

    @Override
    public void forEach(Consumer<? super ImageAnnotationDataElement> action) {
        dataElements.forEach(action);
    }
}
