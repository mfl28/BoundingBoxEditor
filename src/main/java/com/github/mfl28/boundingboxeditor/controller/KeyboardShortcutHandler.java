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
import com.github.mfl28.boundingboxeditor.ui.MainView;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Handles the keyboard shortcuts of the main window. Holding the image navigation shortcuts moves through the
 * images; while one is held, the navigate-next/previous-key-pressed properties are set, which allows skipping the
 * loading of the images that are only passed through.
 */
class KeyboardShortcutHandler {
    /**
     * Shortcuts that are handled here (on key release, undo also while drawing) and are shown in the menus as the
     * accelerators of their menu items. Their key presses are consumed so that the menu doesn't trigger them too.
     */
    static final List<KeyCombination> SHORTCUTS_SHOWN_IN_MENUS =
            List.of(KeyCombinations.undo, KeyCombinations.redo, KeyCombinations.openSettings);

    private final Model model;
    private final Editor editor;
    private final BooleanProperty navigatePreviousKeyPressed = new SimpleBooleanProperty(false);
    private final BooleanProperty navigateNextKeyPressed = new SimpleBooleanProperty(false);
    private final List<KeyCombinationEventHandler> keyCombinationHandlers;

    /**
     * The parts of the image editor the key handling interacts with.
     */
    interface Editor {
        /**
         * Returns whether a bounding shape is currently being drawn, during which key events are ignored.
         *
         * @return true if a bounding shape is being drawn
         */
        boolean isDrawingInProgress();

        /**
         * Switches the editor's zoom and pan mode on or off.
         *
         * @param zoomableAndPannable true to switch the mode on, false to switch it off
         */
        void setZoomableAndPannable(boolean zoomableAndPannable);

        /**
         * Shows the next image.
         */
        void showNextImage();

        /**
         * Shows the previous image.
         */
        void showPreviousImage();

        /**
         * Returns whether there is a next image to navigate to (taking the image file filter into account).
         *
         * @return true if there is a next image
         */
        boolean hasNextImage();

        /**
         * Returns whether there is a previous image to navigate to (taking the image file filter into account).
         *
         * @return true if there is a previous image
         */
        boolean hasPreviousImage();
    }

    /**
     * Creates a new keyboard shortcut handler.
     *
     * @param model           the model
     * @param editor          the image editor
     * @param actionShortcuts the handlers of all shortcuts besides image navigation
     */
    KeyboardShortcutHandler(Model model, Editor editor, List<KeyCombinationEventHandler> actionShortcuts) {
        this.model = model;
        this.editor = editor;
        this.keyCombinationHandlers = Stream.concat(createNavigationShortcuts().stream(), actionShortcuts.stream())
                .toList();
    }

    /**
     * Creates the handlers of the shortcuts that trigger actions in the view.
     *
     * @param view         the main view
     * @param openSettings opens the settings dialog
     * @param undo         undoes the last edit of the bounding shapes
     * @param redo         redoes the last undone edit of the bounding shapes
     * @param copy         copies the selected bounding shape
     * @param paste        pastes the copied bounding shape
     * @return the shortcut handlers
     */
    static List<KeyCombinationEventHandler> createViewActionShortcuts(MainView view, Runnable openSettings,
                                                                     Runnable undo, Runnable redo, Runnable copy,
                                                                     Runnable paste) {
        return Stream.concat(Stream.of(
                new KeyCombinationEventHandler(KeyCombinations.deleteSelectedBoundingShape,
                        null, event -> view.removeSelectedTreeItemAndChildren()),
                new KeyCombinationEventHandler(KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected,
                        null, event -> view.removeEditingVerticesWhenPolygonViewSelected()),
                new KeyCombinationEventHandler(KeyCombinations.focusCategorySearchField,
                        null, event -> view.getCategorySearchField().requestFocus()),
                new KeyCombinationEventHandler(KeyCombinations.focusFileSearchField,
                        null, event -> view.getImageFileSearchField().requestFocus()),
                new KeyCombinationEventHandler(KeyCombinations.focusCategoryNameTextField,
                        null, event -> view.getObjectCategoryInputField().requestFocus()),
                new KeyCombinationEventHandler(KeyCombinations.focusTagTextField,
                        null, event -> view.getTagInputField().requestFocus()),
                new KeyCombinationEventHandler(KeyCombinations.hideSelectedBoundingShape,
                        null, event -> view.getObjectTree().setToggleIconStateForSelectedObjectTreeItem(false)),
                new KeyCombinationEventHandler(KeyCombinations.hideAllBoundingShapes,
                        null, event -> view.getObjectTree().setToggleIconStateForAllTreeItems(false)),
                new KeyCombinationEventHandler(KeyCombinations.hideNonSelectedBoundingShapes,
                        null, event -> view.getObjectTree().setToggleIconStateForNonSelectedObjectTreeItems(false)),
                new KeyCombinationEventHandler(KeyCombinations.showSelectedBoundingShape,
                        null, event -> view.getObjectTree().setToggleIconStateForSelectedObjectTreeItem(true)),
                new KeyCombinationEventHandler(KeyCombinations.showAllBoundingShapes,
                        null, event -> view.getObjectTree().setToggleIconStateForAllTreeItems(true)),
                new KeyCombinationEventHandler(KeyCombinations.resetSizeAndCenterImage,
                        null, event -> view.getEditorImagePane().resetImageViewSize()),
                new KeyCombinationEventHandler(KeyCombinations.selectRectangleDrawingMode,
                        null, event -> view.getEditor().getEditorToolBar().getRectangleModeButton().setSelected(true)),
                new KeyCombinationEventHandler(KeyCombinations.selectPolygonDrawingMode,
                        null, event -> view.getEditor().getEditorToolBar().getPolygonModeButton().setSelected(true)),
                new KeyCombinationEventHandler(KeyCombinations.selectFreehandDrawingMode,
                        null, event -> view.getEditor().getEditorToolBar().getFreehandModeButton().setSelected(true)),
                new KeyCombinationEventHandler(KeyCombinations.changeSelectedBoundingShapeCategory,
                        null, event -> view.initiateCurrentSelectedBoundingBoxCategoryChange()),
                new KeyCombinationEventHandler(KeyCombinations.simplifyPolygon,
                        null, event -> view.simplifyCurrentSelectedBoundingPolygon()),
                new KeyCombinationEventHandler(KeyCombinations.saveBoundingShapeAsImage,
                        null, event -> view.saveCurrentSelectedBoundingShapeAsImage()),
                new KeyCombinationEventHandler(KeyCombinations.openSettings,
                        null, event -> openSettings.run()),
                new KeyCombinationEventHandler(KeyCombinations.undo,
                        null, event -> undo.run()),
                new KeyCombinationEventHandler(KeyCombinations.redo,
                        null, event -> redo.run()),
                new KeyCombinationEventHandler(KeyCombinations.copyBoundingShape,
                        null, event -> copy.run()),
                new KeyCombinationEventHandler(KeyCombinations.pasteBoundingShape,
                        null, event -> paste.run())
        ), createCategorySelectionShortcuts(view)).toList();
    }

    /**
     * Creates the shortcuts that select a category by its position in the category table: the number keys 1-9
     * (also on the numeric keypad), without modifiers.
     *
     * @param view the main view
     * @return the shortcut handlers
     */
    private static Stream<KeyCombinationEventHandler> createCategorySelectionShortcuts(MainView view) {
        return IntStream.rangeClosed(1, 9).boxed().flatMap(number -> Stream.of("DIGIT", "NUMPAD")
                .map(keyName -> new KeyCombinationEventHandler(new KeyCodeCombination(KeyCode.valueOf(keyName + number)),
                        null, event -> selectCategory(view.getObjectCategoryTable(), number - 1))));
    }

    private static void selectCategory(TableView<?> categoryTable, int index) {
        if(index < categoryTable.getItems().size()) {
            categoryTable.getSelectionModel().select(index);
            categoryTable.scrollTo(index);
        }
    }

    /**
     * Returns all shortcut handlers, in the order in which they are matched.
     *
     * @return the shortcut handlers
     */
    List<KeyCombinationEventHandler> getKeyCombinationHandlers() {
        return keyCombinationHandlers;
    }

    /**
     * Handles a key press in the main window.
     *
     * @param event the key event
     */
    void onKeyPressed(KeyEvent event) {
        if(!(event.getTarget() instanceof TextInputControl)
                && SHORTCUTS_SHOWN_IN_MENUS.stream().anyMatch(keyCombination -> keyCombination.match(event))) {
            event.consume();
        }

        if(editor.isDrawingInProgress()) {
            return;
        }

        if(event.isShortcutDown()) {
            editor.setZoomableAndPannable(true);
        }

        if(event.getTarget() instanceof TextInputControl) {
            return;
        }

        keyCombinationHandlers.stream()
                .filter(keyCombinationHandler -> keyCombinationHandler.handlesPressed(event))
                .findFirst()
                .ifPresent(keyCombinationEventHandler -> keyCombinationEventHandler.onPressed(event));
    }

    /**
     * Handles a key release in the main window.
     *
     * @param event the key event
     */
    void onKeyReleased(KeyEvent event) {
        final boolean drawingInProgress = editor.isDrawingInProgress();

        // While drawing, only undo is handled: it undoes the last drawing step (e.g. a polygon vertex).
        if(drawingInProgress && !KeyCombinations.undo.match(event)) {
            return;
        }

        if(!drawingInProgress && !event.isShortcutDown()) {
            editor.setZoomableAndPannable(false);
        }

        if(event.getTarget() instanceof TextInputControl) {
            return;
        }

        keyCombinationHandlers.stream()
                .filter(keyCombinationHandler -> keyCombinationHandler.handlesReleased(event))
                .findFirst()
                .ifPresent(keyCombinationEventHandler -> keyCombinationEventHandler.onReleased(event));
    }

    /**
     * Returns the property that is true while the navigate-next shortcut is held.
     *
     * @return the property
     */
    ReadOnlyBooleanProperty navigateNextKeyPressedProperty() {
        return navigateNextKeyPressed;
    }

    /**
     * Returns the property that is true while the navigate-previous shortcut is held.
     *
     * @return the property
     */
    ReadOnlyBooleanProperty navigatePreviousKeyPressedProperty() {
        return navigatePreviousKeyPressed;
    }

    private List<KeyCombinationEventHandler> createNavigationShortcuts() {
        return List.of(
                new KeyCombinationEventHandler(KeyCombination.NO_MATCH, null,
                        event -> {
                            navigatePreviousKeyPressed.set(false);
                            navigateNextKeyPressed.set(false);
                        },
                        event -> (navigateNextKeyPressed.get() || navigatePreviousKeyPressed.get()) &&
                                (KeyCombinations.navigationReleaseKeyCodes.contains(event.getCode())
                                        || !event.isShortcutDown())
                ),
                new KeyCombinationEventHandler(KeyCombinations.navigateNext,
                        event -> handleNavigateNextKeyPressed(), null),
                new KeyCombinationEventHandler(KeyCombinations.navigatePrevious,
                        event -> handleNavigatePreviousKeyPressed(), null)
        );
    }

    private void handleNavigateNextKeyPressed() {
        if(model.containsImageFiles() && editor.hasNextImage()
                && !navigatePreviousKeyPressed.get()) {
            navigateNextKeyPressed.set(true);
            editor.showNextImage();
        } else {
            navigateNextKeyPressed.set(false);
        }
    }

    private void handleNavigatePreviousKeyPressed() {
        if(model.containsImageFiles() && editor.hasPreviousImage()
                && !navigateNextKeyPressed.get()) {
            navigatePreviousKeyPressed.set(true);
            editor.showPreviousImage();
        } else {
            navigatePreviousKeyPressed.set(false);
        }
    }
}
