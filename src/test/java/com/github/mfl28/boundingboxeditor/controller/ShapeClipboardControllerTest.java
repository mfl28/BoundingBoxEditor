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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ShapeClipboardControllerTest {
    private static final ObjectCategory BOAT = new ObjectCategory("Boat", Color.RED);
    private static final ObjectCategory SAIL = new ObjectCategory("Sail", Color.WHITE);

    private final FakeOperations operations = new FakeOperations();
    private final ShapeClipboardController clipboardController = new ShapeClipboardController(operations);

    @Test
    void onPaste_ShouldAddAnIndependentCopyOfTheCopiedShapeAndSelectIt() {
        final BoundingShapeData boat = boatWithSail();
        operations.selectedShape = boat;

        clipboardController.copySelectedShape();
        assertTrue(clipboardController.hasCopiedShape());
        assertEquals(List.of(boat), operations.copiedShapes);

        final BoundingShapeData other = new BoundingBoxData(SAIL, 0.5, 0.5, 0.6, 0.6, new ArrayList<>());
        operations.shapes = List.of(other);
        clipboardController.paste();

        assertEquals(List.of(other, boat), operations.shapes);
        assertEquals(List.of(List.of(1)), operations.selectedPaths);
        assertEquals(1, operations.pastedShapes.size());

        // Changing the original doesn't change the pasted copy (tags and parts are not shared).
        boat.getTags().add("changed");
        boat.getParts().getFirst().getTags().add("changed");
        assertEquals(List.of("pose: left"), operations.shapes.get(1).getTags());
        assertEquals(List.of(), operations.shapes.get(1).getParts().getFirst().getTags());
    }

    @Test
    void onPaste_IntoTheSameImage_ShouldSelectTheNewShape() {
        final BoundingShapeData boat = boatWithSail();
        operations.selectedShape = boat;
        operations.shapes = List.of(boat);

        clipboardController.copySelectedShape();
        clipboardController.paste();
        clipboardController.paste();

        assertEquals(3, operations.shapes.size());
        assertEquals(List.of(List.of(1), List.of(2)), operations.selectedPaths);
    }

    @Test
    void onPaste_WithoutCopiedShapeOrWhileEditingIsNotPossible_ShouldDoNothing() {
        clipboardController.copySelectedShape();
        clipboardController.paste();

        assertFalse(clipboardController.hasCopiedShape());
        assertTrue(operations.copiedShapes.isEmpty());
        assertTrue(operations.pastedShapes.isEmpty());

        operations.selectedShape = boatWithSail();
        clipboardController.copySelectedShape();
        operations.editingPossible = false;
        clipboardController.paste();

        assertTrue(operations.shapes.isEmpty());
        assertTrue(operations.pastedShapes.isEmpty());
    }

    private static BoundingShapeData boatWithSail() {
        final BoundingShapeData boat = new BoundingPolygonData(BOAT, new ArrayList<>(List.of(0.1, 0.1, 0.4, 0.1,
                0.4, 0.4)), new ArrayList<>(List.of("pose: left")));
        boat.setParts(List.of(new BoundingBoxData(SAIL, 0.2, 0.2, 0.3, 0.3, new ArrayList<>())));
        return boat;
    }

    private static class FakeOperations implements ShapeClipboardController.Operations {
        private final List<BoundingShapeData> copiedShapes = new ArrayList<>();
        private final List<BoundingShapeData> pastedShapes = new ArrayList<>();
        private final List<List<Integer>> selectedPaths = new ArrayList<>();
        private boolean editingPossible = true;
        private BoundingShapeData selectedShape;
        private List<BoundingShapeData> shapes = List.of();

        @Override
        public boolean isEditingPossible() {
            return editingPossible;
        }

        @Override
        public Optional<BoundingShapeData> getSelectedShape() {
            return Optional.ofNullable(selectedShape);
        }

        @Override
        public List<BoundingShapeData> extractShapes() {
            return shapes;
        }

        @Override
        public void restoreShapes(List<BoundingShapeData> shapes) {
            this.shapes = List.copyOf(shapes);
        }

        @Override
        public void selectShape(List<Integer> path) {
            selectedPaths.add(path);
        }

        @Override
        public void onShapeCopied(BoundingShapeData shape) {
            copiedShapes.add(shape);
        }

        @Override
        public void onShapePasted(BoundingShapeData shape) {
            pastedShapes.add(shape);
        }
    }
}
