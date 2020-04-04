package boundingboxeditor.model.io;

import java.util.Collection;
import java.util.Map;

public class ImageAnnotationData {
    private final Collection<ImageAnnotation> imageAnnotations;
    private final Map<String, Integer> categoryToShapeCountMap;

    public ImageAnnotationData(Collection<ImageAnnotation> imageAnnotations,
                               Map<String, Integer> categoryToShapeCountMap) {
        this.imageAnnotations = imageAnnotations;
        this.categoryToShapeCountMap = categoryToShapeCountMap;
    }

    public Collection<ImageAnnotation> getImageAnnotations() {
        return imageAnnotations;
    }

    public Map<String, Integer> getCategoryToShapeCountMap() {
        return categoryToShapeCountMap;
    }
}
