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
import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class EditingShortcutTests extends BoundingBoxEditorTestBase {
    private static final String CATEGORY_NAME = "Test";
    private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onCopyAndPaste_ShouldCarryTheSelectedShapeOverToAnotherImage(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "drawn box", testinfo);

        timeOutClickOn(robot, CATEGORY_NAME + " 1", testinfo);
        final BoundingShapeData copiedBox = mainView.extractCurrentBoundingShapeData().getFirst();
        releaseShortcut(KeyCombinations.copyBoundingShape);
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.equalTo("Copied the Test shape."),
                   saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "next image without shapes", testinfo);

        releaseShortcut(KeyCombinations.pasteBoundingShape);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "pasted box", testinfo);
        // Same category and relative position (the images have different sizes).
        final BoundingBoxData pastedBox = (BoundingBoxData) mainView.extractCurrentBoundingShapeData().getFirst();
        verifyThat(pastedBox.getCategory(), Matchers.equalTo(copiedBox.getCategory()), saveScreenshot(testinfo));
        verifyThat(pastedBox.getXMinRelative(),
                   Matchers.closeTo(((BoundingBoxData) copiedBox).getXMinRelative(), 1e-6), saveScreenshot(testinfo));
        verifyThat(pastedBox.getYMaxRelative(),
                   Matchers.closeTo(((BoundingBoxData) copiedBox).getYMaxRelative(), 1e-6), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().getFirst().getViewData().isSelected(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(CATEGORY_NAME), Matchers.equalTo(2),
                   saveScreenshot(testinfo));

        // The paste can be undone.
        waitUntil(() -> !mainView.getUndoMenuItem().isDisable(), "undo available", testinfo);
        robot.interact(controller::onRegisterUndoAction);
        waitUntil(() -> mainView.getCurrentBoundingShapes().isEmpty(), "paste undone", testinfo);
    }

    @Test
    void onArrowKeys_ShouldMoveTheSelectedShapeByImagePixels(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "drawn box", testinfo);
        // Selects the box and gives the editor the focus.
        moveAndClickRelativeToImageView(robot, javafx.scene.input.MouseButton.PRIMARY, new Point2D(0.3, 0.3));
        waitUntil(() -> mainView.getCurrentBoundingShapes().getFirst().getViewData().isSelected(), "box selected",
                  testinfo);

        final double imageWidth = model.getCurrentImageMetaData().getImageWidth();
        final double imageHeight = model.getCurrentImageMetaData().getImageHeight();
        final BoundingBoxData before = (BoundingBoxData) mainView.extractCurrentBoundingShapeData().getFirst();

        robot.type(KeyCode.RIGHT);
        robot.press(KeyCode.SHIFT).type(KeyCode.DOWN).release(KeyCode.SHIFT);

        final BoundingBoxData after = (BoundingBoxData) mainView.extractCurrentBoundingShapeData().getFirst();
        verifyThat((after.getXMinRelative() - before.getXMinRelative()) * imageWidth, Matchers.closeTo(1, 1e-3),
                   saveScreenshot(testinfo));
        verifyThat((after.getYMinRelative() - before.getYMinRelative()) * imageHeight, Matchers.closeTo(10, 1e-3),
                   saveScreenshot(testinfo));
        verifyThat(after.getRelativeBoundsInImage().getWidth(),
                   Matchers.closeTo(before.getRelativeBoundsInImage().getWidth(), 1e-9), saveScreenshot(testinfo));

        // And back.
        robot.type(KeyCode.LEFT);
        robot.press(KeyCode.SHIFT).type(KeyCode.UP).release(KeyCode.SHIFT);
        final BoundingBoxData movedBack = (BoundingBoxData) mainView.extractCurrentBoundingShapeData().getFirst();
        verifyThat(movedBack.getXMinRelative(), Matchers.closeTo(before.getXMinRelative(), 1e-9),
                   saveScreenshot(testinfo));
        verifyThat(movedBack.getYMinRelative(), Matchers.closeTo(before.getYMinRelative(), 1e-9),
                   saveScreenshot(testinfo));

        // Not beyond the image's edge.
        robot.interact(() -> mainView.getCurrentBoundingShapes().getFirst().moveBy(-1e6, 0));

        verifyThat(((BoundingBoxData) mainView.extractCurrentBoundingShapeData().getFirst()).getXMinRelative(),
                   Matchers.closeTo(0, 1e-9), saveScreenshot(testinfo));
    }

    @Test
    void onNumberKeys_ShouldSelectTheCategoryAtThatPosition(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "First", testinfo);
        enterNewCategory(robot, "Second", testinfo);

        releaseKey(KeyCode.DIGIT2);
        verifyThat(mainView.getObjectCategoryTable().getSelectionModel().getSelectedIndex(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        releaseKey(KeyCode.NUMPAD1);
        verifyThat(mainView.getObjectCategoryTable().getSelectionModel().getSelectedIndex(), Matchers.equalTo(0),
                   saveScreenshot(testinfo));

        // Numbers beyond the categories change nothing.
        releaseKey(KeyCode.DIGIT9);
        verifyThat(mainView.getObjectCategoryTable().getSelectionModel().getSelectedIndex(), Matchers.equalTo(0),
                   saveScreenshot(testinfo));
    }

    private void releaseShortcut(KeyCombination shortcut) {
        final KeyCodeCombination keyCodeShortcut = (KeyCodeCombination) shortcut;
        final boolean shortcutDown = keyCodeShortcut.getShortcut() == KeyCombination.ModifierValue.DOWN;
        final KeyEvent event = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCodeShortcut.getCode(),
                keyCodeShortcut.getShift() == KeyCombination.ModifierValue.DOWN, shortcutDown && !IS_MAC, false,
                shortcutDown && IS_MAC);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(event));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void releaseKey(KeyCode keyCode) {
        final KeyEvent event = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode, false, false, false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(event));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void waitUntil(Callable<Boolean> condition, String description, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      condition),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected state not reached: " +
                                                                                   description));
    }
}
