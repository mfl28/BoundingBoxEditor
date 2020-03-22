package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.geometry.Point2D;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.testfx.api.FxAssert.verifyThat;

class BoundingPolygonDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onOpeningNewImageFolder_WhenBoundingPolygonsExist_ShouldResetCorrectly(FxRobot robot) throws TimeoutException {
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

        // Select polygon drawing mode:
        robot.clickOn("Polygon");
        WaitForAsyncUtils.waitForFxEvents();

        // Draw a bounding polygon.
        Double[] targetImageViewPointRatios = {0.25, 0.25, 0.1, 0.6, 0.4, 0.75, 0.75, 0.3};

        moveAndClickRelativeToImageView(robot,
                new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        robot.rightClickOn();
        WaitForAsyncUtils.waitForFxEvents();

        int drawnBoundingPolygonFileIndex = model.getCurrentFileIndex();

        verifyThat(mainView.getCurrentBoundingPolygons().size(), Matchers.equalTo(1));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        final BoundingPolygonView drawnBoundingPolygon = mainView.getCurrentBoundingPolygons().get(0);
        final List<Double> drawnPointCoordinates = List.of(drawnBoundingPolygon.getPoints().toArray(Double[]::new));

        verifyThat(drawnPointCoordinates, Matchers.hasSize(8));
        verifyThat(drawnBoundingPolygon, NodeMatchers.isVisible());
        verifyThat(drawnBoundingPolygon.getImageRelativeRatios().toArray(Double[]::new), ratioListCloseTo(targetImageViewPointRatios));

        robot.clickOn("#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingPolygons().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingPolygonFileIndex)
                .isHasAssignedBoundingShapes(), Matchers.is(true));

        robot.clickOn("#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingPolygons().size(), Matchers.equalTo(1));

        final BoundingPolygonView reloadedBoundingPolygon = mainView.getCurrentBoundingPolygons().get(0);

        verifyThat(reloadedBoundingPolygon, NodeMatchers.isVisible());
        verifyThat(reloadedBoundingPolygon.getPoints(), Matchers.hasSize(8));
        verifyThat(reloadedBoundingPolygon.getPoints().toArray(Double[]::new),
                doubleListCloseTo(drawnPointCoordinates.toArray(Double[]::new)));
        verifyThat(reloadedBoundingPolygon.getObjectCategory(), Matchers.equalTo(drawnBoundingPolygon.getObjectCategory()));
        verifyThat(reloadedBoundingPolygon.getImageMetaData(), Matchers.equalTo(drawnBoundingPolygon.getImageMetaData()));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        // Move handle.
        robot.clickOn(reloadedBoundingPolygon);
        WaitForAsyncUtils.waitForFxEvents();

        Point2D dragEndRatiosPoint = new Point2D(0.1, 0.1);

        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), dragEndRatiosPoint);
        WaitForAsyncUtils.waitForFxEvents();

        List<Double> actualRatios = reloadedBoundingPolygon.getImageRelativeRatios();
        targetImageViewPointRatios[0] = 0.1;
        targetImageViewPointRatios[1] = 0.1;

        verifyThat(actualRatios.toArray(Double[]::new), ratioListCloseTo(targetImageViewPointRatios));
    }
}
