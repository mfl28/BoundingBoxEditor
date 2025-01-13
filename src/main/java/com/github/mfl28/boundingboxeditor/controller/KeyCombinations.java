/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;

/**
 * Class containing possible key-combinations.
 */
public class KeyCombinations {
    public static final KeyCombination navigateNext = new KeyCodeCombination(KeyCode.D,
            KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination navigatePrevious = new KeyCodeCombination(KeyCode.A,
            KeyCombination.SHORTCUT_DOWN);

    public static final List<KeyCode> navigationReleaseKeyCodes = List.of(
            KeyCode.A, KeyCode.D);
    public static final KeyCombination showAllBoundingShapes =
            new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
    public static final KeyCombination hideAllBoundingShapes =
            new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
    public static final KeyCombination showSelectedBoundingShape =
            new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination hideSelectedBoundingShape =
            new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN);

    public static final KeyCombination resetSizeAndCenterImage =
            new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination focusCategoryNameTextField =
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination focusCategorySearchField =
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination focusTagTextField =
            new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination focusFileSearchField =
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN, KeyCombination.ALT_DOWN);
    public static final KeyCombination deleteSelectedBoundingShape = new KeyCodeCombination(KeyCode.DELETE);
    public static final KeyCombination selectRectangleDrawingMode =
            new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination selectPolygonDrawingMode =
            new KeyCodeCombination(KeyCode.DIGIT2, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination selectFreehandDrawingMode =
            new KeyCodeCombination(KeyCode.DIGIT3, KeyCombination.SHORTCUT_DOWN);
    public static final KeyCombination removeEditingVerticesWhenBoundingPolygonSelected =
            new KeyCodeCombination(KeyCode.DELETE, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination addVerticesToPolygon =
            KeyCombination.keyCombination("Shift + Middle-Click inside Polygon");
    public static final KeyCombination changeSelectedBoundingShapeCategory =
            new KeyCodeCombination(KeyCode.C, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination hideNonSelectedBoundingShapes =
            new KeyCodeCombination(KeyCode.H, KeyCombination.SHIFT_DOWN);

    public static final KeyCombination simplifyPolygon =
            new KeyCodeCombination(KeyCode.S, KeyCombination.SHIFT_DOWN);
    public static final KeyCombination saveBoundingShapeAsImage =
            new KeyCodeCombination(KeyCode.I, KeyCombination.SHIFT_DOWN);

    public static final KeyCombination openSettings =
            new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN);

    private KeyCombinations() {
        throw new IllegalStateException("Key Combination Class");
    }
}
