package boundingboxeditor.model.io;

import boundingboxeditor.model.ObjectCategory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Holds {@link ImageAnnotation}s and shape count data.
 */
public class ImageAnnotationData {
    private final Collection<ImageAnnotation> imageAnnotations;
    private final Map<String, Integer> categoryNameToBoundingShapeCountMap;
    private final Map<String, ObjectCategory> categoryNameToCategoryMap;

    /**
     * Creates a new image annotation data object.
     *
     * @param imageAnnotations                    the collection of {@link ImageAnnotation}s
     * @param categoryNameToBoundingShapeCountMap a map that maps category names to the number of assigned shapes
     * @param categoryNameToCategoryMap           a map that maps category names to {@link ObjectCategory} objects
     */
    public ImageAnnotationData(Collection<ImageAnnotation> imageAnnotations,
                               Map<String, Integer> categoryNameToBoundingShapeCountMap,
                               Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.imageAnnotations = imageAnnotations;
        this.categoryNameToBoundingShapeCountMap = categoryNameToBoundingShapeCountMap;
        this.categoryNameToCategoryMap = categoryNameToCategoryMap;
    }

    public static ImageAnnotationData empty() {
        return new ImageAnnotationData(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
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
    public Map<String, Integer> getCategoryNameToBoundingShapeCountMap() {
        return categoryNameToBoundingShapeCountMap;
    }

    /**
     * Returns the category-name to category object map.
     *
     * @return the map
     */
    public Map<String, ObjectCategory> getCategoryNameToCategoryMap() {
        return categoryNameToCategoryMap;
    }
}
