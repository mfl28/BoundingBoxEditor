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
import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class AutosaveTests extends BoundingBoxEditorTestBase {
    private static final String IMAGE_FILE_NAME = "austin-neill-685084-unsplash.jpg";

    @Start
    void start(Stage stage) {
        super.onStart(stage);
    }

    @Test
    void onEditingAnnotations_ShouldSaveThemAutomaticallyUntilTheyAreExported(FxRobot robot, TestInfo testinfo,
                                                                              @TempDir Path tempDir) {
        final File folder = testImageFolder();
        final AutosaveStore store = controller.getAutosaveStore();
        robot.interact(() -> controller.loadImageFiles(folder));
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Boat", testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.5, 0.5));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "the drawn box", testinfo);

        // (Instead of waiting for the autosave delay.)
        robot.interact(controller::autosaveNow);
        controller.waitForAutosave();

        verifyThat(Files.isRegularFile(store.fileFor(folder)), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));

        // Exported annotations need no autosave.
        Platform.runLater(() -> controller.initiateAnnotationExport(tempDir.resolve("annotations.json").toFile(),
                                                                    ImageAnnotationSaveStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);
        controller.waitForAutosave();

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(Files.exists(store.fileFor(folder)), Matchers.is(false), saveScreenshot(testinfo));
    }

    @Test
    void onOpeningFolderWithAutosave_WhenRestoring_ShouldLoadTheUnsavedAnnotations(FxRobot robot,
                                                                                    TestInfo testinfo)
            throws Exception {
        final File folder = testImageFolder();
        writeAutosave(folder);

        robot.interact(() -> controller.loadImageFiles(folder));
        final Stage dialog = timeOutGetTopModalStage(robot, Controller.RESTORE_AUTOSAVE_DIALOG_TITLE, testinfo);
        timeOutLookUpInStageAndClickOn(robot, dialog, "Yes", testinfo);

        waitUntil(() -> model.getImageFileNameToAnnotationMap().containsKey(IMAGE_FILE_NAME), "the restored annotation",
                  testinfo);
        final List<BoundingShapeData> shapes = model.getImageFileNameToAnnotationMap().get(IMAGE_FILE_NAME)
                                                    .getBoundingShapeData();
        verifyThat(shapes.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(shapes.getFirst().getTags(), Matchers.contains("difficult"), saveScreenshot(testinfo));
        // Still not exported, so still unsaved, and the autosave is kept.
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
        controller.waitForAutosave();
        verifyThat(Files.isRegularFile(controller.getAutosaveStore().fileFor(folder)), Matchers.is(true),
                   saveScreenshot(testinfo));
        // The autosave folder doesn't become the folder for importing annotations.
        verifyThat(controller.getIoMetaData().getDefaultAnnotationLoadingDirectory(),
                   Matchers.not(Matchers.equalTo(controller.getAutosaveStore().getDirectory().toFile())),
                   saveScreenshot(testinfo));
    }

    @Test
    void onOpeningFolderWithAutosave_WhenNotRestoring_ShouldDeleteIt(FxRobot robot, TestInfo testinfo)
            throws Exception {
        final File folder = testImageFolder();
        writeAutosave(folder);

        robot.interact(() -> controller.loadImageFiles(folder));
        final Stage dialog = timeOutGetTopModalStage(robot, Controller.RESTORE_AUTOSAVE_DIALOG_TITLE, testinfo);
        timeOutLookUpInStageAndClickOn(robot, dialog, "No", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        controller.waitForAutosave();

        verifyThat(model.getImageFileNameToAnnotationMap().isEmpty(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(Files.exists(controller.getAutosaveStore().fileFor(folder)), Matchers.is(false),
                   saveScreenshot(testinfo));
    }

    private File testImageFolder() {
        return new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile());
    }

    private void writeAutosave(File folder) throws Exception {
        final ObjectCategory category = new ObjectCategory("Boat", Color.ORANGE);
        final ImageMetaData image = new ImageMetaData(IMAGE_FILE_NAME, folder.getName(),
                                                      new File(folder, IMAGE_FILE_NAME).toURI().toString(), 3854,
                                                      2033, 3);
        final BoundingBoxData box = new BoundingBoxData(category, 0.2, 0.2, 0.6, 0.6, List.of("difficult"));
        controller.getAutosaveStore().save(folder, new ImageAnnotationData(
                List.of(new ImageAnnotation(image, List.of(box))), Map.of("Boat", 1), Map.of("Boat", category)));
    }

    private void waitUntil(Callable<Boolean> condition, String description, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                        condition),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected state not reached: " +
                                                                                   description));
    }
}
