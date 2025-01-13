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

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.controller.utils.KeyCombinationEventHandler;
import com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView;
import com.github.mfl28.boundingboxeditor.ui.EditorImagePaneView;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonType;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.MockedConstruction;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.verify;
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
                        KeyCombinations.navigateNext, KeyCombinations.navigatePrevious,
                        KeyCombinations.showAllBoundingShapes, KeyCombinations.hideAllBoundingShapes,
                        KeyCombinations.showSelectedBoundingShape, KeyCombinations.hideSelectedBoundingShape,
                        KeyCombinations.resetSizeAndCenterImage, KeyCombinations.focusCategoryNameTextField,
                        KeyCombinations.focusCategorySearchField, KeyCombinations.focusTagTextField,
                        KeyCombinations.focusFileSearchField, KeyCombinations.deleteSelectedBoundingShape,
                        KeyCombinations.selectRectangleDrawingMode, KeyCombinations.selectPolygonDrawingMode,
                        KeyCombinations.selectFreehandDrawingMode, KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected,
                        KeyCombinations.changeSelectedBoundingShapeCategory,
                        KeyCombinations.hideNonSelectedBoundingShapes, KeyCombinations.simplifyPolygon,
                        KeyCombinations.saveBoundingShapeAsImage, KeyCombinations.openSettings
                ));

        testOpenSettingsKeyEvent(robot, testinfo);
        testNavigateNextKeyEvent(testinfo, true, true, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, true, true, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testNavigateNextKeyEvent(testinfo, false, true, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, false, true, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testNavigateNextKeyEvent(testinfo, true, false, "wexor-tmg-L-2p8fapOA8-unsplash.jpg");
        testNavigatePreviousKeyEvent(testinfo, true, false, "rachel-hisko-rEM3cK8F1pk-unsplash.jpg");
        testSelectFreehandDrawingModeKeyEvent();
        testSelectRectangleModeKeyEvent();
        testFocusCategorySearchFieldKeyEvent(robot);
        testFocusFileSearchKeyEvent(robot);
        testFocusCategoryNameTextFieldKeyEvent(robot);
        testFocusTagTextFieldKeyEventWhenNoBoundingShapeSelected();
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

        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.equalTo(true));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.POLYGON));

        // Clicking outside the imageview should finalize any drawn shapes.
        robot.clickOn(mainView.getStatusBar());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.equalTo(false));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.NONE));

        BoundingPolygonView polygon = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());

        testFocusTagTextFieldKeyEventWhenBoundingShapeSelected(robot);
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

        robot.clickOn(mainView.getStatusBar());
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
        testSaveCurrentlySelectedBoundingShapeKeyEvent();
        testRemoveCurrentlySelectedBoundingShapeKeyEvent();
        testResetImageViewSizeKeyEvent(robot);
    }

    private void testOpenSettingsKeyEvent(FxRobot robot, TestInfo testinfo) {
        KeyEvent openSettingsKeyEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.openSettings, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(openSettingsKeyEvent));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(settingsStage.isShowing(), Matchers.is(true), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, settingsStage, ButtonType.CANCEL, testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);

        verifyThat(settingsStage.isShowing(), Matchers.is(false));
    }

    private void testSaveCurrentlySelectedBoundingShapeKeyEvent() {
        final AtomicReference<MockedConstruction<FileChooser>> mockedFileChooser = createMockedFileChooser(null);
        verifyThat(mockedFileChooser.get(), Matchers.notNullValue());

        try {
            KeyEvent saveCurrentlySelectedBoundingShapeAsImageEvent = buildKeyEventFromCombination(
                    (KeyCodeCombination) KeyCombinations.saveBoundingShapeAsImage, KeyEvent.KEY_RELEASED);
            Platform.runLater(() -> controller.onRegisterSceneKeyReleased(saveCurrentlySelectedBoundingShapeAsImageEvent));
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(mockedFileChooser.get().constructed().size(), Matchers.equalTo(1));
            verify(mockedFileChooser.get().constructed().get(0)).setInitialFileName("rachel-hisko-rEM3cK8F1pk-unsplash_dummy_2.png");

        } finally {
            mockedFileChooser.get().close();
        }
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

        KeyEvent resetImageViewSizeKeyEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.resetSizeAndCenterImage, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(resetImageViewSizeKeyEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImageView().getFitWidth(), Matchers.equalTo(originalFitWidth));
        verifyThat(mainView.getEditorImageView().getFitHeight(), Matchers.equalTo(originalFitHeight));
    }

    private void testRemoveCurrentlySelectedBoundingShapeKeyEvent() {
        KeyEvent removeSelectedShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.deleteSelectedBoundingShape, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(removeSelectedShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(1));
    }

    private void testHideNonSelectedShapesKeyEvent(BoundingPolygonView polygon, BoundingPolygonView polygon2) {
        KeyEvent hideNonSelectedShapesEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.hideNonSelectedBoundingShapes, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(hideNonSelectedShapesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon, NodeMatchers.isInvisible());
        verifyThat(polygon2, NodeMatchers.isVisible());
    }

    private void testInitiateCategoryChangeKeyEvent(TestInfo testinfo, FxRobot robot) {
        KeyEvent categoryChangeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.changeSelectedBoundingShapeCategory, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(categoryChangeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage changeCategoryStage = timeOutGetTopModalStage(robot, "Change Category", testinfo);
        verifyThat(changeCategoryStage, Matchers.notNullValue(), saveScreenshot(testinfo));
        timeOutLookUpInStageAndClickOn(robot, changeCategoryStage, "Cancel", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Change Category", testinfo);
    }

    private void testSimplifySelectedPolygonKeyEvent(BoundingPolygonView polygon) {
        int numVertices = polygon.getPoints().size() / 2;
        KeyEvent simplifySelectedPolygonEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.simplifyPolygon, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(simplifySelectedPolygonEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numVertices));
    }

    private void testRemovePolygonVerticesKeyEvent(FxRobot robot, Double[] targetImageViewPointRatios, BoundingPolygonView polygon) {
        int numInitialVertices = targetImageViewPointRatios.length / 2;
        robot.clickOn("#vertex-handle", MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        KeyEvent removeEditingVerticesEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.removeEditingVerticesWhenBoundingPolygonSelected, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(removeEditingVerticesEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.getPoints().size() / 2, Matchers.equalTo(numInitialVertices - 1));
    }

    private void testShowAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showAllBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.showAllBoundingShapes, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(showAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideAllBoundingShapesKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideAllBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.hideAllBoundingShapes, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(hideAllBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testShowSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent showSelectedBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.showSelectedBoundingShape, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(showSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isVisible());
    }

    private void testHideSelectedBoundingShapeKeyEvent(BoundingPolygonView polygon) {
        KeyEvent hideSelectedBoundingShapeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.hideSelectedBoundingShape, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(hideSelectedBoundingShapeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(polygon.isSelected(), Matchers.is(true));
        verifyThat(polygon, NodeMatchers.isInvisible());
    }

    private void testSelectPolygonModeKeyEvent() {
        KeyEvent selectPolygonModeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.selectPolygonDrawingMode, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(selectPolygonModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getPolygonModeButton().isSelected(), Matchers.is(true));
    }

    private void testFocusTagTextFieldKeyEventWhenNoBoundingShapeSelected() {
        KeyEvent focusTagTextFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.focusTagTextField, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(focusTagTextFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        // No bounding-shapes are selected, therefore tag text-field should be disabled.
        verifyThat(controller.getView().getTagInputField().isFocused(), Matchers.is(false));
    }

    private void testFocusTagTextFieldKeyEventWhenBoundingShapeSelected(FxRobot robot) {
        KeyEvent focusTagTextFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.focusTagTextField, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(focusTagTextFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getTagInputField().isFocused(), Matchers.is(true));

        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getTagInputField().isFocused(), Matchers.is(false));
        verifyThat(controller.getView().getTagInputField().getText(), Matchers.nullValue());
    }

    private void testFocusCategoryNameTextFieldKeyEvent(FxRobot robot) {
        KeyEvent focusCategoryNameTextField = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.focusCategoryNameTextField, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(focusCategoryNameTextField));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getObjectCategoryInputField().isFocused(), Matchers.is(true));

        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getObjectCategoryInputField().isFocused(), Matchers.is(false));
        verifyThat(controller.getView().getObjectCategoryInputField().getText(), Matchers.nullValue());
    }

    private void testFocusFileSearchKeyEvent(FxRobot robot) {
        KeyEvent focusFileSearchFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.focusFileSearchField, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(focusFileSearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getImageFileSearchField().isFocused(), Matchers.is(true));

        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getImageFileSearchField().isFocused(), Matchers.is(false));
        verifyThat(controller.getView().getImageFileSearchField().getText(), Matchers.nullValue());
    }

    private void testFocusCategorySearchFieldKeyEvent(FxRobot robot) {
        KeyEvent focusCategorySearchFieldEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.focusCategorySearchField, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(focusCategorySearchFieldEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getCategorySearchField().isFocused(), Matchers.is(true));

        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getCategorySearchField().isFocused(), Matchers.is(false));
        verifyThat(controller.getView().getCategorySearchField().getText(), Matchers.nullValue());
    }

    private void testSelectRectangleModeKeyEvent() {
        KeyEvent selectRectangleModeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.selectRectangleDrawingMode, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(selectRectangleModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getRectangleModeButton().isSelected(), Matchers.is(true));
    }

    private void testSelectFreehandDrawingModeKeyEvent() {
        KeyEvent selectFreehandDrawingModeEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.selectFreehandDrawingMode, KeyEvent.KEY_RELEASED);
        Platform.runLater(() -> controller.onRegisterSceneKeyReleased(selectFreehandDrawingModeEvent));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(controller.getView().getEditor().getEditorToolBar().getFreehandModeButton().isSelected(), Matchers.is(true));
    }

    private void testNavigatePreviousKeyEvent(TestInfo testinfo, boolean keyReleased, boolean ctrlReleased, String expectedTargetImageName) {
        KeyEvent navigatePreviousPressedEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.navigatePrevious, KeyEvent.KEY_PRESSED);
        KeyEvent navigatePreviousReleasedEventTemplate = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.navigatePrevious, KeyEvent.KEY_RELEASED);

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
        KeyEvent navigateNextPressedEvent = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.navigateNext, KeyEvent.KEY_PRESSED);
        KeyEvent navigateNextReleasedEventTemplate = buildKeyEventFromCombination((KeyCodeCombination) KeyCombinations.navigateNext, KeyEvent.KEY_RELEASED);

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
