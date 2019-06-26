package BoundingBoxEditor.controller;

import BoundingBoxEditor.BoundingBoxEditorTestBase;
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
import java.util.concurrent.TimeoutException;

class ControllerIOTests extends BoundingBoxEditorTestBase {
    private static String REFERENCE_ANNOTATIONS_PATH = "/testannotations/reference";
    private static String EXPECTED_FILE_NAME = "austin-neill-685084-unsplash_jpg_A.xml";
    private static String REFERENCE_FILE_PATH = REFERENCE_ANNOTATIONS_PATH + "/" + EXPECTED_FILE_NAME;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFilesFromDirectory(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onSaveAnnotation_WhenPreviouslyImportedAnnotation_ShouldProduceEquivalentOutput(FxRobot robot, @TempDir Path tempDirectory)
            throws TimeoutException, IOException {
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        final File referenceAnnotationsDirectory = new File(getClass().getResource(REFERENCE_ANNOTATIONS_PATH).getFile());
        final File referenceFile = new File(getClass().getResource(REFERENCE_FILE_PATH).getFile());

        // Load bounding-boxes defined in the reference annotation-file.
        Platform.runLater(() -> controller.importAnnotationsFromDirectory(referenceAnnotationsDirectory));
        WaitForAsyncUtils.waitForFxEvents();

        // Create temporary folder to save annotations to.
        Path actualDir = Files.createDirectory(tempDirectory.resolve("actual"));

        Assertions.assertTrue(Files.isDirectory(actualDir), "Actual files directory exists.");

        // Zoom a bit to change the image-view size.
        robot.moveTo(mainView.getBoundingBoxEditorImageView())
                .press(KeyCode.CONTROL)
                .scroll(-30)
                .release(KeyCode.CONTROL);

        // Save the annotations to the temporary folder.
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> controller.saveAnnotationsToDirectory(actualDir.toFile()));
        WaitForAsyncUtils.waitForFxEvents();

        Path actualFilePath = actualDir.resolve(EXPECTED_FILE_NAME);

        // Check if the expected output-file exists.
        Assertions.assertTrue(Files.exists(actualFilePath), "Actual file exists.");

        WaitForAsyncUtils.waitForFxEvents();

        // The files should be exactly the same.
        Assertions.assertArrayEquals(Files.readAllBytes(referenceFile.toPath()), Files.readAllBytes(actualFilePath));
    }
}
