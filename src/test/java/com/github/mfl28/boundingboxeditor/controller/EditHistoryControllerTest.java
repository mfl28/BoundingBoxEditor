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
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the per-image undo and redo handling against a fake editor whose shown shapes are a plain list.
 */
@Tag("unit")
class EditHistoryControllerTest {
    private static final ObjectCategory CATEGORY = new ObjectCategory("foo", Color.RED);
    private static final File IMAGE_1 = new File("image1.jpg");
    private static final File IMAGE_2 = new File("image2.jpg");

    private final FakeEditor editor = new FakeEditor();
    private final EditHistoryController editHistoryController = new EditHistoryController(editor);

    @Test
    void onUndoAndRedo_ShouldRestoreRecordedShapes() {
        editHistoryController.onImageShown(IMAGE_1);
        assertFalse(editHistoryController.undoAvailableProperty().get());

        editor.shownShapes = List.of(box(0.1));
        editHistoryController.checkpoint();
        editor.shownShapes = List.of(box(0.1), box(0.5));
        editHistoryController.checkpoint();

        assertTrue(editHistoryController.undoAvailableProperty().get());

        editHistoryController.undo();
        assertEquals(List.of(box(0.1)), editor.shownShapes);
        assertTrue(editHistoryController.redoAvailableProperty().get());

        editHistoryController.undo();
        assertEquals(List.of(), editor.shownShapes);
        assertFalse(editHistoryController.undoAvailableProperty().get());

        editHistoryController.redo();
        editHistoryController.redo();
        assertEquals(List.of(box(0.1), box(0.5)), editor.shownShapes);
        assertFalse(editHistoryController.redoAvailableProperty().get());
    }

    @Test
    void onUndo_WhenEditNotYetRecorded_ShouldRecordItFirst() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));

        editHistoryController.undo();

        assertEquals(List.of(), editor.shownShapes);
        assertTrue(editHistoryController.redoAvailableProperty().get());
    }

    @Test
    void onImageSwitch_ShouldKeepEachImagesHistory() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));
        editHistoryController.onImageHidden();

        editor.shownShapes = List.of();
        editHistoryController.onImageShown(IMAGE_2);
        assertFalse(editHistoryController.undoAvailableProperty().get());
        editHistoryController.onImageHidden();

        editor.shownShapes = List.of(box(0.1));
        editHistoryController.onImageShown(IMAGE_1);
        assertTrue(editHistoryController.undoAvailableProperty().get());

        editHistoryController.undo();
        assertEquals(List.of(), editor.shownShapes);
    }

    @Test
    void onImageShown_WhenShapesChangedWhileHidden_ShouldRecordChange() {
        editHistoryController.onImageShown(IMAGE_1);
        editHistoryController.onImageHidden();

        editor.shownShapes = List.of(box(0.3));
        editHistoryController.onImageShown(IMAGE_1);

        editHistoryController.undo();
        assertEquals(List.of(), editor.shownShapes);
    }

    @Test
    void onClear_ShouldDiscardAllHistories() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));
        editHistoryController.checkpoint();

        editHistoryController.clear();
        assertFalse(editHistoryController.undoAvailableProperty().get());

        editHistoryController.onImageShown(IMAGE_1);
        editHistoryController.undo();
        assertEquals(List.of(box(0.1)), editor.shownShapes);
    }

    @Test
    void onUndo_WhileDrawing_ShouldUndoDrawingStepOnly() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));
        editHistoryController.checkpoint();

        editor.drawingInProgress = true;
        editHistoryController.undo();

        assertEquals(1, editor.drawingStepsUndone);
        assertEquals(List.of(box(0.1)), editor.shownShapes);
    }

    @Test
    void onCheckpoint_WhenEditingNotPossible_ShouldNotRecord() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.editingPossible = false;
        editor.shownShapes = List.of(box(0.1));

        editHistoryController.checkpoint();

        assertFalse(editHistoryController.undoAvailableProperty().get());
    }

    @Test
    void onUndo_WhenRestoredShapesDifferByRounding_ShouldKeepRedoAvailable() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));
        editHistoryController.checkpoint();

        // Reading restored shapes back can differ slightly from the snapshot.
        editor.restoreOffset = 1e-9;
        editHistoryController.undo();
        editHistoryController.checkpoint();

        assertTrue(editHistoryController.redoAvailableProperty().get());
    }

    @Test
    void onUndoAndRedo_ShouldSelectRestoredShape() {
        editHistoryController.onImageShown(IMAGE_1);
        editor.shownShapes = List.of(box(0.1));
        editHistoryController.checkpoint();
        editor.shownShapes = List.of(box(0.1), box(0.5));
        editHistoryController.checkpoint();
        editor.shownShapes = List.of(box(0.1), box(0.6));
        editHistoryController.checkpoint();

        // Undoing the move of the second box selects it.
        editHistoryController.undo();
        assertEquals(List.of(1), editor.selectedShapePath);

        // Undoing the drawing of the second box only removes a shape: nothing to select.
        editor.selectedShapePath = null;
        editHistoryController.undo();
        assertNull(editor.selectedShapePath);

        // Redoing it brings the box back and selects it.
        editHistoryController.redo();
        assertEquals(List.of(1), editor.selectedShapePath);
    }

    @Test
    void onFindChangedShapePath_ShouldFindDeepestChangedShape() {
        final BoundingShapeData unchangedTop = box(0.0);
        final BoundingShapeData shownParent = box(0.1);
        shownParent.setParts(List.of(box(0.2), box(0.3)));
        final BoundingShapeData restoredParent = box(0.1);
        restoredParent.setParts(List.of(box(0.2), box(0.35)));

        assertEquals(Optional.of(List.of(1, 1)), EditHistoryController.findChangedShapePath(
                List.of(unchangedTop, shownParent), List.of(unchangedTop, restoredParent)));
    }

    @Test
    void onFindChangedShapePath_WhenPartRemoved_ShouldFindParent() {
        final BoundingShapeData shownParent = box(0.1);
        shownParent.setParts(List.of(box(0.2)));

        assertEquals(Optional.of(List.of(0)), EditHistoryController.findChangedShapePath(
                List.of(shownParent), List.of(box(0.1))));
    }

    @Test
    void onFindChangedShapePath_WhenShapesOnlyRemovedOrReordered_ShouldFindNothing() {
        assertEquals(Optional.empty(), EditHistoryController.findChangedShapePath(
                List.of(box(0.1), box(0.5)), List.of(box(0.5))));
        assertEquals(Optional.empty(), EditHistoryController.findChangedShapePath(
                List.of(box(0.1), box(0.5)), List.of(box(0.5), box(0.1))));
    }

    private static BoundingShapeData box(double minX) {
        return new BoundingBoxData(CATEGORY, minX, 0.1, minX + 0.2, 0.3, List.of());
    }

    private static class FakeEditor implements EditHistoryController.Operations {
        List<BoundingShapeData> shownShapes = List.of();
        boolean editingPossible = true;
        boolean drawingInProgress = false;
        int drawingStepsUndone = 0;
        double restoreOffset = 0;
        List<Integer> selectedShapePath = null;

        @Override
        public boolean isEditingPossible() {
            return editingPossible && !drawingInProgress;
        }

        @Override
        public boolean isDrawingInProgress() {
            return drawingInProgress;
        }

        @Override
        public void undoDrawingStep() {
            drawingStepsUndone++;
        }

        @Override
        public List<BoundingShapeData> extractShapes() {
            return shownShapes;
        }

        @Override
        public void restoreShapes(List<BoundingShapeData> shapes) {
            final List<BoundingShapeData> restored = new ArrayList<>();

            for(BoundingShapeData shape : shapes) {
                final BoundingBoxData boxData = (BoundingBoxData) shape;
                restored.add(new BoundingBoxData(CATEGORY, boxData.getRelativeBoundsInImage().getMinX() + restoreOffset,
                        0.1, boxData.getRelativeBoundsInImage().getMaxX(), 0.3, List.of()));
            }

            shownShapes = restored;
        }

        @Override
        public void selectShape(List<Integer> path) {
            selectedShapePath = path;
        }
    }
}
