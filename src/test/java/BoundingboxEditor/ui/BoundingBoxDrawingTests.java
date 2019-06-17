package BoundingboxEditor.ui;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

class BoundingBoxDrawingTests extends BoundingBoxEditorTestBase {
    private static String TEST_IMAGE_FOLDER_PATH_1 = "/TestImages/MediumSizedImages";
    private static String TEST_IMAGE_FOLDER_PATH_2 = "/TestImages";

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFilesFromDirectory(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onLoadingOtherFolder_AfterBoundingBoxDrawn_ShouldResetCorrectly(FxRobot robot) throws TimeoutException {
        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#next-button");
        WaitForAsyncUtils.waitForFxEvents();

        drawBoundingBox(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(1));
        final BoundingBoxView drawnBoundingBox = mainView.getCurrentBoundingBoxes().get(0);

        robot.clickOn("#previous-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(0));

        robot.clickOn("#next-button");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingBoxes(), CoreMatchers.hasItem(drawnBoundingBox));

        Platform.runLater(() -> controller.loadImageFilesFromDirectory(
                new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_2).getFile()))
        );

        WaitForAsyncUtils.waitForFxEvents();
        waitUntilCurrentImageIsLoaded();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(0));

        enterNewCategory(robot, "Test");
        WaitForAsyncUtils.waitForFxEvents();

        drawBoundingBox(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), CoreMatchers.equalTo(1));

    }
}
