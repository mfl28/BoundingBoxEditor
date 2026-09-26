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

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.input.MouseEvent;

import java.util.Optional;

public interface BoundingShapeDrawer {
    void initializeShape(MouseEvent event, ObjectCategory objectCategory);
    void updateShape(MouseEvent event);
    void finalizeShape();
    boolean isDrawingInProgress();

    /**
     * Undoes the last step of the shape that is being drawn. If nothing is left of the shape, the drawing is
     * cancelled.
     *
     * @return the shape if the drawing was cancelled (the caller removes it), otherwise an empty optional
     */
    Optional<BoundingShapeViewable> undoLastStep();

    EditorImagePaneView.DrawingMode getDrawingMode();
}
