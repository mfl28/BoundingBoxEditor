package boundingboxeditor.controller;

import boundingboxeditor.BoundingBoxEditorTestBase;
import boundingboxeditor.model.io.IOResult;
import boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import boundingboxeditor.ui.BoundingBoxView;
import boundingboxeditor.ui.BoundingPolygonView;
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

import static org.testfx.api.FxAssert.verifyThat;

class ControllerIOTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()), false);
    }

    @Test
    void onExportAnnotation_PVOC_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot, @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationFilePath = "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";
        final String expectedFileName = "austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory does not exist.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getImageFileListView().getSelectionModel().getSelectedItem().isHasAssignedBoundingShapes()
                        && mainView.getCurrentBoundingShapes().stream().filter(viewable -> viewable instanceof BoundingBoxView).count() == 8
                        && mainView.getCurrentBoundingShapes().stream().filter(viewable -> viewable instanceof BoundingPolygonView).count() == 1),
                "Bounding shape counts did not match within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));

        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.new AnnotationSaverService(actualDir.toFile(), ImageAnnotationSaveStrategy.Type.PASCAL_VOC).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

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
                () -> Arrays.equals(referenceArray, Files.readAllBytes(actualFilePath))),
                "Expected annotation output-file content was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onExportAnnotation_YOLO_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot, @TempDir Path tempDirectory)
            throws IOException {
        final String referenceAnnotationDirectoryPath = "/testannotations/yolo/reference";
        final String expectedAnnotationFileName = "austin-neill-685084-unsplash.txt";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationFolder = new File(getClass().getResource(referenceAnnotationDirectoryPath).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFolder, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory does not exist.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getImageFileListView().getSelectionModel().getSelectedItem().isHasAssignedBoundingShapes()
                        && mainView.getCurrentBoundingShapes().stream().filter(viewable -> viewable instanceof BoundingBoxView).count() == 9
                        && mainView.getCurrentBoundingShapes().stream().noneMatch(viewable -> viewable instanceof BoundingPolygonView)),
                "Bounding shape counts did not match within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getEditorImageView())
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));

        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.new AnnotationSaverService(actualDir.toFile(), ImageAnnotationSaveStrategy.Type.YOLO).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(expectedAnnotationFileName);
        Path actualObjectDataFilePath = actualDir.resolve("object.data");

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Files.exists(actualFilePath) && Files.exists(actualObjectDataFilePath)),
                "Output-files were not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final File objectDataFile = referenceAnnotationFolder.toPath().resolve("object.data").toFile();
        final byte[] objectDataFileArray = Files.readAllBytes(objectDataFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Arrays.equals(objectDataFileArray, Files.readAllBytes(actualObjectDataFilePath))),
                "Expected annotation output-file content was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");


        // The output file should be exactly the same as the reference file.
        final File referenceFile = referenceAnnotationFolder.toPath().resolve(expectedAnnotationFileName).toFile();
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Arrays.equals(referenceArray, Files.readAllBytes(actualFilePath))),
                "Expected annotation output-file content was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onLoadAnnotation_YOLO_WhenObjectDataFileMissing_ShouldNotLoadAnnotations(FxRobot robot) {
        final String inputPath = "/testannotations/yolo/missing-classes-file";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(inputPath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Annotation import error report") != null),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final Stage errorReportStage = getTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "The folder does not contain any valid annotation files.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0), Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1));

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry1 = new IOResult.ErrorInfoEntry("object.data",
                "Does not exist in annotation folder \"missing-classes-file\".");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1));

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(0));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));
    }

    @Test
    void onLoadAnnotation_YOLO_WhenAnnotationFileContainsErrors_ShouldNotLoadInvalidBoundingBoxes(FxRobot robot) {
        final String inputPath = "/testannotations/yolo/invalid-annotations";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File inputFile = new File(getClass().getResource(inputPath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Annotation import error report") != null),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final Stage errorReportStage = getTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "Some bounding boxes could not be loaded from 4 image-annotation files.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0), Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(5));

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry1 = new IOResult.ErrorInfoEntry("austin-neill-685084-unsplash.txt",
                "Invalid category index 4 (of 4 categories) on line 1.");

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry2 = new IOResult.ErrorInfoEntry("caleb-george-316073-unsplash.txt",
                "Missing or invalid category index on line 1.");

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry3 = new IOResult.ErrorInfoEntry("nico-bhlr-1067059-unsplash.txt",
                "Missing or invalid bounding-box bounds on line 1.");

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry4 = new IOResult.ErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                "Bounds ratio not within [0, 1] on line 1.");

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry5 = new IOResult.ErrorInfoEntry("tyler-nix-582593-unsplash.txt",
                "Invalid bounding-box coordinates on line 2.");

        verifyThat(errorInfoEntries, Matchers.containsInAnyOrder(referenceErrorInfoEntry1, referenceErrorInfoEntry2,
                referenceErrorInfoEntry3, referenceErrorInfoEntry4, referenceErrorInfoEntry5));

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        verifyThat(counts.size(), Matchers.equalTo(1));
        verifyThat(counts.get("Ship"), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(model.getImageAnnotationData().getImageAnnotations(), Matchers.hasSize(1));

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingNonCriticalElements_ShouldNotLoadIncompleteBoundingBoxes(FxRobot robot) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_elements.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationFile = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Annotation import error report") != null),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final Stage errorReportStage = getTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "Some bounding boxes could not be loaded from 1 image-annotation file.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0), Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(2));

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry1 = new IOResult.ErrorInfoEntry("annotation_with_missing_elements.xml",
                "Missing element: name");
        final IOResult.ErrorInfoEntry referenceErrorInfoEntry2 = new IOResult.ErrorInfoEntry("annotation_with_missing_elements.xml",
                "Missing element: ymin");
        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1, referenceErrorInfoEntry2));

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 1) && Objects.equals(counts.get("Sail"), 6) && counts.get("Flag") == null),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenFileHasMissingCriticalElement_ShouldNotLoadAnyBoundingBoxes(FxRobot robot) {
        final String inputFilePath = "/testannotations/pvoc/annotation_with_missing_filename.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationFile = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Annotation import error report") != null),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final Stage errorReportStage = getTopModalStage(robot, "Annotation import error report");
        verifyThat(errorReportStage, Matchers.notNullValue());

        final String errorReportDialogContentReferenceText = "The folder does not contain any valid annotation files.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0), Matchers.instanceOf(TableView.class));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class));

        @SuppressWarnings("unchecked") final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> errorInfoTable.getItems().size() == 1),
                "Expected number of error info entries not found in " + TIMEOUT_DURATION_IN_SEC + " sec.");

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry = new IOResult.ErrorInfoEntry("annotation_with_missing_filename.xml",
                "Missing element: filename");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry));

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().isEmpty(), Matchers.is(true));
        verifyThat(model.getObjectCategories(), Matchers.empty());
        verifyThat(model.getImageAnnotationData().getImageAnnotations(), Matchers.empty());
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty());

        // Should not have changed the status message.
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));
    }

    @Test
    void onLoadAnnotation_PVOC_WhenAnnotationsPresent_ShouldAskForAndCorrectlyApplyUserChoice(FxRobot robot) {
        final String referenceAnnotationFilePath = "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getCurrentBoundingShapes().size() == 1),
                "Expected number of bounding boxes not found in " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class));

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
    void onReloadAnnotations_afterImageFilesReopened_shouldCorrectlyDisplayBoundingShapes(FxRobot robot) {
        final String referenceAnnotationFilePath = "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationFile = new File(getClass().getResource(referenceAnnotationFilePath).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0));

        loadImageFolderAndClickKeepCategoriesAndSaveAnnotationOptions(robot, TEST_IMAGE_FOLDER_PATH_1, "No", "No");

        verifyThat(model.getCurrentFileIndex(), Matchers.equalTo(0));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(model.getImageFileNameToAnnotationMap().size(), Matchers.equalTo(0));
        verifyThat(controller.lastLoadedImageUrl, Matchers.nullValue());

        // Reload bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(referenceAnnotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> countsReloaded = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(countsReloaded.get("Boat"), 2) && Objects.equals(countsReloaded.get("Sail"), 6)
                        && Objects.equals(countsReloaded.get("Flag"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0));
        verifyThat(mainView.getObjectTree().getRoot().getChildren(), Matchers.hasSize(0));
    }

    private void userChoosesNoOnAnnotationImportDialogSubtest(FxRobot robot, File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "No");

        // All previously existing bounding boxes should have been removed, only
        // the newly imported ones should exist.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(3));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(3));
        verifyThat(model.getImageAnnotationData().getImageAnnotations(), Matchers.hasSize(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty());
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(0));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(0).isHasAssignedBoundingShapes(),
                Matchers.is(true));
    }

    private void userChoosesYesOnAnnotationImportDialogSubTest(FxRobot robot, BoundingBoxView drawnBoundingBox, File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Yes");

        // Everything should have stayed the same for the current image...
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        // ... but there should be additional categories and bounding boxes in the model.
        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingShapesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6)
                        && Objects.equals(counts.get("Flag"), 1) && Objects.equals(counts.get("Test"), 1)),
                "Correct bounding box per-category-counts were not read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(4));
        verifyThat(model.getObjectCategories(), Matchers.hasSize(4));
        verifyThat(model.getImageAnnotationData().getImageAnnotations(), Matchers.hasSize(2));
        verifyThat(model.getImageAnnotationData().getImageAnnotations(), Matchers.hasSize(2));

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
    }

    private void userChoosesCancelOnAnnotationImportDialogSubtest(FxRobot robot,
                                                                  BoundingBoxView drawnBoundingBox,
                                                                  File annotationFile) {
        importAnnotationAndClickDialogOption(robot, annotationFile, "Cancel");

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox));
        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));
    }

    private void importAnnotationAndClickDialogOption(FxRobot robot, File annotationFile, String userChoice) {
        Platform.runLater(() -> controller.initiateAnnotationFolderImport(annotationFile, ImageAnnotationLoadStrategy.Type.PASCAL_VOC));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Import annotation data") != null),
                "Expected info dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage topModalStage = getTopModalStage(robot, "Import annotation data");
        timeOutLookUpInStageAndClickOn(robot, topModalStage, userChoice);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
