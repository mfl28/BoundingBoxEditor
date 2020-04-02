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

import static org.testfx.api.FxAssert.verifyThat;

class BoundingBoxDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onOpeningNewImageFolder_WhenBoundingBoxesExist_ShouldResetCorrectly(FxRobot robot) {
        waitUntilCurrentImageIsLoaded();

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        int drawnBoundingBoxFileIndex = model.getCurrentFileIndex();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getCurrentBoundingShapes().size() == 1),
                "Expected number of bounding boxes not found in " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class));
        final BoundingBoxView drawnBoundingBox = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);

        timeOutClickOn(robot, "#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingBoxFileIndex)
                .isHasAssignedBoundingShapes(), Matchers.is(true));

        timeOutClickOn(robot, "#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class));

        final BoundingBoxView boundingBoxView = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);

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

        Stage dialogStage = getTopModalStage(robot, "Open image folder");
        verifyThat(dialogStage, Matchers.notNullValue());

        // Do not save existing bounding box annotations.
        timeOutLookUpInStageAndClickOn(robot, dialogStage, "No");
        WaitForAsyncUtils.waitForFxEvents();

        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(0));

        verifyThat(mainView.getImageFileListView().getItems()
                        .stream().noneMatch(ImageFileListView.FileInfo::isHasAssignedBoundingShapes),
                Matchers.is(true));

        enterNewCategory(robot, testCategoryName);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(0));
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class));

        final BoundingBoxView boundingBoxView3 = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);
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
        timeOutClickOn(robot, "File");
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Exit");
        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> getTopModalStage(robot, "Exit Application") != null),
                "Expected info dialog did not open within " + TIMEOUT_DURATION_IN_SEC + " sec.");

        Stage exitDialogStage = getTopModalStage(robot, "Exit Application");
        verifyThat(exitDialogStage, Matchers.notNullValue());

        timeOutLookUpInStageAndClickOn(robot, exitDialogStage, "Cancel");
    }
}
