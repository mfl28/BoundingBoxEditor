/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package boundingboxeditor.model.data;

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
