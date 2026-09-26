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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class UndoHistoryTest {
    private static final ObjectCategory CATEGORY = new ObjectCategory("foo", Color.RED);
    private static final List<BoundingShapeData> EMPTY = List.of();
    private static final List<BoundingShapeData> ONE_BOX = List.of(box(0.1));
    private static final List<BoundingShapeData> TWO_BOXES = List.of(box(0.1), box(0.5));

    @Test
    void onRecord_WhenStateUnchanged_ShouldNotAddStep() {
        final UndoHistory history = new UndoHistory(ONE_BOX, 10);

        assertFalse(history.record(List.of(box(0.1))));
        assertFalse(history.canUndo());
    }

    @Test
    void onUndoAndRedo_ShouldStepThroughRecordedStates() {
        final UndoHistory history = new UndoHistory(EMPTY, 10);
        history.record(ONE_BOX);
        history.record(TWO_BOXES);

        assertEquals(Optional.of(ONE_BOX), history.undo());
        assertEquals(Optional.of(EMPTY), history.undo());
        assertEquals(Optional.empty(), history.undo());
        assertTrue(history.canRedo());

        assertEquals(Optional.of(ONE_BOX), history.redo());
        assertEquals(Optional.of(TWO_BOXES), history.redo());
        assertEquals(Optional.empty(), history.redo());
    }

    @Test
    void onRecord_AfterUndo_ShouldDiscardRedoSteps() {
        final UndoHistory history = new UndoHistory(EMPTY, 10);
        history.record(ONE_BOX);
        history.undo();

        assertTrue(history.record(TWO_BOXES));
        assertFalse(history.canRedo());
        assertEquals(Optional.of(EMPTY), history.undo());
    }

    @Test
    void onRecord_WhenLimitReached_ShouldDropOldestStep() {
        final UndoHistory history = new UndoHistory(EMPTY, 2);
        history.record(ONE_BOX);
        history.record(TWO_BOXES);
        history.record(List.of(box(0.7)));

        assertEquals(Optional.of(TWO_BOXES), history.undo());
        assertEquals(Optional.of(ONE_BOX), history.undo());
        assertEquals(Optional.empty(), history.undo());
    }

    @Test
    void onReplaceCurrentState_ShouldKeepSteps() {
        final UndoHistory history = new UndoHistory(EMPTY, 10);
        history.record(ONE_BOX);
        history.undo();

        history.replaceCurrentState(List.of(box(0.0000001)));

        assertTrue(history.canRedo());
        assertFalse(history.record(List.of(box(0.0000001))));
    }

    private static BoundingShapeData box(double minX) {
        return new BoundingBoxData(CATEGORY, minX, 0.1, minX + 0.2, 0.3, List.of());
    }
}
