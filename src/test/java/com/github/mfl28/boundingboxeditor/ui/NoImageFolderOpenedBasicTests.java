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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class NoImageFolderOpenedBasicTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
    }

    @Test
    void onMenuItemsClicked_ShouldCorrectlyApplyVisibilityAndShowDialogueWindows(FxRobot robot, TestInfo testinfo) {
        verifyNodeVisibilities(testinfo);
        verifyMenuBarFunctionality(robot, testinfo);
    }

    @Test
    void onDragFolderIntoView_ShouldCorrectlyImportImageFiles(FxRobot robot, TestInfo testInfo) {
        EventHandler<? super MouseEvent> dragDetectedHandler = mainView.getOnDragDetected();
        try {
            Map<DataFormat, Object> dataMap = new HashMap<>();
            dataMap.put(DataFormat.FILES, List.of(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_3).getFile())));

            setDummyMainViewDragDetector(dataMap);

            robot.moveTo(getScreenPointFromRatios(mainView, new Point2D(0.5, 0.25)))
                    .press(MouseButton.PRIMARY).drag(mainView, MouseButton.PRIMARY).dropTo(mainView).release(MouseButton.PRIMARY);
            WaitForAsyncUtils.waitForFxEvents();

            waitUntilCurrentImageIsLoaded(testInfo);

            verifyThat(model.getImageFileNameSet(), Matchers.containsInAnyOrder(
                    "rachel-hisko-rEM3cK8F1pk-unsplash.jpg",
                    "wexor-tmg-L-2p8fapOA8-unsplash.jpg"));
        } finally {
            mainView.setOnDragDetected(dragDetectedHandler);
        }
    }

    @Test
    void onDragMultipleFoldersIntoView_ShouldDoNothing(FxRobot robot) {
        EventHandler<? super MouseEvent> dragDetectedHandler = mainView.getOnDragDetected();
        try {
            Map<DataFormat, Object> dataMap = new HashMap<>();
            dataMap.put(DataFormat.FILES, List.of(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()),
                    new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_2).getFile())));

            setDummyMainViewDragDetector(dataMap);

            robot.moveTo(getScreenPointFromRatios(mainView, new Point2D(0.5, 0.25)))
                    .press(MouseButton.PRIMARY).drag(mainView, MouseButton.PRIMARY).dropTo(mainView).release(MouseButton.PRIMARY);
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(model.getImageFileNameSet(), Matchers.empty());
        } finally {
            mainView.setOnDragDetected(dragDetectedHandler);
        }
    }


    @Test
    void onDragNonFolderFileIntoView_ShouldDoNothing(FxRobot robot) {
        EventHandler<? super MouseEvent> dragDetectedHandler = mainView.getOnDragDetected();
        try {
            Map<DataFormat, Object> dataMap = new HashMap<>();
            dataMap.put(DataFormat.FILES, List.of(
                    new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1 + "/austin-neill-685084-unsplash.jpg").getFile())));

            setDummyMainViewDragDetector(dataMap);

            robot.moveTo(getScreenPointFromRatios(mainView, new Point2D(0.5, 0.25)))
                    .press(MouseButton.PRIMARY).drag(mainView, MouseButton.PRIMARY).dropTo(mainView).release(MouseButton.PRIMARY);
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(model.getImageFileNameSet(), Matchers.empty());
        } finally {
            mainView.setOnDragDetected(dragDetectedHandler);
        }
    }

    @Test
    void onEmptyDragIntoView_ShouldDoNothing(FxRobot robot) {
        EventHandler<? super MouseEvent> dragDetectedHandler = mainView.getOnDragDetected();
        try {
            Map<DataFormat, Object> dataMap = new HashMap<>();

            setDummyMainViewDragDetector(dataMap);

            robot.moveTo(getScreenPointFromRatios(mainView, new Point2D(0.5, 0.25)))
                    .press(MouseButton.PRIMARY).drag(mainView, MouseButton.PRIMARY).dropTo(mainView).release(MouseButton.PRIMARY);
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(model.getImageFileNameSet(), Matchers.empty());
        } finally {
            mainView.setOnDragDetected(dragDetectedHandler);
        }
    }

    private void setDummyMainViewDragDetector(Map<DataFormat, Object> content) {
        mainView.setOnDragDetected(event -> {
            Dragboard dragboard = mainView.startDragAndDrop(TransferMode.LINK);
            dragboard.setContent(content);
        });
    }

    private void verifyMenuBarFunctionality(FxRobot robot, TestInfo testinfo) {
        timeOutClickOn(robot, "#file-menu", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem openFolderItem = getSubMenuItem(robot, "File", "Open Folder...");
        assertTrue(openFolderItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Open folder item not " +
                "visible"));
        assertFalse(openFolderItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Open folder item not " +
                "enabled"));

        timeOutClickOn(robot, "#file-open-folder-menu-item", testinfo);
        robot.push(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem saveItem = getSubMenuItem(robot, "File", "Export Annotations");
        assertTrue(saveItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Save item not visible"));
        assertFalse(saveItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Save item not enabled"));

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-export-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#pvoc-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage = timeOutGetTopModalStage(robot, "Save Error", testinfo);
        verifyThat(categoryCreationErrorStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Save Error", testinfo);

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-export-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "#pvoc-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#yolo-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage2 = timeOutGetTopModalStage(robot, "Save Error", testinfo);
        verifyThat(categoryCreationErrorStage2, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, categoryCreationErrorStage2, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Save Error", testinfo);

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Exit item not visible"));
        assertFalse(exitItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Exit item not enabled"));

        timeOutClickOn(robot, "#view-menu", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem fitWindowItem = getSubMenuItem(robot, "View", "Maximize Images");
        assertTrue(fitWindowItem.isVisible(),
                () -> saveScreenshotAndReturnMessage(testinfo, "Maximize images item not " +
                        "visible"));
        assertTrue(fitWindowItem.isDisable(),
                () -> saveScreenshotAndReturnMessage(testinfo, "Maximize images item not " +
                        "disabled"));

        MenuItem imageExplorerItem = getSubMenuItem(robot, "View", "Show Images Panel");
        assertTrue(imageExplorerItem.isVisible(),
                () -> saveScreenshotAndReturnMessage(testinfo, "Image explorer item not " +
                        "visible"));
        assertTrue(imageExplorerItem.isDisable(),
                () -> saveScreenshotAndReturnMessage(testinfo, "Image explorer item not " +
                        "disabled"));
    }

    private void verifyNodeVisibilities(TestInfo testinfo) {
        verifyThat("#main-menu-bar", NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat("#work-space", NodeMatchers.isInvisible(), saveScreenshot(testinfo));
        verifyThat("#status-panel", NodeMatchers.isInvisible(), saveScreenshot(testinfo));
    }
}
