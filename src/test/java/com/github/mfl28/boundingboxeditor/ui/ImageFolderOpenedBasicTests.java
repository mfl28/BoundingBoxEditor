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
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class ImageFolderOpenedBasicTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onImageFolderOpened_UIElementsShouldHaveCorrectState(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyImageMetaDataLoaded(testinfo);
        verifyNodeVisibilities(testinfo);
        verifyMenuBarFunctionality(robot, testinfo);
        verifyCategorySelectorState(testinfo);
        verifyCategorySelectorEnterNewCategoryFunctionality(robot, testinfo);
        verifyImageSidePanelSearchFunctionality(robot, testinfo);
        verifyCategorySearchFunctionality(robot, testinfo);
    }

    @Test
    void onImageFolderOpened_WhenImageFileChanges_ShouldForceReloadFolder(FxRobot robot, @TempDir File tempDir,
                                                                          TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        final File sourceImage = model.getCurrentImageFile();

        Assertions.assertDoesNotThrow(() -> Files.copy(sourceImage.toPath(), tempDir.toPath().resolve("foo.jpg")),
                                      () -> saveScreenshotAndReturnMessage(testinfo, "Could not " +
                                              "copy image file to temporary " +
                                              "directory."));

        verifyThat(Files.isRegularFile(tempDir.toPath().resolve("foo.jpg")), Matchers.is(true),
                   saveScreenshot(testinfo));

        loadImageFolder(tempDir);

        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile(), Matchers.equalTo(tempDir.toPath().resolve("foo.jpg").toFile()),
                   saveScreenshot(testinfo));

        // Verify that exactly one Image file change watcher thread is running:
        timeOutAssertThreadCount("ImageFileChangeWatcher", 1, testinfo);

        // Rename the loaded image file:
        Assertions.assertDoesNotThrow(() -> Files.move(tempDir.toPath().resolve("foo.jpg"),
                                                       tempDir.toPath().resolve("bar.jpg")),
                                      () -> saveScreenshotAndReturnMessage(testinfo, "Could not rename image file in " +
                                              "temporary directory."));

        final Stage alert = timeOutGetTopModalStage(robot, "Image Files Changed", testinfo);
        timeOutLookUpInStageAndClickOn(robot, alert, "OK", testinfo);
        timeOutAssertTopModalStageClosed(robot, "Image Files Changed", testinfo);

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded(testinfo);

        verifyThat(model.getCurrentImageFile(), Matchers.equalTo(tempDir.toPath().resolve("bar.jpg").toFile()),
                   saveScreenshot(testinfo));
        timeOutAssertThreadCount("ImageFileChangeWatcher", 1, testinfo);

        timeOutAssertNoTopModelStage(robot, testinfo);

        // Delete the loaded image file:
        Assertions.assertDoesNotThrow(() -> Files.delete(tempDir.toPath().resolve("bar.jpg")),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Could not delete image in temporary " +
                                                                                   "directory."));

        final Stage alert2 = timeOutGetTopModalStage(robot, "Image Files Changed", testinfo);
        timeOutClickOnButtonInDialogStage(robot, alert2, ButtonType.OK, testinfo);

        timeOutAssertTopModalStageClosed(robot, "Image Files Changed", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorAlert = timeOutGetTopModalStage(robot, "Image Folder Loading Error", testinfo);
        timeOutClickOnButtonInDialogStage(robot, errorAlert, ButtonType.OK, testinfo);

        timeOutAssertTopModalStageClosed(robot, "Image Folder Loading Error", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertThreadCount("ImageFileChangeWatcher", 0, testinfo);

        verifyThat(model.containsImageFiles(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.isWorkspaceVisible(), Matchers.is(false), saveScreenshot(testinfo));

        loadImageFolder(tempDir);

        final Stage errorAlert1 = timeOutGetTopModalStage(robot, "Image Folder Loading Error", testinfo);
        timeOutClickOnButtonInDialogStage(robot, errorAlert1, ButtonType.OK, testinfo);

        timeOutAssertTopModalStageClosed(robot, "Image Folder Loading Error", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertThreadCount("ImageFileChangeWatcher", 0, testinfo);
    }

    private void verifyNodeVisibilities(TestInfo testinfo) {
        verifyThat("#main-menu-bar", NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat("#work-space", NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat("#status-panel", NodeMatchers.isVisible(), saveScreenshot(testinfo));
    }

    private void verifyImageMetaDataLoaded(TestInfo testinfo) {
        verifyThat(model.getImageFileNameToMetaDataMap().size(), Matchers.equalTo(4), saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap(), Matchers.hasKey("austin-neill-685084-unsplash.jpg"),
                   saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap(), Matchers.hasKey("caleb-george-316073-unsplash.jpg"),
                   saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap(), Matchers.hasKey("nico-bhlr-1067059-unsplash.jpg"),
                   saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap(), Matchers.hasKey("tyler-nix-582593-unsplash.jpg"),
                   saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap().values().stream().allMatch(ImageMetaData::hasDetails),
                   Matchers.is(true));
    }

    private void verifyMenuBarFunctionality(FxRobot robot, TestInfo testinfo) {
        timeOutClickOn(robot, "View", testinfo);

        WaitForAsyncUtils.waitForFxEvents();

        CheckMenuItem fitWindowItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Maximize Images");
        assertTrue(fitWindowItem.getParentMenu().isShowing(), () -> saveScreenshotAndReturnMessage(testinfo, "View " +
                "menu not showing"));
        assertTrue(fitWindowItem.isVisible(),
                   () -> saveScreenshotAndReturnMessage(testinfo, "Fit window item not visible"));
        assertFalse(fitWindowItem.isDisable(),
                    () -> saveScreenshotAndReturnMessage(testinfo, "Fit window item not enabled"));
        assertTrue(fitWindowItem.isSelected(), () -> saveScreenshotAndReturnMessage(testinfo, "Fit window item not " +
                "selected"));

        CheckMenuItem imageExplorerItem = (CheckMenuItem) getSubMenuItem(robot, "View", "Show Images Panel");
        assertTrue(imageExplorerItem.isVisible(),
                   () -> saveScreenshotAndReturnMessage(testinfo, "Image explorer item not " +
                           "visible"));
        assertFalse(imageExplorerItem.isDisable(),
                    () -> saveScreenshotAndReturnMessage(testinfo, "Image explorer item not " +
                            "enabled"));
        assertTrue(imageExplorerItem.isSelected(),
                   () -> saveScreenshotAndReturnMessage(testinfo, "Image explorer item not " +
                           "selected"));

        robot.rightClickOn();

        WaitForAsyncUtils.waitForFxEvents();

        assertFalse(fitWindowItem.getParentMenu().isShowing(), () -> saveScreenshotAndReturnMessage(testinfo,
                                                                                                    "View menu is " +
                                                                                                            "showing"));

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        MenuItem openFolderItem = getSubMenuItem(robot, "File", "Open Folder...");
        assertTrue(openFolderItem.getParentMenu().isShowing(), () -> saveScreenshotAndReturnMessage(testinfo, "File " +
                "menu is not showing"));
        assertTrue(openFolderItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Open folder item not " +
                "visible"));
        assertFalse(openFolderItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Open folder item " +
                "not enabled"));

        timeOutClickOn(robot, "#file-open-folder-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitForFxEvents();

        MenuItem exportItem = getSubMenuItem(robot, "File", "Export Annotations");
        assertTrue(exportItem.isVisible(),
                   () -> saveScreenshotAndReturnMessage(testinfo, "Export annotations item not " +
                           "visible"));
        assertFalse(exportItem.isDisable(),
                    () -> saveScreenshotAndReturnMessage(testinfo, "Export annotations item not " +
                            "enabled"));

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-export-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#pvoc-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage errorDialogStage = timeOutGetTopModalStage(robot, "Save Error", testinfo);
        verifyThat(errorDialogStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, errorDialogStage, ButtonType.OK, testinfo);
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

        Stage errorDialogStage2 = timeOutGetTopModalStage(robot, "Save Error", testinfo);
        verifyThat(errorDialogStage2, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, errorDialogStage2, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Save Error", testinfo);

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-export-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "#pvoc-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#json-export-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage errorDialogStage3 = timeOutGetTopModalStage(robot, "Save Error", testinfo);
        verifyThat(errorDialogStage3, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, errorDialogStage3, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Save Error", testinfo);

        MenuItem settingsItem = getSubMenuItem(robot, "File", "Settings");

        assertTrue(settingsItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Setttings item not " +
                "visible"));
        assertFalse(settingsItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Settings item not " +
                "enabled"));

        MenuItem exitItem = getSubMenuItem(robot, "File", "Exit");
        assertTrue(exitItem.isVisible(), () -> saveScreenshotAndReturnMessage(testinfo, "Exit item not visible"));
        assertFalse(exitItem.isDisable(), () -> saveScreenshotAndReturnMessage(testinfo, "Exit item not enabled"));
    }

    private void verifyCategorySelectorState(TestInfo testinfo) {
        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0), saveScreenshot(testinfo));
    }

    private void verifyCategorySelectorEnterNewCategoryFunctionality(FxRobot robot, TestInfo testinfo) {
        // Enter category without valid name
        enterNewCategory(robot, null, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage = timeOutGetTopModalStage(robot, "Category Creation Error", testinfo);
        verifyThat(categoryCreationErrorStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, categoryCreationErrorStage, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error", testinfo);

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0), saveScreenshot(testinfo));

        // Enter valid category name
        enterNewCategory(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1), saveScreenshot(testinfo));
        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Test"), saveScreenshot(testinfo));

        // Enter duplicate category name
        enterNewCategory(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage2 = timeOutGetTopModalStage(robot, "Category Creation Error", testinfo);
        verifyThat(categoryCreationErrorStage2, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, categoryCreationErrorStage2, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error", testinfo);

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(1), saveScreenshot(testinfo));

        // Renaming a category
        timeOutClickOn(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasTableCell("Dummy"), saveScreenshot(testinfo));

        // Entering a category with a name that previously existed but is not currently in the category-selector
        enterNewCategory(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // There should be no error message
        verifyThat(getTopModalStage(robot, "Category Creation Error"), Matchers.nullValue(), saveScreenshot(testinfo));

        // Renaming a category to a name that already exists
        timeOutClickOn(robot, "Test", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.write("Dummy").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage3 = timeOutGetTopModalStage(robot, "Category Creation Error", testinfo);
        verifyThat(categoryCreationErrorStage3, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, categoryCreationErrorStage3, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error", testinfo);

        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("Test"),
                   saveScreenshot(testinfo));

        // Renaming a category to a blank string
        timeOutClickOn(robot, "Dummy", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("Dummy"),
                   saveScreenshot(testinfo));

        timeOutClickOn(robot, "Dummy", testinfo);
        robot.write("    ").push(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        Stage categoryCreationErrorStage4 = timeOutGetTopModalStage(robot, "Category Creation Error", testinfo);
        verifyThat(categoryCreationErrorStage4, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, categoryCreationErrorStage4, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Category Creation Error", testinfo);

        // Deleting remaining categories
        timeOutClickOnNth(robot, "#delete-button", 1, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn();
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getRowContextMenu().isShowing(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));

        timeOutClickOn(robot, "#delete-button", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getObjectCategoryTable(), TableViewMatchers.hasNumRows(0), saveScreenshot(testinfo));
    }

    private void verifyImageSidePanelSearchFunctionality(FxRobot robot, TestInfo testinfo) {
        final TextField fileSearchField = mainView.getImageFileSearchField();

        verifyThat(fileSearchField.getPromptText(), Matchers.equalTo("Search File"), saveScreenshot(testinfo));
        verifyThat(fileSearchField, NodeMatchers.isNotFocused(), saveScreenshot(testinfo));

        robot.clickOn(fileSearchField);
        verifyThat(fileSearchField, NodeMatchers.isFocused(), saveScreenshot(testinfo));

        robot.write("nico");
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo("nico-bhlr-1067059-unsplash.jpg"),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentImage().getUrl(), Matchers.endsWith("nico-bhlr-1067059-unsplash.jpg"),
                   saveScreenshot(testinfo));
    }

    private void verifyCategorySearchFunctionality(FxRobot robot, TestInfo testinfo) {
        enterNewCategory(robot, "AAA", testinfo);
        enterNewCategory(robot, "ABB", testinfo);
        enterNewCategory(robot, "ABC", testinfo);

        final TextField categorySearchField = mainView.getCategorySearchField();
        verifyThat(categorySearchField.getPromptText(), Matchers.equalTo("Search Category"), saveScreenshot(testinfo));

        robot.clickOn(categorySearchField).write("A");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("AAA"),
                   saveScreenshot(testinfo));

        robot.write("B");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("ABB"),
                   saveScreenshot(testinfo));

        robot.write("C");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getObjectCategoryTable().getSelectedCategory().getName(), Matchers.equalTo("ABC"),
                   saveScreenshot(testinfo));
    }
}
