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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests moving shapes (e.g. with the arrow keys), which keeps them inside the image.
 */
@Tag("unit")
class BoundingShapeMoveTest {
    private static final double IMAGE_WIDTH = 1000;
    private static final double IMAGE_HEIGHT = 500;
    // The image is shown at half its size, offset within the editor.
    private static final Bounds IMAGE_VIEW_BOUNDS = new BoundingBox(20, 10, IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2);
    private static final ObjectCategory CATEGORY = new ObjectCategory("category", Color.RED);

    @Test
    void onMoveBy_BoundingBox_ShouldMoveWithinTheImage() {
        final BoundingBoxView boxView = BoundingBoxView.fromData(
                new BoundingBoxData(CATEGORY, 0.2, 0.2, 0.4, 0.6, new ArrayList<>()), IMAGE_WIDTH, IMAGE_HEIGHT);
        boxView.autoScaleWithBoundsAndInitialize(new SimpleObjectProperty<>(IMAGE_VIEW_BOUNDS), IMAGE_WIDTH,
                                                 IMAGE_HEIGHT);

        // 5 view units are 10 image pixels, i.e. 0.01 of the width and 0.02 of the height.
        boxView.moveBy(5, -5);
        assertBounds(0.21, 0.18, 0.41, 0.58, (BoundingBoxData) boxView.toBoundingShapeData());

        boxView.moveBy(-1e6, 1e6);
        assertBounds(0, 0.6, 0.2, 1, (BoundingBoxData) boxView.toBoundingShapeData());
    }

    @Test
    void onMoveBy_BoundingPolygon_ShouldMoveAllPointsWithinTheImage() {
        final BoundingPolygonView polygonView = BoundingPolygonView.fromData(
                new BoundingPolygonData(CATEGORY, List.of(0.2, 0.2, 0.4, 0.2, 0.3, 0.6), new ArrayList<>()),
                IMAGE_WIDTH, IMAGE_HEIGHT);
        polygonView.autoScaleWithBoundsAndInitialize(new SimpleObjectProperty<>(IMAGE_VIEW_BOUNDS), IMAGE_WIDTH,
                                                     IMAGE_HEIGHT);

        polygonView.moveBy(5, -5);
        assertPoints(List.of(0.21, 0.18, 0.41, 0.18, 0.31, 0.58), polygonView);

        // The whole polygon stops at the image's edges, keeping its shape.
        polygonView.moveBy(1e6, 1e6);
        assertPoints(List.of(0.8, 0.6, 1.0, 0.6, 0.9, 1.0), polygonView);
        polygonView.moveBy(-1e6, -1e6);
        assertPoints(List.of(0.0, 0.0, 0.2, 0.0, 0.1, 0.4), polygonView);
    }

    private static void assertBounds(double xMin, double yMin, double xMax, double yMax, BoundingBoxData data) {
        assertEquals(xMin, data.getXMinRelative(), 1e-9);
        assertEquals(yMin, data.getYMinRelative(), 1e-9);
        assertEquals(xMax, data.getXMaxRelative(), 1e-9);
        assertEquals(yMax, data.getYMaxRelative(), 1e-9);
    }

    private static void assertPoints(List<Double> expected, BoundingPolygonView polygonView) {
        final List<Double> actual = ((BoundingPolygonData) polygonView.toBoundingShapeData()).getRelativePointsInImage();
        assertEquals(expected.size(), actual.size());

        for(int i = 0; i < expected.size(); ++i) {
            assertEquals(expected.get(i), actual.get(i), 1e-9, "point coordinate " + i);
        }
    }
}
