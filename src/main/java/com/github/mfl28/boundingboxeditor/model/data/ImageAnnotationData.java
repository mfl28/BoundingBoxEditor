/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Holds {@link ImageAnnotation}s and shape count data.
 */
public record ImageAnnotationData(Collection<ImageAnnotation> imageAnnotations,
                                  Map<String, Integer> categoryNameToBoundingShapeCountMap,
                                  Map<String, ObjectCategory> categoryNameToCategoryMap) {

    public static ImageAnnotationData empty() {
        return new ImageAnnotationData(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }
}
