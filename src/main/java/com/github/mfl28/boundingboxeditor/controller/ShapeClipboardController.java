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

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Copies the selected bounding shape (with its parts and tags) and pastes it into the shown image, e.g. to carry
 * a shape over to the next image. Positions are relative to the image, so a shape keeps its relative position in
 * images of another size.
 */
class ShapeClipboardController {
    private final Operations operations;
    private BoundingShapeData copiedShape;

    /**
     * The parts of the application the clipboard works with.
     */
    interface Operations {
        /**
         * Returns whether the shown image's shapes can be edited (an image is shown and nothing is being drawn).
         *
         * @return true if editing is possible
         */
        boolean isEditingPossible();

        /**
         * Returns the selected shape.
         *
         * @return the selected shape with its parts, or an empty optional if none is selected
         */
        Optional<BoundingShapeData> getSelectedShape();

        /**
         * Returns the shapes of the shown image.
         *
         * @return the top-level shapes with their parts
         */
        List<BoundingShapeData> extractShapes();

        /**
         * Replaces the shapes of the shown image.
         *
         * @param shapes the top-level shapes with their parts
         */
        void restoreShapes(List<BoundingShapeData> shapes);

        /**
         * Selects a shape.
         *
         * @param path the shape's position (see {@link EditHistoryController.Operations#selectShape})
         */
        void selectShape(List<Integer> path);

        /**
         * Called after a shape was copied.
         *
         * @param shape the copied shape
         */
        void onShapeCopied(BoundingShapeData shape);

        /**
         * Called after a shape was pasted, e.g. to record the edit for undo.
         *
         * @param shape the pasted shape
         */
        void onShapePasted(BoundingShapeData shape);
    }

    /**
     * Creates a new clipboard controller.
     *
     * @param operations the operations
     */
    ShapeClipboardController(Operations operations) {
        this.operations = operations;
    }

    /**
     * Copies the selected shape.
     */
    void copySelectedShape() {
        operations.getSelectedShape().ifPresent(shape -> {
            copiedShape = copyOf(shape);
            operations.onShapeCopied(copiedShape);
        });
    }

    /**
     * Adds a copy of the copied shape to the shown image and selects it.
     */
    void paste() {
        if(copiedShape == null || !operations.isEditingPossible()) {
            return;
        }

        final BoundingShapeData pastedShape = copyOf(copiedShape);
        final List<BoundingShapeData> shapes = new ArrayList<>(operations.extractShapes());
        shapes.add(pastedShape);
        operations.restoreShapes(shapes);

        // The shapes are shown grouped by category; the pasted one comes after equal ones that were there before.
        final List<BoundingShapeData> shownShapes = operations.extractShapes();

        for(int i = shownShapes.size() - 1; i >= 0; --i) {
            if(shownShapes.get(i).equals(pastedShape)) {
                operations.selectShape(List.of(i));
                break;
            }
        }

        operations.onShapePasted(pastedShape);
    }

    /**
     * Returns whether a shape was copied.
     *
     * @return true if a shape can be pasted
     */
    boolean hasCopiedShape() {
        return copiedShape != null;
    }

    /**
     * Creates an independent copy of a shape: its tags and parts are not shared with the original.
     *
     * @param shape the shape
     * @return the copy
     */
    static BoundingShapeData copyOf(BoundingShapeData shape) {
        final BoundingShapeData copy = switch(shape) {
            case BoundingBoxData box -> new BoundingBoxData(box.getCategory(), box.getXMinRelative(),
                    box.getYMinRelative(), box.getXMaxRelative(), box.getYMaxRelative(), new ArrayList<>(box.getTags()));
            case BoundingPolygonData polygon -> new BoundingPolygonData(polygon.getCategory(),
                    new ArrayList<>(polygon.getRelativePointsInImage()), new ArrayList<>(polygon.getTags()));
        };

        copy.setParts(shape.getParts().stream().map(ShapeClipboardController::copyOf).toList());
        return copy;
    }
}
