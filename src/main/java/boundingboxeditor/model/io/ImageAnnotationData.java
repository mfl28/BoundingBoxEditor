package boundingboxeditor.model.io;

import java.util.Collection;
import java.util.Map;

/**
 * Holds {@link ImageAnnotation}s and shape count data.
 */
public class ImageAnnotationData {
    private final Collection<ImageAnnotation> imageAnnotations;
    private final Map<String, Integer> categoryToShapeCountMap;

    /**
     * Creates a new image annotation data object.
     *
     * @param imageAnnotations        the collection of {@link ImageAnnotation}s
     * @param categoryToShapeCountMap a map that maps category names to the number of assigned shapes
     */
    public ImageAnnotationData(Collection<ImageAnnotation> imageAnnotations,
                               Map<String, Integer> categoryToShapeCountMap) {
        this.imageAnnotations = imageAnnotations;
        this.categoryToShapeCountMap = categoryToShapeCountMap;
    }

    /**
     * Returns the image annotations.
     *
     * @return the image annotations
     */
    public Collection<ImageAnnotation> getImageAnnotations() {
        return imageAnnotations;
    }

    /**
     * Returns the category to assigned shapes count map.
     *
     * @return the map
     */
    public Map<String, Integer> getCategoryToShapeCountMap() {
        return categoryToShapeCountMap;
    }
}
