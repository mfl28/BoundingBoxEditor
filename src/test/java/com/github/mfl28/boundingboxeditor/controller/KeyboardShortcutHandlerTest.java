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

import com.github.mfl28.boundingboxeditor.controller.utils.KeyCombinationEventHandler;
import com.github.mfl28.boundingboxeditor.model.Model;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the keyboard shortcut handling without the UI, using a mocked editor.
 */
@Tag("unit")
class KeyboardShortcutHandlerTest {
    private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");
    private static final KeyCombination ACTION_KEY_COMBINATION = new KeyCodeCombination(KeyCode.X);

    private final Model model = new Model();
    private final KeyboardShortcutHandler.Editor editor = mock(KeyboardShortcutHandler.Editor.class);
    private final List<KeyEvent> actionEvents = new ArrayList<>();
    private KeyboardShortcutHandler keyboardShortcutHandler;

    @BeforeEach
    void setUp() {
        keyboardShortcutHandler = new KeyboardShortcutHandler(model, editor,
                List.of(new KeyCombinationEventHandler(ACTION_KEY_COMBINATION, null, actionEvents::add)));
    }

    @Test
    void onCreation_ShouldMatchNavigationShortcutsBeforeActionShortcuts() {
        assertEquals(List.of(KeyCombination.NO_MATCH, KeyCombinations.navigateNext, KeyCombinations.navigatePrevious,
                        ACTION_KEY_COMBINATION),
                keyboardShortcutHandler.getKeyCombinationHandlers().stream()
                        .map(KeyCombinationEventHandler::getKeyCombination).toList());
    }

    @Test
    void onNavigateNextHeld_ShouldShowNextImageUntilReleased() {
        givenImages(3);

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.D));

        verify(editor).showNextImage();
        assertTrue(keyboardShortcutHandler.navigateNextKeyPressedProperty().get());

        keyboardShortcutHandler.onKeyReleased(shortcutEvent(KeyEvent.KEY_RELEASED, KeyCode.D));

        assertFalse(keyboardShortcutHandler.navigateNextKeyPressedProperty().get());
    }

    @Test
    void onNavigateNextReleasingShortcutModifier_ShouldStopNavigation() {
        givenImages(3);
        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.D));

        keyboardShortcutHandler.onKeyReleased(new KeyEvent(KeyEvent.KEY_RELEASED, "", "",
                IS_MAC ? KeyCode.META : KeyCode.CONTROL, false, false, false, false));

        assertFalse(keyboardShortcutHandler.navigateNextKeyPressedProperty().get());
    }

    @Test
    void onNavigateNext_WhenAtLastImage_ShouldNotNavigate() {
        givenImages(1);

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.D));

        verify(editor, never()).showNextImage();
        assertFalse(keyboardShortcutHandler.navigateNextKeyPressedProperty().get());
    }

    @Test
    void onNavigatePrevious_WhileNavigatingToNext_ShouldBeIgnored() {
        givenImages(3);
        model.incrementFileIndex();
        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.D));

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.A));

        verify(editor, never()).showPreviousImage();
        assertFalse(keyboardShortcutHandler.navigatePreviousKeyPressedProperty().get());
        assertTrue(keyboardShortcutHandler.navigateNextKeyPressedProperty().get());
    }

    @Test
    void onNavigatePrevious_WhenNotAtFirstImage_ShouldShowPreviousImage() {
        givenImages(3);
        model.incrementFileIndex();

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.A));

        verify(editor).showPreviousImage();
        assertTrue(keyboardShortcutHandler.navigatePreviousKeyPressedProperty().get());
    }

    @Test
    void onShortcutModifier_ShouldSwitchZoomAndPanModeOnWhilePressed() {
        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, IS_MAC ? KeyCode.META : KeyCode.CONTROL));
        verify(editor).setZoomableAndPannable(true);

        keyboardShortcutHandler.onKeyReleased(new KeyEvent(KeyEvent.KEY_RELEASED, "", "",
                IS_MAC ? KeyCode.META : KeyCode.CONTROL, false, false, false, false));
        verify(editor).setZoomableAndPannable(false);
    }

    @Test
    void onActionShortcut_ShouldRunActionOnRelease() {
        final KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.X, false, false, false, false);
        final KeyEvent released = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.X, false, false, false, false);

        keyboardShortcutHandler.onKeyPressed(pressed);
        assertTrue(actionEvents.isEmpty());

        keyboardShortcutHandler.onKeyReleased(released);
        assertEquals(List.of(released), actionEvents);
    }

    @Test
    void onKeyEvents_WhileDrawing_ShouldBeIgnored() {
        givenImages(3);
        when(editor.isDrawingInProgress()).thenReturn(true);

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.D));
        keyboardShortcutHandler.onKeyReleased(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.X, false, false,
                false, false));

        verify(editor, never()).showNextImage();
        verify(editor, never()).setZoomableAndPannable(anyBoolean());
        assertTrue(actionEvents.isEmpty());
    }

    @Test
    void onUndoShortcut_WhileDrawing_ShouldStillBeHandledWithoutZoomAndPanMode() {
        final List<KeyEvent> undoEvents = new ArrayList<>();
        keyboardShortcutHandler = new KeyboardShortcutHandler(model, editor,
                List.of(new KeyCombinationEventHandler(KeyCombinations.undo, null, undoEvents::add)));
        when(editor.isDrawingInProgress()).thenReturn(true);

        keyboardShortcutHandler.onKeyPressed(shortcutEvent(KeyEvent.KEY_PRESSED, KeyCode.Z));
        keyboardShortcutHandler.onKeyReleased(shortcutEvent(KeyEvent.KEY_RELEASED, KeyCode.Z));

        assertEquals(1, undoEvents.size());
        verify(editor, never()).setZoomableAndPannable(anyBoolean());
    }

    @Test
    void onUndoAndRedoShortcuts_ShouldBeDistinguishedByShift() {
        final List<KeyEvent> undoEvents = new ArrayList<>();
        final List<KeyEvent> redoEvents = new ArrayList<>();
        keyboardShortcutHandler = new KeyboardShortcutHandler(model, editor,
                List.of(new KeyCombinationEventHandler(KeyCombinations.undo, null, undoEvents::add),
                        new KeyCombinationEventHandler(KeyCombinations.redo, null, redoEvents::add)));

        keyboardShortcutHandler.onKeyReleased(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.Z, true, !IS_MAC,
                false, IS_MAC));

        assertTrue(undoEvents.isEmpty());
        assertEquals(1, redoEvents.size());
    }

    private void givenImages(int nrImages) {
        final List<File> imageFiles = new ArrayList<>();

        for(int i = 0; i < nrImages; ++i) {
            imageFiles.add(new File("image" + i + ".jpg"));
        }

        model.setImageFiles(imageFiles);
    }

    private static KeyEvent shortcutEvent(EventType<KeyEvent> type, KeyCode keyCode) {
        return new KeyEvent(type, "", "", keyCode, false, !IS_MAC, false, IS_MAC);
    }
}
