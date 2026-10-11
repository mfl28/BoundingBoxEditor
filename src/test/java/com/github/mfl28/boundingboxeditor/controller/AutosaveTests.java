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
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getSavedStatusDescription(),
                   Matchers.startsWith("The annotations are not exported yet. They were saved automatically at "),
                   saveScreenshot(testinfo));
        // An autosave from an earlier day is shown with its date.
        robot.interact(() -> mainView.getStatusBar().setAutosaveTime(Instant.now().minus(2, ChronoUnit.DAYS)));
        verifyThat(mainView.getStatusBar().getSavedStatusDescription(),
                   Matchers.startsWith("The annotations are not exported yet. They were saved automatically on "),
                   saveScreenshot(testinfo));

        // Exported annotations need no autosave.
        Platform.runLater(() -> controller.initiateAnnotationExport(tempDir.resolve("annotations.json").toFile(),
                                                                    ImageAnnotationSaveStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);
        controller.waitForAutosave();

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(Files.exists(store.fileFor(folder)), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().getSavedStatusDescription(),
                   Matchers.equalTo("All annotations are exported."), saveScreenshot(testinfo));
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
        verifyThat(mainView.getStatusBar().getSavedStatusDescription(),
                   Matchers.startsWith("The annotations are not exported yet. They were saved automatically "),
                   saveScreenshot(testinfo));
        // The autosave folder doesn't become the folder for importing annotations.
        verifyThat(controller.getIoMetaData().getDefaultAnnotationLoadingDirectory(),
                   Matchers.not(Matchers.equalTo(controller.getAutosaveStore().directory().toFile())),
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

    @Test
    void onRestoringAutosaveWithoutAsking_ShouldPlaceTheShapesOnTheLoadedImage(FxRobot robot, TestInfo testinfo)
            throws Exception {
        final File folder = testImageFolder();
        writeAutosave(folder);

        // The annotations may be restored before the image is loaded (no dialog in between, as at startup).
        robot.interact(() -> controller.loadImageFilesAndRestoreAutosave(folder));
        waitUntilCurrentImageIsLoaded(testinfo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "the restored box", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        final Bounds imageBounds = mainView.getEditorImageView().getBoundsInParent();
        final Bounds boxBounds = ((Node) mainView.getCurrentBoundingShapes().getFirst()).getBoundsInParent();
        // The autosaved box covers 20 % to 60 % of the image.
        verifyThat(boxBounds.getMinX(),
                   Matchers.closeTo(imageBounds.getMinX() + 0.2 * imageBounds.getWidth(), 3.0),
                   saveScreenshot(testinfo));
        verifyThat(boxBounds.getMaxY(),
                   Matchers.closeTo(imageBounds.getMinY() + 0.6 * imageBounds.getHeight(), 3.0),
                   saveScreenshot(testinfo));
    }

    @Test
    void onStartingWithAnAutosaveOfTheLastFolder_WhenAccepting_ShouldOpenTheFolderAndRestoreIt(FxRobot robot,
                                                                                                 TestInfo testinfo)
            throws Exception {
        final File folder = testImageFolder();
        writeAutosave(folder);
        robot.interact(() -> controller.getRecentImageFolders().add(folder));

        // The dialog waits for an answer, so it is opened later on the JavaFX thread.
        Platform.runLater(controller::offerToRestoreLastSession);
        final Stage dialog = timeOutGetTopModalStage(robot, Controller.RESTORE_AUTOSAVE_DIALOG_TITLE, testinfo);
        timeOutLookUpInStageAndClickOn(robot, dialog, "Yes", testinfo);

        // The folder is opened and restored without asking again.
        waitUntilCurrentImageIsLoaded(testinfo);
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "the restored box", testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);
        verifyThat(model.getImageFileNameToAnnotationMap().get(IMAGE_FILE_NAME).getBoundingShapeData().size(),
                   Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
    }

    @Test
    void onFailingToSaveAutomatically_ShouldShowTheErrorInTheStatusBar(FxRobot robot, TestInfo testinfo)
            throws Exception {
        final File folder = testImageFolder();
        robot.interact(() -> controller.loadImageFiles(folder));
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Boat", testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.2), new Point2D(0.5, 0.5));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "the drawn box", testinfo);

        // A file in place of the autosave folder makes saving fail.
        final Path directory = controller.getAutosaveStore().directory();
        deleteRecursively(directory);
        Files.createFile(directory);

        try {
            robot.interact(controller::autosaveNow);
            controller.waitForAutosave();
            WaitForAsyncUtils.waitForFxEvents();

            verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                       Matchers.startsWith("Could not save the annotations automatically: "),
                       saveScreenshot(testinfo));
        } finally {
            Files.deleteIfExists(directory);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if(!Files.exists(directory)) {
            return;
        }

        try(var files = Files.list(directory)) {
            for(Path file : files.toList()) {
                Files.delete(file);
            }
        }

        Files.delete(directory);
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
