/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model;

import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The criteria that decide which image files are shown in the image file list.
 *
 * @param status        which images to show depending on whether they have bounding shapes
 * @param categoryNames the categories of which an image must contain shapes (empty: no category restriction)
 * @param categoryMatch whether an image needs a shape of any or of all the categories
 * @param nameQuery     text the file name must contain, ignoring case (blank: no name restriction)
 */
public record ImageFileFilter(AnnotationStatus status, Set<String> categoryNames, CategoryMatch categoryMatch,
                              String nameQuery) {
    public static final ImageFileFilter NONE = new ImageFileFilter(AnnotationStatus.ALL, Set.of(), CategoryMatch.ANY, "");

    public ImageFileFilter {
        Objects.requireNonNull(status);
        Objects.requireNonNull(categoryMatch);
        categoryNames = Set.copyOf(categoryNames);
        nameQuery = nameQuery == null ? "" : nameQuery.strip();
    }

    /**
     * Returns whether the filter hides any images.
     *
     * @return true if any criterion is set
     */
    public boolean isActive() {
        return status != AnnotationStatus.ALL || !categoryNames.isEmpty() || !nameQuery.isEmpty();
    }

    /**
     * Returns whether an image file matches the filter.
     *
     * @param fileName   the image's file name
     * @param annotation the image's annotation, or null if it has none
     * @return true if the image should be shown
     */
    public boolean matches(String fileName, ImageAnnotation annotation) {
        if(!nameQuery.isEmpty() && !fileName.toLowerCase(Locale.ROOT).contains(nameQuery.toLowerCase(Locale.ROOT))) {
            return false;
        }

        final boolean annotated = annotation != null && !annotation.getBoundingShapeData().isEmpty();

        if((status == AnnotationStatus.ANNOTATED && !annotated) ||
                (status == AnnotationStatus.NOT_ANNOTATED && annotated)) {
            return false;
        }

        if(categoryNames.isEmpty()) {
            return true;
        }

        if(!annotated) {
            return false;
        }

        final Set<String> imageCategoryNames = annotation.getBoundingShapeData().stream()
                                                         .flatMap(BoundingShapeData::flatten)
                                                         .map(BoundingShapeData::getCategoryName)
                                                         .collect(Collectors.toSet());

        return switch(categoryMatch) {
            case ANY -> categoryNames.stream().anyMatch(imageCategoryNames::contains);
            case ALL -> imageCategoryNames.containsAll(categoryNames);
        };
    }

    public enum AnnotationStatus {ALL, ANNOTATED, NOT_ANNOTATED}

    public enum CategoryMatch {ANY, ALL}
}
