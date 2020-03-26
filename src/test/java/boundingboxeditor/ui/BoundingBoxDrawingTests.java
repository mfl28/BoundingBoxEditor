package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import boundingboxeditor.utils.MathUtils;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

class BoundingBoxDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onOpeningNewImageFolder_WhenBoundingBoxesExist_ShouldResetCorrectly(FxRobot robot) throws TimeoutException {
        waitUntilCurrentImageIsLoaded();

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        robot.clickOn("#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        int drawnBoundingBoxFileIndex = model.getCurrentFileIndex();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getCurrentBoundingBoxes().size() == 1),
                "Expected number of bounding boxes not found in " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        final BoundingBoxView drawnBoundingBox = mainView.getCurrentBoundingBoxes().get(0);

        robot.clickOn("#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingBoxFileIndex)
                .isHasAssignedBoundingShapes(), Matchers.is(true));

        robot.clickOn("#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingBoxes(), Matchers.hasItem(drawnBoundingBox));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        final BoundingBoxView boundingBoxView = mainView.getCurrentBoundingBoxes().get(0);

        // Move bounding box to top left corner.
        double boundingBoxWidth = boundingBoxView.getWidth();
        double boundingBoxHeight = boundingBoxView.getHeight();
        moveRelativeToImageView(robot, new Point2D(0.5, 0.5), new Point2D(0.0, 0.0));
        WaitForAsyncUtils.waitForFxEvents();

        Point2D imageViewRelativePointTopLeft = getParentPointFromImageViewRatios(new Point2D(0.0, 0.0));
        verifyThat(boundingBoxView.getX(), Matchers.closeTo(imageViewRelativePointTopLeft.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getY(), Matchers.closeTo(imageViewRelativePointTopLeft.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getWidth(), Matchers.closeTo(boundingBoxWidth, MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getHeight(), Matchers.closeTo(boundingBoxHeight, MathUtils.DOUBLE_EQUAL_THRESHOLD));

        // Move bounding box to bottom right corner.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(1.0, 1.0));
        WaitForAsyncUtils.waitForFxEvents();

        Point2D imageViewRelativePointBottomRight = getParentPointFromImageViewRatios(new Point2D(1.0, 1.0));
        verifyThat(boundingBoxView.getX() + boundingBoxView.getWidth(), Matchers.closeTo(imageViewRelativePointBottomRight.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getY() + boundingBoxView.getHeight(), Matchers.closeTo(imageViewRelativePointBottomRight.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getWidth(), Matchers.closeTo(boundingBoxWidth, MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getHeight(), Matchers.closeTo(boundingBoxHeight, MathUtils.DOUBLE_EQUAL_THRESHOLD));

        Platform.runLater(() -> controller.initiateImageFolderLoading(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_2).getFile())));
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Open image folder") != null),
                "Expected info dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(getTopModalStage(robot, "Open image folder"), Matchers.notNullValue());

        // Do not save existing bounding box annotations.
        robot.clickOn("No");
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
        verifyThat(mainView.getCurrentBoundingBoxes().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(0));

        verifyThat(mainView.getImageFileListView().getItems()
                        .stream().noneMatch(ImageFileListView.FileInfo::isHasAssignedBoundingShapes),
                Matchers.is(true));

        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(0));
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingBoxes().size(), Matchers.equalTo(1));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        final BoundingBoxView boundingBoxView3 = mainView.getCurrentBoundingBoxes().get(0);
        double preResizeMaxX = boundingBoxView3.getX() + boundingBoxView3.getWidth();
        double preResizeMaxY = boundingBoxView3.getY() + boundingBoxView3.getHeight();

        // Resize test:
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.0, 0.0));
        WaitForAsyncUtils.waitForFxEvents();

        imageViewRelativePointTopLeft = getParentPointFromImageViewRatios(new Point2D(0.0, 0.0));
        verifyThat(boundingBoxView3.getX(), Matchers.closeTo(imageViewRelativePointTopLeft.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getY(), Matchers.closeTo(imageViewRelativePointTopLeft.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getX() + boundingBoxView3.getWidth(), Matchers.closeTo(preResizeMaxX, MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getY() + boundingBoxView3.getHeight(), Matchers.closeTo(preResizeMaxY, MathUtils.DOUBLE_EQUAL_THRESHOLD));

        // Try to exit application:
        robot.clickOn("File").clickOn("Exit");

        verifyThat(getTopModalStage(robot, "Exit Application"), Matchers.notNullValue());
        robot.clickOn("Cancel");
    }
}
