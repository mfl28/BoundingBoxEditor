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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.model.ImageFileFilter;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.AnnotationStatus;
import com.github.mfl28.boundingboxeditor.model.ImageFileFilter.CategoryMatch;
import com.github.mfl28.boundingboxeditor.model.Model;
import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.ui.ImageFileListView.FileInfo;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the filtering of the image file list and the navigation within it, with a real model.
 */
@Tag("unit")
class ImageFileFilterControllerTest {
    private static final ObjectCategory CAR = new ObjectCategory("car", Color.RED);
    private static final ImageFileFilter ANNOTATED =
            new ImageFileFilter(AnnotationStatus.ANNOTATED, Set.of(), CategoryMatch.ANY, "");
    private static final ImageFileFilter NOT_ANNOTATED =
            new ImageFileFilter(AnnotationStatus.NOT_ANNOTATED, Set.of(), CategoryMatch.ANY, "");

    private final Model model = new Model();
    private final ImageFileFilterController filterController = new ImageFileFilterController(model);
    private List<FileInfo> items;

    @BeforeEach
    void setUp() {
        items = IntStream.range(0, 5)
                         .mapToObj(index -> new FileInfo("file:/image" + index + ".jpg", "image" + index + ".jpg", 1,
                                 index))
                         .toList();
        filterController.setItems(items, 0);
    }

    @Test
    void onNoFilter_ShouldShowAllItemsAndNavigateThroughThem() {
        assertEquals(items, filterController.getShownItems());
        assertEquals(5, filterController.shownCountProperty().get());
        assertEquals(5, filterController.matchCountProperty().get());
        assertFalse(filterController.filterActiveProperty().get());
        assertEquals(0, filterController.currentPositionProperty().get());
        assertFalse(filterController.hasPreviousProperty().get());
        assertEquals(Optional.of(items.get(1)), filterController.nextItem());
        assertEquals(Optional.empty(), filterController.previousItem());
    }

    @Test
    void onFilter_ShouldSkipHiddenItemsWhenNavigating() {
        annotate(0, 2, 4);

        assertEquals(Optional.empty(), filterController.setFilter(ANNOTATED));

        assertEquals(List.of(items.get(0), items.get(2), items.get(4)), filterController.getShownItems());
        assertTrue(filterController.filterActiveProperty().get());
        assertEquals(3, filterController.matchCountProperty().get());
        assertEquals(Optional.of(items.get(2)), filterController.nextItem());

        filterController.onCurrentImageChanged(2);
        assertEquals(1, filterController.currentPositionProperty().get());
        assertEquals(Optional.of(items.get(0)), filterController.previousItem());
        assertEquals(Optional.of(items.get(4)), filterController.nextItem());

        filterController.onCurrentImageChanged(4);
        assertFalse(filterController.hasNextProperty().get());
        assertEquals(Optional.empty(), filterController.nextItem());
    }

    @Test
    void onFilterNotMatchingCurrentImage_ShouldReturnFirstMatch() {
        annotate(3, 4);

        assertEquals(Optional.of(items.get(3)), filterController.setFilter(ANNOTATED));
        // The current image stays shown until the caller navigates away from it.
        assertEquals(List.of(items.get(0), items.get(3), items.get(4)), filterController.getShownItems());
        assertEquals(2, filterController.matchCountProperty().get());

        filterController.onCurrentImageChanged(3);
        assertEquals(List.of(items.get(3), items.get(4)), filterController.getShownItems());
    }

    @Test
    void onFilterWithoutMatches_ShouldKeepOnlyCurrentImage() {
        assertEquals(Optional.empty(), filterController.setFilter(ANNOTATED));

        assertEquals(List.of(items.get(0)), filterController.getShownItems());
        assertEquals(0, filterController.matchCountProperty().get());
        assertFalse(filterController.hasNextProperty().get());
        assertFalse(filterController.hasPreviousProperty().get());
    }

    @Test
    void onCurrentImageStoppingToMatch_ShouldHideItOnlyAfterNavigatingAway() {
        filterController.setFilter(NOT_ANNOTATED);
        annotate(0);
        filterController.refresh();

        // Pinned: still shown although annotated now.
        assertEquals(items, filterController.getShownItems());
        assertEquals(4, filterController.matchCountProperty().get());

        filterController.onCurrentImageChanged(1);
        assertEquals(items.subList(1, 5), filterController.getShownItems());
    }

    @Test
    void onSetItems_ShouldResetFilter() {
        annotate(1);
        filterController.setFilter(ANNOTATED);

        filterController.setItems(items, 2);

        assertEquals(ImageFileFilter.NONE, filterController.getFilter());
        assertEquals(items, filterController.getShownItems());
        assertEquals(Optional.of(items.get(2)), filterController.getCurrentItem());
    }

    @Test
    void onClear_ShouldRemoveAllItems() {
        filterController.clear();

        assertTrue(filterController.getAllItems().isEmpty());
        assertTrue(filterController.getShownItems().isEmpty());
        assertEquals(Optional.empty(), filterController.getCurrentItem());
        assertEquals(-1, filterController.currentPositionProperty().get());
        assertEquals(Optional.empty(), filterController.getItem(0));
    }

    private void annotate(int... fileIndices) {
        for(int fileIndex : fileIndices) {
            final String fileName = items.get(fileIndex).getFileName();
            model.getImageFileNameToAnnotationMap().put(fileName, new ImageAnnotation(new ImageMetaData(fileName),
                    List.of(new BoundingBoxData(CAR, 0.1, 0.1, 0.5, 0.5, List.of()))));
        }
    }
}
