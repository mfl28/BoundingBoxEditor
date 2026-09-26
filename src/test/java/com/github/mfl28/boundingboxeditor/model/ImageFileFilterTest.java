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

import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.AnnotationStatus;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.CategoryMatch;
import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ImageFileFilterTest {
    private static final ObjectCategory CAR = new ObjectCategory("car", Color.RED);
    private static final ObjectCategory PERSON = new ObjectCategory("person", Color.BLUE);
    private static final ObjectCategory WHEEL = new ObjectCategory("wheel", Color.GREEN);

    @Test
    void onNoFilter_ShouldMatchEverythingAndBeInactive() {
        assertFalse(ImageFileFilter.NONE.isActive());
        assertTrue(ImageFileFilter.NONE.matches("a.jpg", null));
        assertTrue(ImageFileFilter.NONE.matches("a.jpg", annotation(box(CAR))));
    }

    @Test
    void onAnnotationStatus_ShouldMatchByPresenceOfShapes() {
        final ImageFileFilter annotated = status(AnnotationStatus.ANNOTATED);
        final ImageFileFilter notAnnotated = status(AnnotationStatus.NOT_ANNOTATED);

        assertTrue(annotated.isActive());
        assertTrue(annotated.matches("a.jpg", annotation(box(CAR))));
        assertFalse(annotated.matches("a.jpg", null));
        assertFalse(annotated.matches("a.jpg", annotation()));

        assertTrue(notAnnotated.matches("a.jpg", null));
        assertTrue(notAnnotated.matches("a.jpg", annotation()));
        assertFalse(notAnnotated.matches("a.jpg", annotation(box(CAR))));
    }

    @Test
    void onCategories_ShouldMatchAnyOrAllSelectedCategories() {
        final ImageFileFilter any = new ImageFileFilter(AnnotationStatus.ALL, Set.of("car", "person"),
                CategoryMatch.ANY, "");
        final ImageFileFilter all = new ImageFileFilter(AnnotationStatus.ALL, Set.of("car", "person"),
                CategoryMatch.ALL, "");

        assertTrue(any.matches("a.jpg", annotation(box(CAR))));
        assertTrue(any.matches("a.jpg", annotation(box(PERSON), box(WHEEL))));
        assertFalse(any.matches("a.jpg", annotation(box(WHEEL))));
        assertFalse(any.matches("a.jpg", null));

        assertFalse(all.matches("a.jpg", annotation(box(CAR))));
        assertTrue(all.matches("a.jpg", annotation(box(CAR), box(PERSON), box(WHEEL))));
    }

    @Test
    void onCategories_ShouldConsiderNestedParts() {
        final BoundingShapeData car = box(CAR);
        car.setParts(List.of(box(WHEEL)));
        final ImageFileFilter wheels = new ImageFileFilter(AnnotationStatus.ALL, Set.of("wheel"), CategoryMatch.ANY, "");

        assertTrue(wheels.matches("a.jpg", annotation(car)));
    }

    @Test
    void onNameQuery_ShouldMatchContainedTextIgnoringCase() {
        final ImageFileFilter name = new ImageFileFilter(AnnotationStatus.ALL, Set.of(), CategoryMatch.ANY, " Nico ");

        assertTrue(name.isActive());
        assertTrue(name.matches("nico-bhlr-unsplash.jpg", null));
        assertTrue(name.matches("photo_NICO.png", null));
        assertFalse(name.matches("austin-neill.jpg", null));
    }

    @Test
    void onCombinedCriteria_ShouldRequireAllOfThem() {
        final ImageFileFilter filter = new ImageFileFilter(AnnotationStatus.ANNOTATED, Set.of("car"),
                CategoryMatch.ANY, "street");

        assertTrue(filter.matches("street-1.jpg", annotation(box(CAR))));
        assertFalse(filter.matches("field-1.jpg", annotation(box(CAR))));
        assertFalse(filter.matches("street-2.jpg", annotation(box(PERSON))));
    }

    @Test
    void onNullNameQuery_ShouldBeTreatedAsEmpty() {
        assertFalse(new ImageFileFilter(AnnotationStatus.ALL, Set.of(), CategoryMatch.ANY, null).isActive());
    }

    private static ImageFileFilter status(AnnotationStatus status) {
        return new ImageFileFilter(status, Set.of(), CategoryMatch.ANY, "");
    }

    private static BoundingShapeData box(ObjectCategory category) {
        return new BoundingBoxData(category, 0.1, 0.1, 0.5, 0.5, List.of());
    }

    private static ImageAnnotation annotation(BoundingShapeData... shapes) {
        return new ImageAnnotation(new ImageMetaData("a.jpg"), List.of(shapes));
    }
}
