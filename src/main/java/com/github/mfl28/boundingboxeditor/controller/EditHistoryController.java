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

import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Undoes and redoes edits of the bounding shapes. Every image keeps its own {@link UndoHistory} while the image
 * folder is open. Edits are recorded at checkpoints, which are expected after every finished user interaction:
 * a checkpoint compares the shown shapes with the last recorded state and only records them if they changed.
 */
class EditHistoryController {
    static final int MAX_UNDO_STEPS = 50;

    private final Operations operations;
    private final Map<File, UndoHistory> imageFileToHistory = new HashMap<>();
    private final BooleanProperty undoAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty redoAvailable = new SimpleBooleanProperty(false);
    private UndoHistory currentHistory;

    /**
     * The access to the shown bounding shapes, which is implemented by the {@link Controller}.
     */
    interface Operations {
        /**
         * Returns whether the shown shapes can be recorded or replaced, i.e. an image is fully loaded and no shape is
         * currently being drawn.
         *
         * @return true if the shapes are in a stable state
         */
        boolean isEditingPossible();

        /**
         * Returns whether a shape is currently being drawn.
         *
         * @return true while drawing
         */
        boolean isDrawingInProgress();

        /**
         * Undoes the last step of the shape that is currently being drawn (e.g. its last polygon vertex).
         */
        void undoDrawingStep();

        /**
         * Returns the shown image's shapes.
         *
         * @return the shapes, with nested shapes as parts of their parents
         */
        List<BoundingShapeData> extractShapes();

        /**
         * Replaces the shown image's shapes.
         *
         * @param shapes the shapes to show
         */
        void restoreShapes(List<BoundingShapeData> shapes);
    }

    EditHistoryController(Operations operations) {
        this.operations = operations;
    }

    /**
     * Switches to the history of an image once it and its shapes are shown. A new history starts with the shown
     * shapes; an existing one records the shown shapes if they changed while the image was not shown (e.g. by a
     * prediction).
     *
     * @param imageFile the shown image's file
     */
    void onImageShown(File imageFile) {
        final List<BoundingShapeData> shapes = operations.extractShapes();
        currentHistory = imageFileToHistory.get(imageFile);

        if(currentHistory == null) {
            currentHistory = new UndoHistory(shapes, MAX_UNDO_STEPS);
            imageFileToHistory.put(imageFile, currentHistory);
        } else {
            currentHistory.record(shapes);
        }

        updateAvailability();
    }

    /**
     * Records the pending edits of the shown image before another image is shown.
     */
    void onImageHidden() {
        checkpoint();
        currentHistory = null;
        updateAvailability();
    }

    /**
     * Records the shown shapes if they changed since the last checkpoint.
     */
    void checkpoint() {
        if(currentHistory != null && operations.isEditingPossible()) {
            currentHistory.record(operations.extractShapes());
            updateAvailability();
        }
    }

    /**
     * Undoes the last edit. While a shape is being drawn, undoes the last drawing step instead.
     */
    void undo() {
        if(operations.isDrawingInProgress()) {
            operations.undoDrawingStep();
            return;
        }

        checkpoint();

        if(currentHistory != null && operations.isEditingPossible()) {
            currentHistory.undo().ifPresent(this::restore);
            updateAvailability();
        }
    }

    /**
     * Redoes the last undone edit.
     */
    void redo() {
        checkpoint();

        if(currentHistory != null && operations.isEditingPossible()) {
            currentHistory.redo().ifPresent(this::restore);
            updateAvailability();
        }
    }

    /**
     * Discards the histories of all images, e.g. when a new image folder is opened or annotations are imported.
     */
    void clear() {
        imageFileToHistory.clear();
        currentHistory = null;
        updateAvailability();
    }

    /**
     * Returns the property that is true while there is an edit of the shown image to undo.
     *
     * @return the property
     */
    ReadOnlyBooleanProperty undoAvailableProperty() {
        return undoAvailable;
    }

    /**
     * Returns the property that is true while there is an undone edit of the shown image to redo.
     *
     * @return the property
     */
    ReadOnlyBooleanProperty redoAvailableProperty() {
        return redoAvailable;
    }

    private void restore(List<BoundingShapeData> shapes) {
        operations.restoreShapes(shapes);
        currentHistory.replaceCurrentState(operations.extractShapes());
    }

    private void updateAvailability() {
        undoAvailable.set(currentHistory != null && currentHistory.canUndo());
        redoAvailable.set(currentHistory != null && currentHistory.canRedo());
    }
}
