package boundingboxeditor.ui;

import boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

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

        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));

        WaitForAsyncUtils.waitForFxEvents();
        int drawnBoundingPolygonFileIndex = model.getCurrentFileIndex();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                () -> mainView.getCurrentBoundingShapes().size() == 1),
                "Expected number of bounding polygons not found in " + TIMEOUT_DURATION_IN_SEC + " sec.");

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class));

        final BoundingPolygonView drawnBoundingPolygon = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        final List<Double> drawnPointCoordinates = List.of(drawnBoundingPolygon.getPoints().toArray(Double[]::new));

        verifyThat(drawnPointCoordinates, Matchers.hasSize(targetImageViewPointRatios.length));
        verifyThat(drawnBoundingPolygon.getVertexHandles(), Matchers.hasSize(targetImageViewPointRatios.length / 2));
        verifyThat(drawnBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true));
        verifyThat(drawnBoundingPolygon.getVertexHandles().get(drawnBoundingPolygon.getVertexHandles().size() - 1).isEditing(), Matchers.equalTo(true));
        verifyThat(drawnBoundingPolygon, NodeMatchers.isVisible());
        verifyThat(drawnBoundingPolygon.getImageRelativeRatios().toArray(Double[]::new), ratioListCloseTo(targetImageViewPointRatios));
        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(true));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(true));

        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(), Matchers.equalTo(drawnBoundingPolygon));

        robot.rightClickOn(mainView.getEditorImageView());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(false));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(false));
        verifyThat(drawnBoundingPolygon.isEditing(), Matchers.equalTo(false));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(), Matchers.nullValue());

        robot.clickOn(drawnBoundingPolygon, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(true));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(false));
        verifyThat(drawnBoundingPolygon.isEditing(), Matchers.equalTo(false));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(), Matchers.equalTo(drawnBoundingPolygon));

        robot.clickOn("#previous-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingPolygonFileIndex)
                .isHasAssignedBoundingShapes(), Matchers.is(true));

        robot.clickOn("#next-button");
        waitUntilCurrentImageIsLoaded();
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class));

        final BoundingPolygonView reloadedBoundingPolygon = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);

        verifyThat(reloadedBoundingPolygon, NodeMatchers.isVisible());
        verifyThat(reloadedBoundingPolygon.getPoints(), Matchers.hasSize(targetImageViewPointRatios.length));
        verifyThat(reloadedBoundingPolygon.getVertexHandles(), Matchers.hasSize(targetImageViewPointRatios.length / 2));
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

        verifyThat(reloadedBoundingPolygon.isSelected(), Matchers.equalTo(true));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(), Matchers.equalTo(reloadedBoundingPolygon));

        Point2D dragEndRatiosPoint = new Point2D(0.1, 0.1);

        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), dragEndRatiosPoint);
        WaitForAsyncUtils.waitForFxEvents();

        List<Double> actualRatios = reloadedBoundingPolygon.getImageRelativeRatios();
        targetImageViewPointRatios[0] = 0.1;
        targetImageViewPointRatios[1] = 0.1;

        verifyThat(actualRatios.toArray(Double[]::new), ratioListCloseTo(targetImageViewPointRatios));

        // Select a handle and try to select a subsequent invalid handle.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE, new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(1).isEditing(), Matchers.equalTo(false));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(2).isEditing(), Matchers.equalTo(false));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(3).isEditing(), Matchers.equalTo(false));

        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.hasSize(1));
        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.contains(0));

        // Select two more valid handles.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE, new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(1).isEditing(), Matchers.equalTo(true));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(2).isEditing(), Matchers.equalTo(false));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(3).isEditing(), Matchers.equalTo(true));

        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.hasSize(3));
        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.containsInRelativeOrder(0, 2, 6));

        // Splice.
        robot.clickOn(reloadedBoundingPolygon, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles(), Matchers.hasSize(6));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isEditing), Matchers.equalTo(false));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().stream().map(BoundingPolygonView.VertexHandle::getPointIndex).collect(Collectors.toList()),
                Matchers.containsInRelativeOrder(0, 2, 4, 6, 8, 10));
        verifyThat(reloadedBoundingPolygon.isEditing(), Matchers.equalTo(false));
        verifyThat(reloadedBoundingPolygon.isSelected(), Matchers.equalTo(true));

        // Remove polygon.
        robot.rightClickOn(reloadedBoundingPolygon).clickOn("Delete");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty());
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(), Matchers.equalTo(null));

        // Start to construct a new polygon.
        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        // Change drawing mode while construction is in progress.
        robot.clickOn("Rectangle");
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(1));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class));

        BoundingPolygonView newBoundingPolygonView = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(newBoundingPolygonView, Matchers.notNullValue());

        verifyThat(newBoundingPolygonView.isConstructing(), Matchers.equalTo(false));
        verifyThat(newBoundingPolygonView.isEditing(), Matchers.equalTo(false));
        verifyThat(newBoundingPolygonView.isSelected(), Matchers.equalTo(true));

        // Select all handles.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE,
                new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(newBoundingPolygonView.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isEditing), Matchers.equalTo(true));
        verifyThat(newBoundingPolygonView.getEditingIndices(), Matchers.containsInRelativeOrder(0, 2, 4, 6));

        // Splice.
        robot.clickOn(newBoundingPolygonView, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(newBoundingPolygonView.getVertexHandles(), Matchers.hasSize(8));
        verifyThat(newBoundingPolygonView.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isEditing), Matchers.equalTo(false));
        verifyThat(newBoundingPolygonView.getVertexHandles().stream().map(BoundingPolygonView.VertexHandle::getPointIndex).collect(Collectors.toList()),
                Matchers.containsInRelativeOrder(0, 2, 4, 6, 8, 10, 12, 14));
        verifyThat(newBoundingPolygonView.isEditing(), Matchers.equalTo(false));
        verifyThat(newBoundingPolygonView.isSelected(), Matchers.equalTo(true));

    }
}
