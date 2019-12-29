package boundingboxeditor.controller;

import boundingboxeditor.BoundingBoxEditorTestBase;
import boundingboxeditor.model.io.IOResult;
import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.Start;
import org.testfx.service.finder.NodeFinder;
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
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

class ControllerIOTests extends BoundingBoxEditorTestBase {
    private static int TIMEOUT_DURATION_IN_SEC = 5;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onSaveAnnotation_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot, @TempDir Path tempDirectory)
            throws TimeoutException, IOException {
        final String referenceAnnotationsPath = "/testannotations/reference";
        final String expectedFileName = "austin-neill-685084-unsplash_jpg_A.xml";
        final String referenceFilePath = referenceAnnotationsPath + "/" + expectedFileName;

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));

        final File referenceAnnotationsDirectory = new File(getClass().getResource(referenceAnnotationsPath).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.new AnnotationLoaderService(referenceAnnotationsDirectory).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory exists.");

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingBoxesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getBoundingBoxEditorImageView())
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));

        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.new AnnotationSaverService(actualDir.toFile()).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(expectedFileName);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Files.exists(actualFilePath)),
                "Output-file was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // The output file should be exactly the same as the reference file.
        final File referenceFile = new File(getClass().getResource(referenceFilePath).getFile());
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Arrays.equals(referenceArray, Files.readAllBytes(actualFilePath))),
                "Expected annotation output-file content was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");
    }

    @Test
    void onLoadAnnotation_WhenFileHasMissingNonCriticalElements_ShouldNotLoadIncompleteBoundingBoxes(FxRobot robot)
            throws TimeoutException {
        final String inputFilePath = "/testannotations/annotation_with_missing_elements.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationsDirectory = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.new AnnotationLoaderService(referenceAnnotationsDirectory).startAndShowProgressDialog());
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

        @SuppressWarnings("unchecked")
        final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(2));

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry1 = new IOResult.ErrorInfoEntry("annotation_with_missing_elements.xml",
                "Missing element: name");
        final IOResult.ErrorInfoEntry referenceErrorInfoEntry2 = new IOResult.ErrorInfoEntry("annotation_with_missing_elements.xml",
                "Missing element: ymin");
        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1, referenceErrorInfoEntry2));

        // Close error report dialog.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> robot.lookup("OK").tryQuery().isPresent()),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Assertions.assertDoesNotThrow(() ->robot.clickOn("OK"));
        WaitForAsyncUtils.waitForFxEvents();

        final Map<String, Integer> counts = model.getCategoryToAssignedBoundingBoxesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 1) && Objects.equals(counts.get("Sail"), 6) && counts.get("Flag") == null),
                "Correct bounding box per-category-counts read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully imported annotations from 1 file in"));
    }

    @Test
    void onLoadAnnotation_WhenFileHasMissingCriticalElement_ShouldNotLoadAnyBoundingBoxes(FxRobot robot)
            throws TimeoutException {
        final String inputFilePath = "/testannotations/annotation_with_missing_filename.xml";

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationsDirectory = new File(getClass().getResource(inputFilePath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.new AnnotationLoaderService(referenceAnnotationsDirectory).startAndShowProgressDialog());
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

        @SuppressWarnings("unchecked")
        final TableView<IOResult.ErrorInfoEntry> errorInfoTable = (TableView<IOResult.ErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOResult.ErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1));

        final IOResult.ErrorInfoEntry referenceErrorInfoEntry = new IOResult.ErrorInfoEntry("annotation_with_missing_filename.xml",
                "Missing element: filename");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry));

        // Close error report dialog.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> robot.lookup("OK").tryQuery().isPresent()),
                "Expected error report dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Assertions.assertDoesNotThrow(() ->robot.clickOn("OK"));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingBoxesCountMap().isEmpty(), Matchers.is(true));
        verifyThat(model.getBoundingBoxCategories(), Matchers.empty());
        verifyThat(model.getImageAnnotations(), Matchers.empty());
        verifyThat(mainView.getCurrentBoundingBoxes(), Matchers.empty());

        // Should not have changed the status message.
        verifyThat(mainView.getStatusBar().getCurrentEventMessage(), Matchers.startsWith("Successfully loaded 4 image-files from folder "));
    }
}
