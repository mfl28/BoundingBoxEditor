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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * The undo and redo history of one image's bounding shapes. Each entry is a snapshot of all the image's shapes, so
 * every kind of edit (drawing, moving, deleting, re-nesting, changing categories or tags, ...) is undone the same
 * way, by restoring the previous snapshot.
 */
final class UndoHistory {
    private final int maxUndoSteps;
    private final Deque<List<BoundingShapeData>> undoStates = new ArrayDeque<>();
    private final Deque<List<BoundingShapeData>> redoStates = new ArrayDeque<>();
    private List<BoundingShapeData> currentState;

    /**
     * Creates a history.
     *
     * @param initialState the image's shapes when the history starts
     * @param maxUndoSteps the number of steps that can be undone; older steps are dropped
     */
    UndoHistory(List<BoundingShapeData> initialState, int maxUndoSteps) {
        this.currentState = List.copyOf(initialState);
        this.maxUndoSteps = maxUndoSteps;
    }

    /**
     * Records the image's shapes as the new state, if they differ from the current one. Recording a change makes
     * the previous state undoable and discards the states that could be redone.
     *
     * @param state the image's shapes
     * @return true if the state was recorded, false if nothing changed
     */
    boolean recordState(List<BoundingShapeData> state) {
        if(currentState.equals(state)) {
            return false;
        }

        undoStates.push(currentState);

        if(undoStates.size() > maxUndoSteps) {
            undoStates.removeLast();
        }

        redoStates.clear();
        currentState = List.copyOf(state);
        return true;
    }

    /**
     * Steps back to the previous state.
     *
     * @return the state to restore, or an empty optional if there is nothing to undo
     */
    Optional<List<BoundingShapeData>> undo() {
        if(undoStates.isEmpty()) {
            return Optional.empty();
        }

        redoStates.push(currentState);
        currentState = undoStates.pop();
        return Optional.of(currentState);
    }

    /**
     * Steps forward to the state that was last undone.
     *
     * @return the state to restore, or an empty optional if there is nothing to redo
     */
    Optional<List<BoundingShapeData>> redo() {
        if(redoStates.isEmpty()) {
            return Optional.empty();
        }

        undoStates.push(currentState);
        currentState = redoStates.pop();
        return Optional.of(currentState);
    }

    /**
     * Replaces the current state without changing the undo and redo steps. Used after restoring a state, because
     * the restored shapes can differ from the snapshot by rounding when they are read back.
     *
     * @param state the image's shapes
     */
    void replaceCurrentState(List<BoundingShapeData> state) {
        currentState = List.copyOf(state);
    }

    boolean canUndo() {
        return !undoStates.isEmpty();
    }

    boolean canRedo() {
        return !redoStates.isEmpty();
    }
}
