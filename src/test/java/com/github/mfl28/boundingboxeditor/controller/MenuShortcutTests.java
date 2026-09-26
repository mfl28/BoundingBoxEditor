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
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class MenuShortcutTests extends BoundingBoxEditorTestBase {
    private static final String CATEGORY_NAME = "Test";
    private static final boolean IS_MAC = System.getProperty("os.name").startsWith("Mac");

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        // As in BoundingBoxEditorApp: these tests check how the key handling and the menu accelerators interact.
        stage.getScene().setOnKeyPressed(controller::onRegisterSceneKeyPressed);
        stage.getScene().setOnKeyReleased(controller::onRegisterSceneKeyReleased);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onShortcutsShownInMenus_ShouldTriggerTheirActionOnlyOnce(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);

        verifyThat(mainView.getUndoMenuItem().getAccelerator(), Matchers.equalTo(KeyCombinations.undo),
                   saveScreenshot(testinfo));
        verifyThat(mainView.getRedoMenuItem().getAccelerator(), Matchers.equalTo(KeyCombinations.redo),
                   saveScreenshot(testinfo));

        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "first box", testinfo);
        moveRelativeToImageView(robot, new Point2D(0.6, 0.6), new Point2D(0.8, 0.8));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2 && !mainView.getUndoMenuItem().isDisable(),
                  "second box", testinfo);

        // Handled by the key handling on release; the menu's accelerator must not undo a second time.
        pressShortcut(KeyCombinations.undo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "one box after undo", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));

        pressShortcut(KeyCombinations.redo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2, "two boxes after redo", testinfo);

        pressShortcut(KeyCombinations.openSettings);
        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        verifyThat(Window.getWindows().stream().filter(window -> window instanceof Stage stage
                           && "Settings".equals(stage.getTitle())).count(), Matchers.equalTo(1L),
                   saveScreenshot(testinfo));
        timeOutClickOnButtonInDialogStage(robot, settingsStage, ButtonType.CANCEL, testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);
    }

    @Test
    void onOpenFolderAndExportShortcuts_ShouldRequestTheDialogs(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.4, 0.4));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "box", testinfo);

        try(MockedFileDialogs fileDialogs = mockCancelledFileDialogs()) {
            pressShortcut(KeyCombinations.openImageFolder);
            waitUntil(() -> fileDialogs.directoryChoosers().constructed().size() == 1, "folder dialog", testinfo);

            // Without an earlier export, the export shortcut uses the Pascal VOC format (a folder dialog).
            verifyThat(getExportInLastFormatMenuItem().getText(), Matchers.equalTo("Export as Pascal-VOC..."),
                       saveScreenshot(testinfo));
            pressShortcut(KeyCombinations.exportAnnotationsInLastFormat);
            waitUntil(() -> fileDialogs.directoryChoosers().constructed().size() == 2, "Pascal VOC export",
                      testinfo);
            verifyThat(fileDialogs.fileChoosers().constructed().size(), Matchers.equalTo(0), saveScreenshot(testinfo));

            timeOutClickOn(robot, "#file-menu", testinfo);
            timeOutClickOn(robot, "#file-export-annotations-menu", testinfo);
            timeOutMoveTo(robot, "#pvoc-export-menu-item", testinfo);
            timeOutClickOn(robot, "#json-export-menu-item", testinfo);
            waitUntil(() -> fileDialogs.fileChoosers().constructed().size() == 1, "JSON export from the menu",
                      testinfo);
            verifyThat(getExportInLastFormatMenuItem().getText(), Matchers.equalTo("Export as JSON..."),
                       saveScreenshot(testinfo));

            // Now the export shortcut uses JSON (a file dialog).
            pressShortcut(KeyCombinations.exportAnnotationsInLastFormat);
            waitUntil(() -> fileDialogs.fileChoosers().constructed().size() == 2, "JSON export", testinfo);
            verifyThat(fileDialogs.directoryChoosers().constructed().size(), Matchers.equalTo(2),
                       saveScreenshot(testinfo));
        }
    }

    @Test
    void onOpenRecentFolder_ShouldListOpenedFoldersAndReopenThem(FxRobot robot, TestInfo testinfo,
                                                                  @TempDir Path tempDir) {
        waitUntilCurrentImageIsLoaded(testinfo);

        final File loadedFolder = new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile())
                .getAbsoluteFile();
        verifyThat(getRecentFolderPaths(), Matchers.equalTo(List.of(loadedFolder.getPath())),
                   saveScreenshot(testinfo));

        final File otherFolder = new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_3).getFile())
                .getAbsoluteFile();
        robot.interact(() -> controller.onRegisterOpenRecentImageFolderAction(otherFolder));
        waitUntil(() -> model.getImageFileNameSet().contains("wexor-tmg-L-2p8fapOA8-unsplash.jpg"),
                  "other folder opened", testinfo);
        waitUntil(() -> getRecentFolderPaths().equals(List.of(otherFolder.getPath(), loadedFolder.getPath())),
                  "other folder first", testinfo);

        // Opening a recent folder from the menu.
        timeOutClickOn(robot, "#file-menu", testinfo);
        timeOutClickOn(robot, "#file-open-recent-menu", testinfo);
        timeOutClickOn(robot, loadedFolder.getPath(), testinfo);
        waitUntil(() -> model.getImageFileNameSet().contains("austin-neill-685084-unsplash.jpg"),
                  "first folder reopened", testinfo);
        waitUntil(() -> getRecentFolderPaths().equals(List.of(loadedFolder.getPath(), otherFolder.getPath())),
                  "first folder first again", testinfo);

        // A folder that was deleted meanwhile is reported and removed.
        final File deletedFolder = tempDir.resolve("deleted").toFile();
        // The error dialog blocks until it is closed, so this doesn't wait for the call to return.
        Platform.runLater(() -> controller.onRegisterOpenRecentImageFolderAction(deletedFolder));
        final Stage errorStage = timeOutGetTopModalStage(robot, "Image Folder Not Found", testinfo);
        timeOutClickOnButtonInDialogStage(robot, errorStage, ButtonType.OK, testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);

        robot.interact(controller::onRegisterClearRecentImageFoldersAction);
        verifyThat(getRecentFolderPaths(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(getOpenRecentMenu().isDisable(), Matchers.is(true), saveScreenshot(testinfo));
    }

    /**
     * Sends a shortcut's key press and release through the window's event dispatch, including the menu
     * accelerators. (The robot's real key presses depend on the keyboard layout.)
     *
     * @param shortcut the shortcut
     */
    private void pressShortcut(KeyCombination shortcut) {
        final KeyCodeCombination keyCodeShortcut = (KeyCodeCombination) shortcut;

        for(EventType<KeyEvent> type : List.of(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED)) {
            // A dialog opened by the shortcut blocks until it is closed, so this doesn't wait for the dispatch.
            Platform.runLater(() -> {
                final Node target = Objects.requireNonNullElse(mainView.getScene().getFocusOwner(), mainView);
                Event.fireEvent(target, new KeyEvent(target, target, type, "", "", keyCodeShortcut.getCode(),
                        keyCodeShortcut.getShift() == KeyCombination.ModifierValue.DOWN,
                        keyCodeShortcut.getShortcut() == KeyCombination.ModifierValue.DOWN && !IS_MAC, false,
                        keyCodeShortcut.getShortcut() == KeyCombination.ModifierValue.DOWN && IS_MAC));
            });
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    private Menu getOpenRecentMenu() {
        return (Menu) findFileMenuItem("file-open-recent-menu");
    }

    private MenuItem getExportInLastFormatMenuItem() {
        return findFileMenuItem("file-export-in-last-format-menu-item");
    }

    private MenuItem findFileMenuItem(String id) {
        final MenuBar menuBar = (MenuBar) mainView.lookup("#main-menu-bar");
        return menuBar.getMenus().getFirst().getItems().stream().filter(item -> id.equals(item.getId())).findFirst()
                      .orElseThrow();
    }

    private List<String> getRecentFolderPaths() {
        return getOpenRecentMenu().getItems().stream()
                                  .takeWhile(item -> !(item instanceof SeparatorMenuItem))
                                  .map(MenuItem::getText)
                                  .toList();
    }

    private void waitUntil(Callable<Boolean> condition, String description, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      condition),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected state not reached: " +
                                                                                   description));
    }
}
