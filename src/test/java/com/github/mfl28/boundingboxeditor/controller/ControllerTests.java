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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.ui.BoundingBoxView;
import com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.service.query.NodeQuery;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class ControllerTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(Objects.requireNonNull(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1)).getFile()));
    }

    @Test
    void onExportAnnotation_PVOC_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                TestInfo testinfo,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";
        final String expectedFileName = "austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        final File referenceAnnotationFile = new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-import-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#pvoc-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir),
                () -> saveScreenshotAndReturnMessage(testinfo, "Actual files " +
                        "directory does not exist."));

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                Objects.equals(counts.get("Flag"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3), saveScreenshot(testinfo));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getImageFileListView()
                                .getSelectionModel()
                                .getSelectedItem()
                                .isHasAssignedBoundingShapes()
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingBoxView)
                                .count() == 8
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingPolygonView)
                                .count() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Bounding shape counts did not match " +
                                "within " + TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> controller.getIoMetaData()
                                .getDefaultAnnotationLoadingDirectory()
                                .equals(referenceAnnotationFile
                                        .getParentFile())),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected default annotation loading " +
                                "directory was not set within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getImageFileNameToAnnotationMap().values().stream()
                        .allMatch(imageAnnotation -> imageAnnotation.getImageMetaData().hasDetails()),
                Matchers.equalTo(true), saveScreenshot(testinfo));

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .scroll(-30)
                .release(KeyCode.SHORTCUT);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 1 image in"), saveScreenshot(testinfo));

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.initiateAnnotationExport(actualDir.toFile(),
                ImageAnnotationSaveStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        Path actualFilePath = actualDir.resolve(expectedFileName);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Files.exists(actualFilePath)),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Output-file was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));


        // The output file should be exactly the same as the reference file.
        final File referenceFile = new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Arrays.equals(referenceArray,
                                Files.readAllBytes(
                                        actualFilePath))),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected annotation output-file " +
                                "content was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));


        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> controller.getIoMetaData()
                                .getDefaultAnnotationSavingDirectory()
                                .equals(actualDir.toFile())),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected default annotation saving " +
                                "directory was no set within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    @Test
    void onExportAnnotation_YOLO_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                TestInfo testinfo,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationDirectoryPath = "/testannotations/yolo/reference";
        final String expectedAnnotationFileName = "austin-neill-685084-unsplash.txt";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File referenceAnnotationFolder =
                new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationDirectoryPath)).getFile());

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-import-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "#pvoc-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#yolo-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFolder, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir),
                () -> saveScreenshotAndReturnMessage(testinfo, "Actual files " +
                        "directory does not exist."));

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                Objects.equals(counts.get("Flag"), 1) &&
                                Objects.equals(counts.get("Test"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding shape " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(4),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(4), saveScreenshot(testinfo));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getImageFileListView()
                                .getSelectionModel()
                                .getSelectedItem()
                                .isHasAssignedBoundingShapes()
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingBoxView)
                                .count() == 9
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingPolygonView)
                                .count() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Bounding shape counts did not match " +
                                "within " + TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .scroll(-30)
                .release(KeyCode.SHORTCUT);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 1 image in"), saveScreenshot(testinfo));

        // Save the annotations to the temporary folder.
        Platform.runLater(
                () -> controller.initiateAnnotationExport(actualDir.toFile(), ImageAnnotationSaveStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);

        Path actualFilePath = actualDir.resolve(expectedAnnotationFileName);
        Path actualObjectDataFilePath = actualDir.resolve("object.data");

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Files.exists(actualFilePath) &&
                                Files.exists(actualObjectDataFilePath)),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Output-files were not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));

        final File objectDataFile = referenceAnnotationFolder.toPath().resolve("object.data").toFile();
        final byte[] objectDataFileArray = Files.readAllBytes(objectDataFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Arrays.equals(objectDataFileArray,
                                Files.readAllBytes(
                                        actualObjectDataFilePath))),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected annotation output-file " +
                                "content was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));


        // The output file should be exactly the same as the reference file.
        final File referenceFile = referenceAnnotationFolder.toPath().resolve(expectedAnnotationFileName).toFile();
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Arrays.equals(referenceArray,
                                Files.readAllBytes(
                                        actualFilePath))),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected annotation output-file " +
                                "content was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    @Test
    void onExportAnnotation_JSON_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                TestInfo testinfo,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath = "/testannotations/json/reference/annotations.json";
        final String expectedAnnotationFileName = "annotations.json";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File referenceAnnotationFile =
                new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-import-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "#pvoc-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#json-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), () -> saveScreenshotAndReturnMessage(testinfo,
                "Actual " +
                        "files " +
                        "directory does not exist."));

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                Objects.equals(counts.get("Flag"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3), saveScreenshot(testinfo));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getImageFileListView()
                                .getSelectionModel()
                                .getSelectedItem()
                                .isHasAssignedBoundingShapes()
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingBoxView)
                                .count() == 8
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingPolygonView)
                                .count() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Bounding shape counts did not match " +
                                "within " + TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .scroll(-30)
                .release(KeyCode.SHORTCUT);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 1 image in"), saveScreenshot(testinfo));

        // Save the annotations to the temporary folder.
        Platform.runLater(
                () -> controller.initiateAnnotationExport(actualDir.resolve(expectedAnnotationFileName).toFile(),
                        ImageAnnotationSaveStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);

        Path actualFilePath = actualDir.resolve(expectedAnnotationFileName);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Files.exists(actualFilePath)),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Output-files were not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));

        // The output file should be exactly the same as the reference file.
        final byte[] referenceArray = Files.readAllBytes(referenceAnnotationFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Arrays.equals(referenceArray,
                                Files.readAllBytes(
                                        actualFilePath))),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected annotation output-file " +
                                "content was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    @Test
    void onLoadAnnotation_YOLO_WhenObjectDataFileMissing_ShouldNotLoadAnnotations(FxRobot robot, TestInfo testinfo) {
        final String inputPath = "/testannotations/yolo/missing-classes-file";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(Objects.requireNonNull(getClass().getResource(inputPath)).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1), saveScreenshot(testinfo));

        final IOErrorInfoEntry referenceErrorInfoEntry1 = new IOErrorInfoEntry("object.data",
                "Does not exist in annotation folder \"missing-classes-file\".");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1), saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        // Check if closed
        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(0), saveScreenshot(testinfo));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_YOLO_WhenAnnotationFileContainsErrors_ShouldNotLoadInvalidBoundingBoxes(FxRobot robot,
                                                                                                  TestInfo testinfo) {
        final String inputPath = "/testannotations/yolo/invalid-annotations";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(Objects.requireNonNull(getClass().getResource(inputPath)).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText =
                "Some bounding boxes could not be loaded from 4 image-annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(5), saveScreenshot(testinfo));

        final IOErrorInfoEntry referenceErrorInfoEntry1 =
                new IOErrorInfoEntry("austin-neill-685084-unsplash.txt",
                        "Invalid category index 4 (of 4 categories) on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry2 =
                new IOErrorInfoEntry("caleb-george-316073-unsplash.txt",
                        "Missing or invalid category index on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry3 =
                new IOErrorInfoEntry("nico-bhlr-1067059-unsplash.txt",
                        "Invalid number of bounds values on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry4 =
                new IOErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                        "Bounds value not within interval [0, 1] on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry5 =
                new IOErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                        "Invalid bounding-box coordinates on line 2.");

        verifyThat(errorInfoEntries, Matchers.containsInAnyOrder(referenceErrorInfoEntry1, referenceErrorInfoEntry2,
                referenceErrorInfoEntry3, referenceErrorInfoEntry4,
                referenceErrorInfoEntry5), saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(counts.get("Ship"), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(model.createImageAnnotationData().imageAnnotations(), Matchers.hasSize(1),
                saveScreenshot(testinfo));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 1 image in"), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingNonCriticalElements_ShouldNotLoadIncompleteBoundingBoxes(
            FxRobot robot, TestInfo testinfo) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_elements.xml";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        final File referenceAnnotationFile = new File(Objects.requireNonNull(getClass().getResource(inputFilePath)).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText =
                "Some bounding boxes could not be loaded from 1 image-annotation.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(2), saveScreenshot(testinfo));

        final IOErrorInfoEntry referenceErrorInfoEntry1 =
                new IOErrorInfoEntry("annotation_with_missing_elements.xml",
                        "Missing element: name");
        final IOErrorInfoEntry referenceErrorInfoEntry2 =
                new IOErrorInfoEntry("annotation_with_missing_elements.xml",
                        "Missing element: ymin");
        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1, referenceErrorInfoEntry2),
                saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 1) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                counts.get("Flag") == null),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 1 image in"), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingCriticalElement_ShouldNotLoadAnyBoundingBoxes(FxRobot robot,
                                                                                               TestInfo testinfo) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_filename.xml";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        final File referenceAnnotationFile = new File(Objects.requireNonNull(getClass().getResource(inputFilePath)).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> errorInfoTable.getItems().size() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of error info entries" +
                                " not found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        final IOErrorInfoEntry referenceErrorInfoEntry =
                new IOErrorInfoEntry("annotation_with_missing_filename.xml",
                        "Missing element: filename");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry), saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().isEmpty(), Matchers.is(true),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(model.createImageAnnotationData().imageAnnotations(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty(), saveScreenshot(testinfo));

        // Should not have changed the status message.
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenAnnotationsPresent_ShouldAskForAndCorrectlyApplyUserChoice(FxRobot robot,
                                                                                              TestInfo testinfo) {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true), saveScreenshot(testinfo));
        timeOutClickOn(robot, "#next-button", testinfo);

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true), saveScreenshot(testinfo));

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 0),
                saveScreenshot(testinfo));

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getCurrentBoundingShapes()
                                .size() == 1),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of bounding boxes not" +
                                " found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 1),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().getFirst(), Matchers.instanceOf(BoundingBoxView.class),
                saveScreenshot(testinfo));

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true), saveScreenshot(testinfo));
        final BoundingBoxView drawnBoundingBox = (BoundingBoxView) mainView.getCurrentBoundingShapes().getFirst();

        final File annotationFile = new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());

        // (1) User chooses Cancel:
        userChoosesCancelOnAnnotationImportDialogSubtest(robot, drawnBoundingBox, annotationFile, testinfo);

        // (2) User chooses Yes (Keep existing annotations and categories)
        userChoosesYesOnAnnotationImportDialogSubTest(robot, drawnBoundingBox, annotationFile, testinfo);

        // (3) User chooses No (Do not keep existing bounding boxes):
        userChoosesNoOnAnnotationImportDialogSubtest(robot, annotationFile, testinfo);
    }

    @Test
    void onLoadAnnotation_JSON_WhenFileHasMissingCriticalElements_ShouldNotLoadInvalidBoundingBoxes(FxRobot robot,
                                                                                                    TestInfo testinfo) {
        final String missingFileNameAnnotationFilePath = "/testannotations/json/missing_critical_elements.json";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(Objects.requireNonNull(getClass().getResource(missingFileNameAnnotationFilePath)).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText = "Some bounding boxes could not be loaded from 1 " +
                "image-annotation.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(17), saveScreenshot(testinfo));

        final IOErrorInfoEntry error1 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid coordinate value for minX element" +
                        " in bndbox element in annotation " +
                        "for image " +
                        "tyler-nix-582593-unsplash.jpg.");
        final IOErrorInfoEntry error2 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid coordinate value for minY element" +
                        " in bndbox element in annotation " +
                        "for image " +
                        "tyler-nix-582593-unsplash.jpg.");
        final IOErrorInfoEntry error3 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing category name element in " +
                        "annotation for image " +
                        "tyler-nix-582593-unsplash.jpg.");
        final IOErrorInfoEntry error4 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid coordinate value for maxX element" +
                        " in bndbox element in annotation " +
                        "for image " +
                        "nico-bhlr-1067059-unsplash.jpg.");
        final IOErrorInfoEntry error5 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing maxY element in bndbox element in" +
                        " annotation for image " +
                        "nico-bhlr-1067059-unsplash.jpg.");
        final IOErrorInfoEntry error6 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid color element " +
                        "in annotation for image " +
                        "nico-bhlr-1067059-unsplash.jpg.");
        final IOErrorInfoEntry error7 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing category element in bndbox " +
                        "element in annotation for image nico-bhlr-1067059-unsplash.jpg.");
        final IOErrorInfoEntry error8 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid tags value(s) in bndbox element " +
                        "in annotation for image nico-bhlr-1067059-unsplash.jpg.");
        final IOErrorInfoEntry error9 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing bndbox or polygon element in " +
                        "annotation for image austin-neill-685084-unsplash.jpg.");
        final IOErrorInfoEntry error10 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing maxY element in bndbox element in" +
                        " annotation for image austin-neill-685084-unsplash.jpg.");
        final IOErrorInfoEntry error11 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid parts value(s) in bndbox element " +
                        "in annotation for image austin-neill-685084-unsplash.jpg.");
        final IOErrorInfoEntry error12 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing minY element" +
                        " in bndbox element in annotation " +
                        "for image " +
                        "austin-neill-685084-unsplash.jpg.");
        final IOErrorInfoEntry error13 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing maxY element" +
                        " in bndbox element in annotation " +
                        "for image " +
                        "austin-neill-685084-unsplash.jpg.");
        final IOErrorInfoEntry error14 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid number of coordinates in polygon " +
                        "element in annotation for image caleb-george-316073-unsplash.jpg.");
        final IOErrorInfoEntry error15 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Invalid coordinate value(s) in polygon " +
                        "element in annotation for image caleb-george-316073-unsplash.jpg.");
        final IOErrorInfoEntry error16 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Missing image fileName element.");
        final IOErrorInfoEntry error17 = new IOErrorInfoEntry("missing_critical_elements.json",
                "Image nothere.jpg does not belong to " +
                        "currently loaded image files.");

        verifyThat(errorInfoEntries,
                Matchers.containsInAnyOrder(error1, error2, error3, error4, error5, error6, error7,
                        error8, error9, error10, error11, error12, error13, error14,
                        error15, error16, error17), saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(4), saveScreenshot(testinfo));
        verifyThat(counts, Matchers.hasEntry("Car", 1), saveScreenshot(testinfo));
        verifyThat(counts, Matchers.hasEntry("Sail", 2), saveScreenshot(testinfo));
        verifyThat(counts, Matchers.hasEntry("Surfboard", 1), saveScreenshot(testinfo));
        verifyThat(counts, Matchers.hasEntry("Boat", 2), saveScreenshot(testinfo));

        final List<ObjectCategory> objectCategories = model.getObjectCategories();
        verifyThat(objectCategories, Matchers.hasSize(4), saveScreenshot(testinfo));
        verifyThat(objectCategories.stream().map(ObjectCategory::getName).toList(),
                Matchers.containsInAnyOrder("Car", "Sail", "Surfboard", "Boat"), saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(4), saveScreenshot(testinfo));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 3 images in "), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_JSON_WhenAnnotationFileIsEmpty_ShouldDisplayErrorDialog(FxRobot robot, TestInfo testinfo) {
        final String emptyAnnotationFilePath = "/testannotations/json/empty.json";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(Objects.requireNonNull(getClass().getResource(emptyAnnotationFilePath)).getFile());

        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage alert = timeOutGetTopModalStage(robot, "Annotation Import Error", testinfo);
        verifyThat(alert, Matchers.notNullValue(), saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, alert, "OK", testinfo);
        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error", testinfo);
    }

    @Test
    void onLoadAnnotation_JSON_WhenAnnotationFileIsCorrupt_ShouldDisplayErrorReport(FxRobot robot, TestInfo testinfo) {
        final String corruptAnnotationFilePath = "/testannotations/json/corrupt.json";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(Objects.requireNonNull(getClass().getResource(corruptAnnotationFilePath)).getFile());

        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().getFirst(),
                Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().getFirst(), Matchers.instanceOf(TableView.class),
                saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().getFirst();

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1), saveScreenshot(testinfo));

        verifyThat(errorInfoEntries.getFirst().getSourceName(), Matchers.equalTo("corrupt.json"), saveScreenshot(testinfo));
        verifyThat(errorInfoEntries.getFirst().getErrorDescription(), Matchers.startsWith("Unterminated array at line 2 " +
                        "column 13"),
                saveScreenshot(testinfo));

        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
    }

    @Test
    void onReloadAnnotations_afterImageFilesReopened_shouldCorrectlyDisplayBoundingShapes(FxRobot robot,
                                                                                          TestInfo testinfo) {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(controller.getStage().getTitle(), Matchers.matchesRegex("^Bounding Box Editor \\d\\.\\d\\.\\d - .*$"),
                saveScreenshot(testinfo));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        final File referenceAnnotationFile = new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                Objects.equals(counts.get("Flag"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        loadImageFolder(TEST_IMAGE_FOLDER_PATH_1);

        Stage keepExistingCategoriesDialogStage = timeOutAssertDialogOpenedAndGetStage(robot,
                "Open Image Folder",
                "Keep existing categories?",
                testinfo);

        timeOutLookUpInStageAndClickOn(robot, keepExistingCategoriesDialogStage, "No", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertNoTopModelStage(robot, testinfo);

        verifyThat(model.getCurrentFileIndex(), Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToAnnotationMap().size(), Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(controller.lastLoadedImageUrl, Matchers.nullValue(), saveScreenshot(testinfo));
        verifyThat(controller.getIoMetaData().getDefaultImageLoadingDirectory(),
                Matchers.equalTo(new File(Objects.requireNonNull(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1)).getFile())),
                saveScreenshot(testinfo));

        // Reload bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Map<String, Integer> countsReloaded = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects
                                .equals(countsReloaded.get("Boat"), 2) &&
                                Objects.equals(countsReloaded.get("Sail"),
                                        6)
                                &&
                                Objects.equals(countsReloaded.get("Flag"),
                                        1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        timeOutClickOn(robot, "#next-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0), saveScreenshot(testinfo));
    }

    @Test
    void onLoadAnnotation_YOLO_WhenAnnotationWithinYOLOPrecision_ShouldLoadBoundingBoxes(TestInfo testinfo) {

        final String referenceAnnotationDirectoryPath = "/testannotations/yolo/precision";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File referenceAnnotationFolder =
                new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationDirectoryPath)).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFolder, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Test"), 2)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(1),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(1), saveScreenshot(testinfo));
    }

    @Test
    void onExportAnnotation_CSV_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                TestInfo testinfo,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath = "/testannotations/csv/reference/annotations.csv";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 4 image-files from folder "), saveScreenshot(testinfo));

        final File referenceAnnotationFile =
                new File(Objects.requireNonNull(getClass().getResource(referenceAnnotationFilePath)).getFile());

        timeOutClickOn(robot, "#file-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#file-import-annotations-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "#pvoc-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#csv-import-menu-item", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.CSV));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        // Create temporary folder to save annotations to.
        Path actualFile = tempDirectory.resolve("actual.csv");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 2) &&
                                Objects.equals(counts.get("Surfboard"), 3)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding shape " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3), saveScreenshot(testinfo));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getImageFileListView()
                                .getSelectionModel()
                                .getSelectedItem()
                                .isHasAssignedBoundingShapes()
                                && mainView.getCurrentBoundingShapes()
                                .stream()
                                .filter(viewable -> viewable instanceof BoundingBoxView)
                                .count() == 3
                                && mainView.getCurrentBoundingShapes()
                                .stream().noneMatch(viewable -> viewable instanceof BoundingPolygonView)),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Bounding shape counts did not match " +
                                "within " + TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.SHORTCUT)
                .scroll(-30)
                .release(KeyCode.SHORTCUT);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully imported annotations for 3 images in"), saveScreenshot(testinfo));

        // Save the annotations to the temporary folder.
        Platform.runLater(
                () -> controller.initiateAnnotationExport(actualFile.toFile(), ImageAnnotationSaveStrategy.Type.CSV));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testinfo);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Files.exists(actualFile)),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Output-file was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));

        final byte[] referenceFileBytes = Files.readAllBytes(referenceAnnotationFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Arrays.equals(referenceFileBytes,
                                Files.readAllBytes(actualFile))),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected annotation output-file " +
                                "content was not created within " +
                                TIMEOUT_DURATION_IN_SEC + " sec."));
    }

    private void userChoosesNoOnAnnotationImportDialogSubtest(FxRobot robot, File annotationFile, TestInfo testinfo) {
        userChoosesToSaveExistingAnnotationsOnAnnotationImport(robot, annotationFile, testinfo);
        userChoosesNotToSaveExistingAnnotationsOnAnnotationImport(robot, annotationFile, testinfo);
    }

    private void userChoosesToSaveExistingAnnotationsOnAnnotationImport(FxRobot robot, File annotationFile,
                                                                        TestInfo testinfo) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "No", testinfo);

        Stage saveAnnotationsDialog = timeOutGetTopModalStage(robot, "Save Annotations", testinfo);

        // User chooses to save existing annotations:
        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialog, "Yes", testinfo);

        Stage saveFormatDialog = timeOutGetTopModalStage(robot, "Save Annotations", testinfo);
        timeOutLookUpInStageAndClickOn(robot, saveFormatDialog, "Cancel", testinfo);
        timeOutAssertTopModalStageClosed(robot, "Save Annotations", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));
        // Trying to import annotations updates annotation data and "saved" check
        // (there are unsaved annotations):
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false), saveScreenshot(testinfo));
    }

    private void userChoosesNotToSaveExistingAnnotationsOnAnnotationImport(FxRobot robot, File annotationFile,
                                                                           TestInfo testinfo) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "No", testinfo);

        Stage saveAnnotationsDialog = timeOutGetTopModalStage(robot, "Save Annotations", testinfo);

        // User chooses not to save existing annotations:
        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialog, "No", testinfo);
        timeOutAssertTopModalStageClosed(robot, "Save Annotations", testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);

        // All previously existing bounding boxes should have been removed, only
        // the newly imported ones should exist.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6) &&
                                Objects.equals(counts.get("Flag"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3), saveScreenshot(testinfo));
        verifyThat(model.createImageAnnotationData().imageAnnotations(), Matchers.hasSize(1),
                saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(0),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getItems().getFirst().isHasAssignedBoundingShapes(),
                Matchers.is(true), saveScreenshot(testinfo));

        // Loading new annotations from existing annotation files
        // should lead to a positive "saved" status:
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true), saveScreenshot(testinfo));
    }

    private void userChoosesYesOnAnnotationImportDialogSubTest(FxRobot robot, BoundingBoxView drawnBoundingBox,
                                                               File annotationFile, TestInfo testinfo) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Yes", testinfo);

        // Everything should have stayed the same for the current image...
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox), saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));

        // ... but there should be additional categories and bounding boxes in the model.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> Objects.equals(counts.get("Boat"), 2) &&
                                Objects.equals(counts.get("Sail"), 6)
                                &&
                                Objects.equals(counts.get("Flag"), 1) &&
                                Objects.equals(counts.get("Test"), 1)),
                () -> saveScreenshotAndReturnMessage(testinfo, "Correct bounding box " +
                        "per-category-counts were not read within " +
                        TIMEOUT_DURATION_IN_SEC + " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(4),
                saveScreenshot(testinfo));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(4), saveScreenshot(testinfo));
        verifyThat(model.createImageAnnotationData().imageAnnotations(), Matchers.hasSize(2),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getItems().getFirst().isHasAssignedBoundingShapes(),
                Matchers.is(true), saveScreenshot(testinfo));

        // Remove the imported Annotations manually to reset for next test.
        timeOutClickOn(robot, "#previous-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn(mainView.getObjectTree().getRoot().getChildren().getFirst().getGraphic())
                .clickOn("Delete");

        WaitForAsyncUtils.waitForFxEvents();

        for (int i = 0; i != 3; ++i) {
            NodeQuery nodeQuery = robot.from(mainView.getObjectCategoryTable()).lookup("#delete-button").nth(1);
            robot.clickOn((Node) nodeQuery.query(), MouseButton.PRIMARY);
        }

        verifyThat(mainView.getObjectCategoryTable(), TableViewMatchers.hasNumRows(1), saveScreenshot(testinfo));
        verifyThat(mainView.getObjectCategoryTable().getItems().getFirst().getName(), Matchers.equalTo("Test"),
                saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        // Combining the old and new annotations leads to new annotations, meaning
        // there should be unsaved annotations:
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false), saveScreenshot(testinfo));
    }

    private void userChoosesCancelOnAnnotationImportDialogSubtest(FxRobot robot,
                                                                  BoundingBoxView drawnBoundingBox,
                                                                  File annotationFile, TestInfo testinfo) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Cancel", testinfo);

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(1),
                saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 1),
                saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox), saveScreenshot(testinfo));
        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));
        // Trying to import annotations updates annotation data and "saved" check
        // (there are unsaved annotations):
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false), saveScreenshot(testinfo));
    }

    private void importAnnotationAndClickDialogOption(FxRobot robot, File annotationFile, String userChoice,
                                                      TestInfo testinfo) {
        Platform.runLater(
                () -> controller.initiateAnnotationImport(annotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Stage topModalStage = timeOutGetTopModalStage(robot, "Import Annotation Data", testinfo);
        timeOutLookUpInStageAndClickOn(robot, topModalStage, userChoice, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Import Annotation Data", testinfo);

        WaitForAsyncUtils.waitForFxEvents();
    }
}
