package boundingboxeditor.controller;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ControllerIOTests extends BoundingBoxEditorTestBase {
    private static String REFERENCE_ANNOTATIONS_PATH = "/testannotations/reference";
    private static String EXPECTED_FILE_NAME = "austin-neill-685084-unsplash_jpg_A.xml";
    private static String REFERENCE_FILE_PATH = REFERENCE_ANNOTATIONS_PATH + "/" + EXPECTED_FILE_NAME;
    private static int TIMEOUT_DURATION_IN_SEC = 5;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onSaveAnnotation_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot, @TempDir Path tempDirectory)
            throws TimeoutException, IOException {
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationsDirectory = new File(getClass().getResource(REFERENCE_ANNOTATIONS_PATH).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.new AnnotationLoaderService(referenceAnnotationsDirectory).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory exists.");

        final Map counts = model.getCategoryToAssignedBoundingBoxesCountMap();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Objects.equals(counts.get("Boat"), 2) && Objects.equals(counts.get("Sail"), 6) && Objects.equals(counts.get("Flag"), 1)),
                "Correct bounding box per-category-counts read within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getBoundingBoxEditorImageView())
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);

        WaitForAsyncUtils.waitForFxEvents();

        // Save the annotations to the temporary folder.
        Platform.runLater(() -> controller.new AnnotationSaverService(actualDir.toFile()).startAndShowProgressDialog());
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(EXPECTED_FILE_NAME);

        // Wait until the output-file actually exists. If the file was not created in
        // the specified time-frame, a TimeoutException is thrown and the test fails.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Files.exists(actualFilePath)),
                "Output-file was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        // The output file should be exactly the same as the reference file.
        final File referenceFile = new File(getClass().getResource(REFERENCE_FILE_PATH).getFile());
        final byte[] referenceArray = Files.readAllBytes(referenceFile.toPath());

        // Wait until the annotations were written to the output file and the file is equivalent to the reference file
        // or throw a TimeoutException if this did not happen within the specified time-frame.
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> Arrays.equals(referenceArray, Files.readAllBytes(actualFilePath))),
                "Expected annotation output-file content was not created within " + TIMEOUT_DURATION_IN_SEC + " sec.");
    }
}
