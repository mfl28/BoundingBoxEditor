/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.controller.utils.KeyCombinationEventHandler;
import com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.*;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class SceneKeyShortcutTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_3).getFile()));
    }

    @Test
    void onSceneKeyPressed_ShouldPerformCorrectAction(TestInfo testinfo, FxRobot robot) {
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.keyCombinationHandlers.stream().map(KeyCombinationEventHandler::getKeyCombination).toList(),
                Matchers.containsInAnyOrder(
                        KeyCombination.NO_MATCH,
                        Controller.KeyCombinations.navigateNext, Controller.KeyCombinations.navigatePrevious,
                        Controller.KeyCombinations.showAllBoundingShapes, Controller.KeyCombinations.hideAllBoundingShapes,
                        Controller.KeyCombinations.showSelectedBoundingShape, Controller.KeyCombinations.hideSelectedBoundingShape,
                        Controller.KeyCombinations.resetSizeAndCenterImage, Controller.KeyCombinations.focusCategoryNameTextField,
                        Controller.KeyCombinations.focusCategorySearchField, Controller.KeyCombinations.focusTagTextField,
                        Controller.KeyCombinations.focusFileSearchField, Controller.KeyCombinations.deleteSelectedBoundingShape,
                        Controller.KeyCombinations.selectRectangleDrawingMode, Controller.KeyCombinations.selectPolygonDrawingMode,
                        Controller.KeyCombinations.selectFreehandDrawingMode, Controller.KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected,
                        Controller.KeyCombinations.changeSelectedBoundingShapeCategory,
                        Controller.KeyCombinations.hideNonSelectedBoundingShapes, Controller.KeyCombinations.simplifyPolygon
                ));

        testNavigateNextKeyEvent(testinfo, true, true, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, true, true, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testNavigateNextKeyEvent(testinfo, false, true, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, false, true, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testNavigateNextKeyEvent(testinfo, true, false, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, true, false, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testSelectFreehandDrawingModeKeyEvent();
        testSelectRectangleModeKeyEvent();
        testFocusCategorySearchFieldKeyEvent();
        testFocusFileSearchKeyEvent();
        testFocusCategoryNameTextFieldKeyEvent();
        testFocusTagTextFieldKeyEvent();
        testSelectPolygonModeKeyEvent();

        // Draw a bounding polygon.
        enterNewCategory(robot, "dummy", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Double[] targetImageViewPointRatios = {0.25, 0.25, 0.1, 0.6, 0.4, 0.75, 0.75, 0.3};

        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));

        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getCurrentBoundingShapes()
                                .size() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of bounding polygons " +
                                "not found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        BoundingPolygonView polygon = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());

        testHideSelectedBoundingShapeKeyEvent(polygon);
        testShowSelectedBoundingShapeKeyEvent(polygon);
        testHideAllBoundingShapesKeyEvent(polygon);
        testShowAllBoundingShapesKeyEvent(polygon);
        testRemovePolygonVerticesKeyEvent(robot, targetImageViewPointRatios, polygon);
        testSimplifySelectedPolygonKeyEvent(polygon);
        testInitiateCategoryChangeKeyEvent(testinfo, robot);

        // Draw another polygon.
        Double[] targetImageViewPointRatios2 = {0.75, 0.75, 0.75, 0.85, 0.85, 0.85, 0.85, 0.75};

        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(targetImageViewPointRatios2[0], targetImageViewPointRatios2[1]),
                new Point2D(targetImageViewPointRatios2[2], targetImageViewPointRatios2[3]),
                new Point2D(targetImageViewPointRatios2[4], targetImageViewPointRatios2[5]),
                new Point2D(targetImageViewPointRatios2[6], targetImageViewPointRatios2[7]));

        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getCurrentBoundingShapes()
                                .size() == 2),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of bounding polygons " +
                                "not found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        BoundingPolygonView polygon2 = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(1);
        verifyThat(polygon2.isSelected(), Matchers.is(true));
        verifyThat(polygon2, NodeMatchers.isVisible());

        testHideNonSelectedShapesKeyEvent(polygon, polygon2);
        testRemoveCurrentlySelectedBoundingShapeKeyEvent();
        testResetImageViewSizeKeyEvent(robot);
    }

    private void testResetImageViewSizeKeyEvent(FxRobot robot) {
        double originalFitWidth = mainView.getEditorImageView().getFitWidth();
        double originalFitHeight = mainView.getEditorImageView().getFitHeight();

        robot.moveTo(mainView.getEditorImageView())
                .scroll(-30);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.equalTo(originalFitWidth));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.equalTo(originalFitHeight));

        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .press(MouseButton.PRIMARY)
                        .moveBy(10, 10);

        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getCursor(), Matchers.equalTo(Cursor.CLOSED_HAND));

        robot.release(MouseButton.PRIMARY).release(KeyCode.SHORTCUT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getCursor(), Matchers.equalTo(Cursor.OPEN_HAND));

        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .scroll(-30)
                .release(KeyCode.SHORTCUT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.not(Matchers.equalTo(originalFitWidth)));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.not(Matchers.equalTo(originalFitHeight)));

        KeyEvent resetImageViewSizeKeyEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.resetSizeAndCenterImage, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(resetImageViewSizeKeyEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.equalTo(originalFitWidth));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.equalTo(originalFitHeight));
    }

    private void testRemoveCurrentlySelectedBoundingShapeKeyEvent() {
        KeyEvent removeSelectedShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.deleteSelectedBoundingShape, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(removeSelectedShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(1));
    }

    private void testHideNonSelectedShapesKeyEvent(BoundingPolygonView polygon, BoundingPolygonView polygon2) {
        KeyEvent hideNonSelectedShapesEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.hideNonSelectedBoundingShapes, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideNonSelectedShapesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon, NodeMatchers.isInvisible());
        verifyThat(polygon2, NodeMatchers.isVisible());
    }

    private void testInitiateCategoryChangeKeyEvent(TestInfo testinfo, FxRobot robot) {
        KeyEvent categoryChangeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.changeSelectedBoundingShapeCategory, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(categoryChangeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage changeCategoryStage = timeOutGetTopModalStage(robot, "Change Category", testinfo);
        verifyThat(changeCategoryStage, Matchers.notNullValue(), saveScreenshot(testinfo));
        timeOutLookUpInStageAndClickOn(robot, changeCategoryStage, "Cancel", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Change Category", testinfo);
    }

    private void testSimplifySelectedPolygonKeyEvent(BoundingPolygonView polygon) {
        int numVertices = polygon.getPoints().size() / 2;
        KeyEvent simplifySelectedPolygonEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.simplifyPolygon, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(simplifySelectedPolygonEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numVertices));
    }

    private void testRemovePolygonVerticesKeyEvent(FxRobot robot, Double[] targetImageViewPointRatios, BoundingPolygonView polygon) {
        int numInitialVertices = targetImageViewPointRatios.length / 2;
        robot.clickOn("#vertex-handle", MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        KeyEvent removeEditingVerticesEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(removeEditingVerticesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numInitialVertices - 1));
    }

    private void testShowAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showAllBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.showAllBoundingShapes, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(showAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideAllBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.hideAllBoundingShapes, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testShowSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showSelectedBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.showSelectedBoundingShape, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(showSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideSelectedBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.hideSelectedBoundingShape, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testSelectPolygonModeKeyEvent() {
        KeyEvent selectPolygonModeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.selectPolygonDrawingMode, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectPolygonModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getPolygonModeButton().isSelected(), Matchers.is(true));
    }

    private void testFocusTagTextFieldKeyEvent() {
        KeyEvent focusTagTextFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.focusTagTextField, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusTagTextFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        // No bounding-shapes are selected, therefore tag text-field should be disabled.
        verifyThat(controller.getView().getTagInputField().isFocused(), Matchers.is(false));
    }

    private void testFocusCategoryNameTextFieldKeyEvent() {
        KeyEvent focusCategoryNameTextField = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.focusCategoryNameTextField, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusCategoryNameTextField));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getObjectCategoryInputField().isFocused(), Matchers.is(true));
    }

    private void testFocusFileSearchKeyEvent() {
        KeyEvent focusFileSearchFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.focusFileSearchField, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusFileSearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getImageFileSearchField().isFocused(), Matchers.is(true));
    }

    private void testFocusCategorySearchFieldKeyEvent() {
        KeyEvent focusCategorySearchFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.focusCategorySearchField, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusCategorySearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getCategorySearchField().isFocused(), Matchers.is(true));
    }

    private void testSelectRectangleModeKeyEvent() {
        KeyEvent selectRectangleModeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.selectRectangleDrawingMode, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectRectangleModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getRectangleModeButton().isSelected(), Matchers.is(true));
    }

    private void testSelectFreehandDrawingModeKeyEvent() {
        KeyEvent selectFreehandDrawingModeEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.selectFreehandDrawingMode, KeyEvent.KEY_PRESSED);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectFreehandDrawingModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getFreehandModeButton().isSelected(), Matchers.is(true));
    }

    private void testNavigatePreviousKeyEvent(TestInfo testinfo, boolean keyReleased, boolean ctrlReleased, String expectedTargetImageName) {
        KeyEvent navigatePreviousPressedEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.navigatePrevious, KeyEvent.KEY_PRESSED);
        KeyEvent navigatePreviousReleasedEventTemplate = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.navigatePrevious, KeyEvent.KEY_RELEASED);

        KeyEvent navigatePreviousReleasedEvent = new KeyEvent(
                navigatePreviousReleasedEventTemplate.getEventType(), navigatePreviousReleasedEventTemplate.getCharacter(),
                navigatePreviousReleasedEventTemplate.getText(),
                !keyReleased ? (navigatePreviousReleasedEventTemplate.isControlDown() ? KeyCode.CONTROL : KeyCode.COMMAND) : navigatePreviousReleasedEventTemplate.getCode(),
                navigatePreviousReleasedEventTemplate.isShiftDown(),
                navigatePreviousReleasedEventTemplate.isControlDown() && !ctrlReleased,
                navigatePreviousReleasedEventTemplate.isAltDown(), navigatePreviousReleasedEventTemplate.isMetaDown() && !ctrlReleased);

        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(navigatePreviousPressedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(navigatePreviousReleasedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile().getName(), Matchers.equalTo(
                expectedTargetImageName));
    }

    private void testNavigateNextKeyEvent(TestInfo testinfo, boolean keyReleased, boolean ctrlReleased, String expectedTargetImageName) {
        KeyEvent navigateNextPressedEvent = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.navigateNext, KeyEvent.KEY_PRESSED);
        KeyEvent navigateNextReleasedEventTemplate = buildKeyEventFromCombination((KeyCodeCombination) Controller.KeyCombinations.navigateNext, KeyEvent.KEY_RELEASED);

        KeyEvent navigateNextReleasedEvent = new KeyEvent(
                navigateNextReleasedEventTemplate.getEventType(), navigateNextReleasedEventTemplate.getCharacter(), navigateNextReleasedEventTemplate.getText(),
                !keyReleased ? (navigateNextReleasedEventTemplate.isControlDown() ? KeyCode.CONTROL : KeyCode.COMMAND) : navigateNextReleasedEventTemplate.getCode(),
                navigateNextReleasedEventTemplate.isShiftDown(),
                navigateNextReleasedEventTemplate.isControlDown() && !ctrlReleased,
                navigateNextReleasedEventTemplate.isAltDown(), navigateNextReleasedEventTemplate.isMetaDown() && !ctrlReleased);

        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(navigateNextPressedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(navigateNextReleasedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile().getName(), Matchers.equalTo(
                expectedTargetImageName));
    }

    private KeyEvent buildKeyEventFromCombination(KeyCodeCombination keyCombination, EventType<KeyEvent> eventType) {
        KeyEvent eventWindowLinux = new KeyEvent(eventType, "", "", keyCombination.getCode(), keyCombination.getShift() == KeyCombination.ModifierValue.DOWN,
                keyCombination.getShortcut() == KeyCombination.ModifierValue.DOWN, keyCombination.getAlt() == KeyCombination.ModifierValue.DOWN,
                false);
        KeyEvent eventMac = new KeyEvent(eventType, "", "", keyCombination.getCode(), keyCombination.getShift() == KeyCombination.ModifierValue.DOWN,
                false, keyCombination.getAlt() == KeyCombination.ModifierValue.DOWN,
                keyCombination.getShortcut() == KeyCombination.ModifierValue.DOWN);

        if (keyCombination.match(eventWindowLinux)) {
            return eventWindowLinux;
        }

        return eventMac;
    }
}
