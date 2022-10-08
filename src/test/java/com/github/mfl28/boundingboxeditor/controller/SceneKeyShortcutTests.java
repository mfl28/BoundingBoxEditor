/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
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

        testNavigateNextKeyEvent(testinfo);
        testNavigatePreviousKeyEvent(testinfo);
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
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.not(Matchers.equalTo(originalFitWidth)));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.not(Matchers.equalTo(originalFitHeight)));

        KeyEvent resetImageViewSizeKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.R, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(resetImageViewSizeKeyEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.equalTo(originalFitWidth));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.equalTo(originalFitHeight));
    }

    private void testRemoveCurrentlySelectedBoundingShapeKeyEvent() {
        KeyEvent removeSelectedShapeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DELETE, false, false,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(removeSelectedShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(1));
    }

    private void testHideNonSelectedShapesKeyEvent(BoundingPolygonView polygon, BoundingPolygonView polygon2) {
        KeyEvent hideNonSelectedShapesEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.H, true, false,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideNonSelectedShapesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon, NodeMatchers.isInvisible());
        verifyThat(polygon2, NodeMatchers.isVisible());
    }

    private void testInitiateCategoryChangeKeyEvent(TestInfo testinfo, FxRobot robot) {
        KeyEvent categoryChangeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.C, true, false,false, false);
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
        KeyEvent simplifySelectedPolygonEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, true, false,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(simplifySelectedPolygonEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numVertices));
    }

    private void testRemovePolygonVerticesKeyEvent(FxRobot robot, Double[] targetImageViewPointRatios, BoundingPolygonView polygon) {
        int numInitialVertices = targetImageViewPointRatios.length / 2;
        robot.clickOn("#vertex-handle", MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        KeyEvent removeEditingVerticesEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DELETE, true, false,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(removeEditingVerticesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numInitialVertices - 1));
    }

    private void testShowAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showAllBoundingShapeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.V, false, true,true, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(showAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideAllBoundingShapeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.H, false, true,true, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testShowSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showSelectedBoundingShapeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.V, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(showSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideSelectedBoundingShapeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.H, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(hideSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testSelectPolygonModeKeyEvent() {
        KeyEvent selectPolygonModeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.P, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectPolygonModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getPolygonModeButton().isSelected(), Matchers.is(true));
    }

    private void testFocusTagTextFieldKeyEvent() {
        KeyEvent focusTagTextFieldEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.T, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusTagTextFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        // No bounding-shapes are selected, therefore tag text-field should be disabled.
        verifyThat(controller.getView().getTagInputField().isFocused(), Matchers.is(false));
    }

    private void testFocusCategoryNameTextFieldKeyEvent() {
        KeyEvent focusCategoryNameTextField = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.N, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusCategoryNameTextField));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getObjectCategoryInputField().isFocused(), Matchers.is(true));
    }

    private void testFocusFileSearchKeyEvent() {
        KeyEvent focusFileSearchFieldEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, true,true, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusFileSearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getImageFileSearchField().isFocused(), Matchers.is(true));
    }

    private void testFocusCategorySearchFieldKeyEvent() {
        KeyEvent focusCategorySearchFieldEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(focusCategorySearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getCategorySearchField().isFocused(), Matchers.is(true));
    }

    private void testSelectRectangleModeKeyEvent() {
        KeyEvent selectRectangleModeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.K, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectRectangleModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getRectangleModeButton().isSelected(), Matchers.is(true));
    }

    private void testSelectFreehandDrawingModeKeyEvent() {
        KeyEvent selectFreehandDrawingModeEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, false, true,false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(selectFreehandDrawingModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getFreehandModeButton().isSelected(), Matchers.is(true));
    }

    private void testNavigatePreviousKeyEvent(TestInfo testinfo) {
        KeyEvent navigatePreviousPressedEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, true, false, false);

        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(navigatePreviousPressedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        KeyEvent navigatePreviousReleasedEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.A, false, true, false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(navigatePreviousReleasedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile().getName(), Matchers.equalTo("rachel-hisko-rEM3cK8F1pk-unsplash.jpg"));
    }

    private void testNavigateNextKeyEvent(TestInfo testinfo) {
        KeyEvent navigateNextPressedEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.D, false, true, false, false);

        Platform.runLater(() -> controller.onRegisterSceneKeyPressed(navigateNextPressedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        KeyEvent navigateNextReleasedEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.D, false, true, false, false);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(navigateNextReleasedEvent));
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile().getName(), Matchers.equalTo("wexor-tmg-L-2p8fapOA8-unsplash.jpg"));
    }

}
