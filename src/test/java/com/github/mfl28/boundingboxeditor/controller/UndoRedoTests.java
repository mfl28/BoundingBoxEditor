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

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Toggle;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Tests undoing and redoing edits of the bounding shapes through the running application.
 */
@Tag("ui")
class UndoRedoTests extends BoundingBoxEditorTestBase {
    private static final String CATEGORY_NAME = "Test";

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onUndoAndRedo_ShouldRestoreDrawnShapes(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);

        verifyThat(mainView.getUndoMenuItem().isDisable(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getRedoMenuItem().isDisable(), Matchers.is(true), saveScreenshot(testinfo));

        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1 && isUndoAvailable(), "first box", testinfo);
        moveRelativeToImageView(robot, new Point2D(0.6, 0.6), new Point2D(0.8, 0.8));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2, "second box", testinfo);

        verifyThat(mainView.getUndoMenuItem().isDisable(), Matchers.is(false), saveScreenshot(testinfo));

        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "one box after undo", testinfo);
        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "no box after second undo", testinfo);

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(CATEGORY_NAME), Matchers.equalTo(0),
                saveScreenshot(testinfo));
        verifyThat(mainView.getUndoMenuItem().isDisable(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getRedoMenuItem().isDisable(), Matchers.is(false), saveScreenshot(testinfo));

        // Undo can bring back shapes of a category that was deleted meanwhile.
        final ObjectCategory category = model.getCategoryNameToCategoryMap().get(CATEGORY_NAME);
        robot.interact(() -> model.getObjectCategories().remove(category));

        pressShortcut(KeyCombinations.redo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "one box after redo", testinfo);
        verifyThat(model.getObjectCategories(), Matchers.contains(category), saveScreenshot(testinfo));

        robot.interact(() -> controller.onRegisterRedoAction());
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2, "two boxes after second redo", testinfo);
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(CATEGORY_NAME), Matchers.equalTo(2),
                saveScreenshot(testinfo));
        verifyThat(mainView.getRedoMenuItem().isDisable(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                saveScreenshot(testinfo));

        // A new edit discards the undone steps. The category was deleted and restored above, so select it again.
        robot.interact(() -> mainView.getObjectCategoryTable().getSelectionModel().select(category));
        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1 && isRedoAvailable(), "undo before new edit",
                testinfo);
        moveRelativeToImageView(robot, new Point2D(0.5, 0.1), new Point2D(0.7, 0.3));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2 && !isRedoAvailable(),
                "redo discarded after new edit", testinfo);
    }

    @Test
    void onUndoWhileDrawingPolygon_ShouldRemoveLastVertexAndFinallyTheShape(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);

        timeOutClickOn(robot, "#polygon-mode-button-icon", testinfo);

        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(0.25, 0.25), new Point2D(0.1, 0.6), new Point2D(0.4, 0.75));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "polygon being drawn", testinfo);

        final BoundingPolygonView polygon = (BoundingPolygonView) mainView.getCurrentBoundingShapes().getFirst();
        verifyThat(polygon.getPoints().size(), Matchers.equalTo(6), saveScreenshot(testinfo));

        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> polygon.getPoints().size() == 4, "last vertex removed", testinfo);
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(true), saveScreenshot(testinfo));

        pressShortcut(KeyCombinations.undo);
        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "polygon removed", testinfo);
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectTree().getRoot().getChildren().isEmpty(), Matchers.is(true),
                saveScreenshot(testinfo));

        // The cancelled polygon was never finished, so there is nothing else to undo.
        verifyThat(isUndoAvailable(), Matchers.is(false), saveScreenshot(testinfo));

        // Drawing still works afterwards.
        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(0.25, 0.25), new Point2D(0.1, 0.6), new Point2D(0.4, 0.75));
        moveAndClickRelativeToImageView(robot, MouseButton.SECONDARY, new Point2D(0.4, 0.75));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1
                && !mainView.getEditorImagePane().isDrawingInProgress() && isUndoAvailable(), "polygon finished",
                testinfo);

        // Undoing the deletion of a polygon selects it, which shows its vertex handles.
        robot.interact(() -> mainView.getObjectTree().getSelectionModel().select(
                mainView.getObjectTree().getRoot().getChildren().getFirst().getChildren().getFirst()));
        pressShortcut(KeyCombinations.deleteSelectedBoundingShape);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "polygon deleted", testinfo);

        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1
                && isSelected(mainView.getCurrentBoundingShapes().getFirst()), "restored polygon selected", testinfo);
        verifyThat(mainView.getObjectTree().getSelectionModel().getSelectedItem().getValue(),
                Matchers.sameInstance(mainView.getCurrentBoundingShapes().getFirst()), saveScreenshot(testinfo));
    }

    @Test
    void onUndoAfterSwitchingImages_ShouldUndoEditsOfShownImage(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);

        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1 && isUndoAvailable(), "box drawn", testinfo);

        timeOutClickOn(robot, "#next-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty() && !isUndoAvailable(),
                "new image without history", testinfo);

        timeOutClickOn(robot, "#previous-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1 && isUndoAvailable(),
                "first image with its history", testinfo);

        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "box undone", testinfo);
    }

    @Test
    void onUndoAfterMovingAndDeletingViaContextMenu_ShouldRestoreShape(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);

        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1 && isUndoAvailable(), "box drawn", testinfo);
        final List<BoundingShapeData> drawnShapes = shownShapes();

        // Dragging the selected box moves it.
        moveRelativeToImageView(robot, new Point2D(0.3, 0.3), new Point2D(0.5, 0.45));
        waitUntil(() -> !shownShapes().equals(drawnShapes), "box moved", testinfo);
        final List<BoundingShapeData> movedShapes = shownShapes();

        robot.interact(() -> mainView.getObjectTree().getSelectionModel().clearSelection());
        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> shownShapes().equals(drawnShapes), "move undone", testinfo);
        // The moved-back box is selected.
        waitUntil(() -> isSelected(mainView.getCurrentBoundingShapes().getFirst()), "restored box selected",
                testinfo);
        pressShortcut(KeyCombinations.redo);
        waitUntil(() -> shownShapes().equals(movedShapes), "move redone", testinfo);

        // The context menu's actions run in their own window.
        robot.rightClickOn(CATEGORY_NAME + " 1");
        timeOutClickOn(robot, "Delete", testinfo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "box deleted", testinfo);

        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> shownShapes().equals(movedShapes), "deletion undone", testinfo);
    }

    private List<BoundingShapeData> shownShapes() {
        try {
            return WaitForAsyncUtils.asyncFx(mainView::extractCurrentBoundingShapeData).get();
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch(ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isSelected(Object shape) {
        return shape instanceof Toggle toggle && toggle.isSelected();
    }

    private boolean isUndoAvailable() {
        return controller.getEditHistoryController().undoAvailableProperty().get();
    }

    private boolean isRedoAvailable() {
        return controller.getEditHistoryController().redoAvailableProperty().get();
    }

    private void pressShortcut(KeyCombination keyCombination) {
        final KeyEvent event = buildKeyEvent((KeyCodeCombination) keyCombination);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(event));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void waitUntil(Callable<Boolean> condition, String description, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        condition),
                () -> saveScreenshotAndReturnMessage(testinfo, "Expected state not reached: " + description));
    }

    private static KeyEvent buildKeyEvent(KeyCodeCombination keyCombination) {
        final boolean shift = keyCombination.getShift() == KeyCombination.ModifierValue.DOWN;
        final boolean shortcut = keyCombination.getShortcut() == KeyCombination.ModifierValue.DOWN;
        final KeyEvent controlEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCombination.getCode(), shift,
                shortcut, false, false);

        return keyCombination.match(controlEvent) ? controlEvent
                : new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCombination.getCode(), shift, false, false, shortcut);
    }
}
