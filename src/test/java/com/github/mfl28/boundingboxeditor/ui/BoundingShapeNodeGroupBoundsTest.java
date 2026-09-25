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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Picking skips a parent whose bounds don't contain the mouse position, so the node-group of a bounding shape must
 * always enclose the shape. Otherwise the shape can't be clicked anymore.
 */
@Tag("unit")
class BoundingShapeNodeGroupBoundsTest {
    private static final double IMAGE_WIDTH = 1000;
    private static final double IMAGE_HEIGHT = 500;

    @Test
    void onAutoScaleBoundsChanged_NodeGroupBoundsShouldEncloseBoundingBox() {
        final BoundingBoxData data = new BoundingBoxData(new ObjectCategory("category", Color.RED),
                0.5, 0.5, 0.75, 0.75, List.of());
        final SimpleObjectProperty<Bounds> imageViewBounds =
                new SimpleObjectProperty<>(new BoundingBox(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT));

        final BoundingBoxView boundingBoxView = BoundingBoxView.fromData(data, IMAGE_WIDTH, IMAGE_HEIGHT);
        boundingBoxView.autoScaleWithBoundsAndInitialize(imageViewBounds, IMAGE_WIDTH, IMAGE_HEIGHT);
        assertNodeGroupEnclosesShape(boundingBoxView, "after initialization");

        // Zooming/resizing changes the image-view bounds, which moves and scales the bounding box.
        imageViewBounds.set(new BoundingBox(0, 145, IMAGE_WIDTH * 0.8, IMAGE_HEIGHT * 0.8));
        assertNodeGroupEnclosesShape(boundingBoxView, "after first rescale");

        imageViewBounds.set(new BoundingBox(20, 0, IMAGE_WIDTH * 1.2, IMAGE_HEIGHT * 1.2));
        assertNodeGroupEnclosesShape(boundingBoxView, "after second rescale");
    }

    private static void assertNodeGroupEnclosesShape(BoundingBoxView boundingBoxView, String step) {
        final Bounds shapeBounds = boundingBoxView.getBoundsInParent();
        final Bounds nodeGroupBounds = boundingBoxView.getViewData().getNodeGroup().getBoundsInLocal();

        assertTrue(nodeGroupBounds.contains(shapeBounds),
                () -> "Node-group bounds " + nodeGroupBounds + " don't enclose shape bounds " + shapeBounds +
                        " " + step + ".");
    }
}
