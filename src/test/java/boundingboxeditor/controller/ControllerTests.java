package boundingboxeditor.controller;

import boundingboxeditor.BoundingBoxEditorTestBase;
import boundingboxeditor.model.data.ObjectCategory;
import boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import boundingboxeditor.model.io.results.IOErrorInfoEntry;
import boundingboxeditor.ui.BoundingBoxView;
import boundingboxeditor.ui.BoundingPolygonView;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
import org.junit.jupiter.api.Test;
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
import java.util.stream.Collectors;

import static org.testfx.api.FxAssert.verifyThat;

class ControllerTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onExportAnnotation_PVOC_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";
        final String expectedFileName = "austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));
        verifyThat(model.isSaved(), Matchers.is(true));

        final File referenceAnnotationFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Import Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.isSaved(), Matchers.is(true));

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory does not exist.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              Objects.equals(counts.get("Flag"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));

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
                                      "Bounding shape counts did not match within " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> controller.getIoMetaData()
                                                                                      .getDefaultAnnotationLoadingDirectory()
                                                                                      .equals(referenceAnnotationFile
                                                                                                      .getParentFile())),
                                      "Expected default annotation loading directory was not set within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getImageFileNameToAnnotationMap().values().stream()
                        .allMatch(imageAnnotation -> imageAnnotation.getImageMetaData().hasDetails()),
                   Matchers.equalTo(true));

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
             .press(KeyCode.CONTROL)
             .scroll(-30)
             .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 1 image in"));

        verifyThat(model.isSaved(), Matchers.is(true));
        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.initiateAnnotationExport(actualDir.toFile(),
                                                                    ImageAnnotationSaveStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> WaitForAsyncUtils.asyncFx(
                                                                              () -> controller
                                                                                      .getAnnotationExportService()
                                                                                      .getState()
                                                                                      .equals(
                                                                                              Worker.State.SUCCEEDED))
                                                                                             .get()),
                                      "Annotation " +
                                              "saving " +
                                              "service did not succeed within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.isSaved(), Matchers.is(true));
        Path actualFilePath = actualDir.resolve(expectedFileName);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Files.exists(actualFilePath)),
                                      "Output-file was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");


        // The output file should be exactly the same as the reference file.
        final File referenceFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Arrays.equals(referenceArray,
                                                                                          Files.readAllBytes(
                                                                                                  actualFilePath))),
                                      "Expected annotation output-file content was not created within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");


        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> controller.getIoMetaData()
                                                                                      .getDefaultAnnotationSavingDirectory()
                                                                                      .equals(actualDir.toFile())),
                                      "Expected default annotation saving directory was no set within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onExportAnnotation_YOLO_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationDirectoryPath = "/testannotations/yolo/reference";
        final String expectedAnnotationFileName = "austin-neill-685084-unsplash.txt";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationFolder =
                new File(getClass().getResource(referenceAnnotationDirectoryPath).getFile());

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Import Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "YOLO format...");
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFolder, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory does not exist.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              Objects.equals(counts.get("Flag"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));

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
                                                                                         .stream().noneMatch(
                                                                                      viewable -> viewable instanceof BoundingPolygonView)),
                                      "Bounding shape counts did not match within " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
             .press(KeyCode.CONTROL)
             .scroll(-30)
             .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 1 image in"));

        // Save the annotations to the temporary folder.
        Platform.runLater(
                () -> controller.initiateAnnotationExport(actualDir.toFile(), ImageAnnotationSaveStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(expectedAnnotationFileName);
        Path actualObjectDataFilePath = actualDir.resolve("object.data");

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Files.exists(actualFilePath) &&
                                                                              Files.exists(actualObjectDataFilePath)),
                                      "Output-files were not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final File objectDataFile = referenceAnnotationFolder.toPath().resolve("object.data").toFile();
        final byte[] objectDataFileArray = Files.readAllBytes(objectDataFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Arrays.equals(objectDataFileArray,
                                                                                          Files.readAllBytes(
                                                                                                  actualObjectDataFilePath))),
                                      "Expected annotation output-file content was not created within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");


        // The output file should be exactly the same as the reference file.
        final File referenceFile = referenceAnnotationFolder.toPath().resolve(expectedAnnotationFileName).toFile();
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Arrays.equals(referenceArray,
                                                                                          Files.readAllBytes(
                                                                                                  actualFilePath))),
                                      "Expected annotation output-file content was not created within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onExportAnnotation_JSON_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot,
                                                                                                @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath = "/testannotations/json/reference/annotations.json";
        final String expectedAnnotationFileName = "annotations.json";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationFile =
                new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Import Annotations");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutMoveTo(robot, "Pascal-VOC format...");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "JSON format...");
        WaitForAsyncUtils.waitForFxEvents();
        robot.push(KeyCode.ESCAPE);

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory does not exist.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              Objects.equals(counts.get("Flag"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));

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
                                      "Bounding shape counts did not match within " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
             .press(KeyCode.CONTROL)
             .scroll(-30)
             .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 1 image in"));

        // Save the annotations to the temporary folder.
        Platform.runLater(
                () -> controller.initiateAnnotationExport(actualDir.resolve(expectedAnnotationFileName).toFile(),
                                                          ImageAnnotationSaveStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(expectedAnnotationFileName);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Files.exists(actualFilePath)),
                                      "Output-files were not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // The output file should be exactly the same as the reference file.
        final byte[] referenceArray = Files.readAllBytes(referenceAnnotationFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Arrays.equals(referenceArray,
                                                                                          Files.readAllBytes(
                                                                                                  actualFilePath))),
                                      "Expected annotation output-file content was not created within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onLoadAnnotation_YOLO_WhenObjectDataFileMissing_ShouldNotLoadAnnotations(FxRobot robot) {
        final String inputPath = "/testannotations/yolo/missing-classes-file";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(inputPath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1));

        final IOErrorInfoEntry referenceErrorInfoEntry1 = new IOErrorInfoEntry("object.data",
                                                                               "Does not exist in annotation folder \"missing-classes-file\".");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        // Check if closed
        timeOutAssertTopModalStageClosed(robot, "Annotation import error report");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(0));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));
    }

    @Test
    void onLoadAnnotation_YOLO_WhenAnnotationFileContainsErrors_ShouldNotLoadInvalidBoundingBoxes(FxRobot robot) {
        final String inputPath = "/testannotations/yolo/invalid-annotations";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(inputPath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText =
                "Some bounding boxes could not be loaded from 4 image-annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(5));

        final IOErrorInfoEntry referenceErrorInfoEntry1 =
                new IOErrorInfoEntry("austin-neill-685084-unsplash.txt",
                                     "Invalid category index 4 (of 4 categories) on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry2 =
                new IOErrorInfoEntry("caleb-george-316073-unsplash.txt",
                                     "Missing or invalid category index on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry3 =
                new IOErrorInfoEntry("nico-bhlr-1067059-unsplash.txt",
                                     "Missing or invalid bounding-box bounds on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry4 =
                new IOErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                                     "Bounds ratio not within [0, 1] on line 1.");

        final IOErrorInfoEntry referenceErrorInfoEntry5 =
                new IOErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                                     "Invalid bounding-box coordinates on line 2.");

        verifyThat(errorInfoEntries, Matchers.containsInAnyOrder(referenceErrorInfoEntry1, referenceErrorInfoEntry2,
                                                                 referenceErrorInfoEntry3, referenceErrorInfoEntry4,
                                                                 referenceErrorInfoEntry5));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation import error report");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(1));
        verifyThat(counts.get("Ship"), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(model.createImageAnnotationData().getImageAnnotations(), Matchers.hasSize(1));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 1 image in"));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingNonCriticalElements_ShouldNotLoadIncompleteBoundingBoxes(
            FxRobot robot) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_elements.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationFile = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText =
                "Some bounding boxes could not be loaded from 1 image-annotation.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(2));

        final IOErrorInfoEntry referenceErrorInfoEntry1 =
                new IOErrorInfoEntry("annotation_with_missing_elements.xml",
                                     "Missing element: name");
        final IOErrorInfoEntry referenceErrorInfoEntry2 =
                new IOErrorInfoEntry("annotation_with_missing_elements.xml",
                                     "Missing element: ymin");
        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1, referenceErrorInfoEntry2));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertTopModalStageClosed(robot, "Annotation import error report");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 1) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              counts.get("Flag") == null),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 1 image in"));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingCriticalElement_ShouldNotLoadAnyBoundingBoxes(FxRobot robot) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_filename.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationFile = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> errorInfoTable.getItems().size() == 1),
                                      "Expected number of error info entries not found in " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");

        final IOErrorInfoEntry referenceErrorInfoEntry =
                new IOErrorInfoEntry("annotation_with_missing_filename.xml",
                                     "Missing element: filename");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation import error report");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().isEmpty(), Matchers.is(true));
        verifyThat(model.getObjectCategories(), Matchers.empty());
        verifyThat(model.createImageAnnotationData().getImageAnnotations(), Matchers.empty());
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty());

        // Should not have changed the status message.
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenAnnotationsPresent_ShouldAskForAndCorrectlyApplyUserChoice(FxRobot robot) {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.isSaved(), Matchers.is(true));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true));
        timeOutClickOn(robot, "#next-button");

        verifyThat(model.isSaved(), Matchers.is(true));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true));

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 0));

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> mainView.getCurrentBoundingShapes()
                                                                                    .size() == 1),
                                      "Expected number of bounding boxes not found in " + TIMEOUT_DURATION_IN_SEC +
                                              " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class));

        verifyThat(model.isSaved(), Matchers.is(true));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true));
        final BoundingBoxView drawnBoundingBox = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);

        final File annotationFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        // (1) User chooses Cancel:
        userChoosesCancelOnAnnotationImportDialogSubtest(robot, drawnBoundingBox, annotationFile);

        // (2) User chooses Yes (Keep existing annotations and categories)
        userChoosesYesOnAnnotationImportDialogSubTest(robot, drawnBoundingBox, annotationFile);

        // (3) User chooses No (Do not keep existing bounding boxes):
        userChoosesNoOnAnnotationImportDialogSubtest(robot, annotationFile);
    }

    @Test
    void onLoadAnnotation_JSON_WhenFileHasMissingCriticalElements_ShouldNotLoadInvalidBoundingBoxes(FxRobot robot) {
        final String missingFileNameAnnotationFilePath = "/testannotations/json/missing_critical_elements.json";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(missingFileNameAnnotationFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "Some bounding boxes could not be loaded from 1 " +
                "image-annotation.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(17));

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
                                               error15, error16, error17));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation import error report");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(4));
        verifyThat(counts, Matchers.hasEntry("Car", 1));
        verifyThat(counts, Matchers.hasEntry("Sail", 2));
        verifyThat(counts, Matchers.hasEntry("Surfboard", 1));
        verifyThat(counts, Matchers.hasEntry("Boat", 2));

        final List<ObjectCategory> objectCategories = model.getObjectCategories();
        verifyThat(objectCategories, Matchers.hasSize(4));
        verifyThat(objectCategories.stream().map(ObjectCategory::getName).collect(Collectors.toList()),
                   Matchers.containsInAnyOrder("Car", "Sail", "Surfboard", "Boat"));

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(4));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully imported annotations for 3 images in "));
    }

    @Test
    void onLoadAnnotation_JSON_WhenAnnotationFileIsEmpty_ShouldDisplayErrorDialog(FxRobot robot) {
        final String emptyAnnotationFilePath = "/testannotations/json/empty.json";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(emptyAnnotationFilePath).getFile());

        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage alert = timeOutGetTopModalStage(robot, "Annotation Import Error");
        verifyThat(alert, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, alert, "OK");
        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error");
    }

    @Test
    void onLoadAnnotation_JSON_WhenAnnotationFileIsCorrupt_ShouldDisplayErrorReport(FxRobot robot) {
        final String corruptAnnotationFilePath = "/testannotations/json/corrupt.json";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(corruptAnnotationFilePath).getFile());

        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "The source does not contain any valid annotations.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1));

        verifyThat(errorInfoEntries.get(0).getFileName(), Matchers.equalTo("corrupt.json"));
        verifyThat(errorInfoEntries.get(0).getErrorDescription(), Matchers.startsWith("Unterminated array at line 2 " +
                                                                                              "column 13"));

        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
    }

    @Test
    void onReloadAnnotations_afterImageFilesReopened_shouldCorrectlyDisplayBoundingShapes(FxRobot robot) {
        final String referenceAnnotationFilePath =
                "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(controller.getStage().getTitle(), Matchers.startsWith("Bounding Box Editor - "));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                   Matchers.startsWith("Successfully loaded 4 image-files from folder "));
        verifyThat(model.isSaved(), Matchers.is(true));

        final File referenceAnnotationFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              Objects.equals(counts.get("Flag"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.isSaved(), Matchers.is(true));

        timeOutClickOn(robot, "#next-button");

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0));
        verifyThat(model.isSaved(), Matchers.is(true));

        loadImageFolder(TEST_IMAGE_FOLDER_PATH_1);

        Stage keepExistingCategoriesDialogStage = timeOutAssertDialogOpenedAndGetStage(robot,
                                                                                       "Open image folder",
                                                                                       "Keep existing categories?");

        timeOutLookUpInStageAndClickOn(robot, keepExistingCategoriesDialogStage, "No");
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertNoTopModelStage(robot);

        verifyThat(model.getCurrentFileIndex(), Matchers.equalTo(0));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(model.getImageFileNameToAnnotationMap().size(), Matchers.equalTo(0));
        verifyThat(controller.lastLoadedImageUrl, Matchers.nullValue());
        verifyThat(controller.getIoMetaData().getDefaultImageLoadingDirectory(),
                   Matchers.equalTo(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile())));

        // Reload bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller
                .initiateAnnotationImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> countsReloaded = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects
                                                                              .equals(countsReloaded.get("Boat"), 2) &&
                                                                              Objects.equals(countsReloaded.get("Sail"),
                                                                                             6)
                                                                              &&
                                                                              Objects.equals(countsReloaded.get("Flag"),
                                                                                             1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0));
    }

    private void userChoosesNoOnAnnotationImportDialogSubtest(FxRobot robot, File annotationFile) {
        userChoosesToSaveExistingAnnotationsOnAnnotationImport(robot, annotationFile);
        userChoosesNotToSaveExistingAnnotationsOnAnnotationImport(robot, annotationFile);
    }

    private void userChoosesToSaveExistingAnnotationsOnAnnotationImport(FxRobot robot, File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "No");

        Stage saveAnnotationsDialog = timeOutGetTopModalStage(robot, "Save annotations");

        // User chooses to save existing annotations:
        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialog, "Yes");

        Stage saveFormatDialog = timeOutGetTopModalStage(robot, "Save annotations");
        timeOutLookUpInStageAndClickOn(robot, saveFormatDialog, "Cancel");
        timeOutAssertTopModalStageClosed(robot, "Save annotations");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));
        // Trying to import annotations updates annotation data and "saved" check
        // (there are unsaved annotations):
        verifyThat(model.isSaved(), Matchers.is(false));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false));
    }

    private void userChoosesNotToSaveExistingAnnotationsOnAnnotationImport(FxRobot robot, File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "No");

        Stage saveAnnotationsDialog = timeOutGetTopModalStage(robot, "Save annotations");

        // User chooses not to save existing annotations:
        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsDialog, "No");
        timeOutAssertTopModalStageClosed(robot, "Save annotations");
        timeOutAssertNoTopModelStage(robot);

        // All previously existing bounding boxes should have been removed, only
        // the newly imported ones should exist.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6) &&
                                                                              Objects.equals(counts.get("Flag"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));
        verifyThat(model.createImageAnnotationData().getImageAnnotations(), Matchers.hasSize(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty());
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(0));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(0).isHasAssignedBoundingShapes(),
                   Matchers.is(true));

        // Loading new annotations from existing annotation files
        // should lead to a positive "saved" status:
        verifyThat(model.isSaved(), Matchers.is(true));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(true));
    }

    private void userChoosesYesOnAnnotationImportDialogSubTest(FxRobot robot, BoundingBoxView drawnBoundingBox,
                                                               File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Yes");

        // Everything should have stayed the same for the current image...
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        // ... but there should be additional categories and bounding boxes in the model.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> Objects.equals(counts.get("Boat"), 2) &&
                                                                              Objects.equals(counts.get("Sail"), 6)
                                                                              &&
                                                                              Objects.equals(counts.get("Flag"), 1) &&
                                                                              Objects.equals(counts.get("Test"), 1)),
                                      "Correct bounding box per-category-counts were not read within " +
                                              TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(4));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(4));
        verifyThat(model.createImageAnnotationData().getImageAnnotations(), Matchers.hasSize(2));

        verifyThat(mainView.getImageFileListView().getItems().get(0).isHasAssignedBoundingShapes(),
                   Matchers.is(true));

        // Remove the imported Annotations manually to reset for next test.
        timeOutClickOn(robot, "#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn(mainView.getObjectTree().getRoot().getChildren().get(0).getGraphic())
             .clickOn("Delete");

        WaitForAsyncUtils.waitForFxEvents();

        for(int i = 0; i != 3; ++i) {
            NodeQuery nodeQuery = robot.from(mainView.getObjectCategoryTable()).lookup("#delete-button").nth(1);
            robot.clickOn((Node) nodeQuery.query(), MouseButton.PRIMARY);
        }

        verifyThat(mainView.getObjectCategoryTable(), TableViewMatchers.hasNumRows(1));
        verifyThat(mainView.getObjectCategoryTable().getItems().get(0).getName(), Matchers.equalTo("Test"));

        timeOutClickOn(robot, "#next-button");

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();
        // Combining the old and new annotations leads to new annotations, meaning
        // there should be unsaved annotations:
        verifyThat(model.isSaved(), Matchers.is(false));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false));
    }

    private void userChoosesCancelOnAnnotationImportDialogSubtest(FxRobot robot,
                                                                  BoundingBoxView drawnBoundingBox,
                                                                  File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Cancel");

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(1));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap(), Matchers.hasEntry("Test", 1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox));
        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));
        // Trying to import annotations updates annotation data and "saved" check
        // (there are unsaved annotations):
        verifyThat(model.isSaved(), Matchers.is(false));
        verifyThat(mainView.getStatusBar().isSavedStatus(), Matchers.is(false));
    }

    private void importAnnotationAndClickDialogOption(FxRobot robot, File annotationFile, String userChoice) {
        Platform.runLater(
                () -> controller.initiateAnnotationImport(annotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Stage topModalStage = timeOutGetTopModalStage(robot, "Import annotation data");
        timeOutLookUpInStageAndClickOn(robot, topModalStage, userChoice);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Import annotation data");

        WaitForAsyncUtils.waitForFxEvents();
    }
}
