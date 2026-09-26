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

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;

/**
 * Interface to access common data of bounding shapes.
 */
public interface BoundingShapeViewable {
    BoundingShapeViewData getViewData();

    void autoScaleWithBoundsAndInitialize(ReadOnlyObjectProperty<Bounds> autoScaleBounds, double imageWith,
                                          double imageHeight);

    Rectangle2D getRelativeOutlineRectangle();

    BoundingShapeTreeItem toTreeItem();

    /**
     * Moves the shape by the provided distance, but not beyond the image.
     *
     * @param dx the horizontal distance in the coordinates of the editor's image view
     * @param dy the vertical distance in the coordinates of the editor's image view
     */
    void moveBy(double dx, double dy);
}
