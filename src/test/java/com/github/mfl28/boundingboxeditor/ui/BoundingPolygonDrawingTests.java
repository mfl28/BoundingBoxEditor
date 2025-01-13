/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
 *
 * This file is part of Bounding Box Editor
 *
 * Bounding Box Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bounding Box Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Bounding Box Editor. If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mfl28.boundingboxeditor.ui;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.base.NodeMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class BoundingPolygonDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onOpeningNewImageFolder_WhenBoundingPolygonsExist_ShouldResetCorrectly(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));

        // Select polygon drawing mode:
        timeOutClickOn(robot, "#polygon-mode-button-icon", testinfo);
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
                                                                      () -> mainView.getCurrentBoundingShapes()
                                                                                    .size() == 1),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected number of bounding polygons " +
                                                                                   "not found in " +
                                                                                   TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class),
                   saveScreenshot(testinfo));

        final BoundingPolygonView drawnBoundingPolygon =
                (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        final List<Double> drawnPointCoordinates = List.of(drawnBoundingPolygon.getPoints().toArray(Double[]::new));

        verifyThat(drawnPointCoordinates, Matchers.hasSize(targetImageViewPointRatios.length),
                   saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles(), Matchers.hasSize(targetImageViewPointRatios.length / 2),
                   saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles().get(drawnBoundingPolygon.getVertexHandles().size() - 1)
                                       .isEditing(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon, NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getRelativePointsInImageView().toArray(Double[]::new),
                   ratioListCloseTo(targetImageViewPointRatios), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(true), saveScreenshot(testinfo));

        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                   Matchers.equalTo(drawnBoundingPolygon), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(true));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.POLYGON));
        verifyThat(robot.lookup("#bounding-shape-scene-group").query().isMouseTransparent(), Matchers.is(true));

        robot.rightClickOn(mainView.getEditorImageView());
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                   Matchers.nullValue(), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(false));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.NONE));
        verifyThat(robot.lookup("#bounding-shape-scene-group").query().isMouseTransparent(), Matchers.is(false));


        robot.clickOn(drawnBoundingPolygon, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                   Matchers.equalTo(drawnBoundingPolygon), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#previous-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingPolygonFileIndex)
                           .isHasAssignedBoundingShapes(), Matchers.is(true), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class),
                   saveScreenshot(testinfo));

        final BoundingPolygonView reloadedBoundingPolygon =
                (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);

        verifyThat(reloadedBoundingPolygon, NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getPoints(), Matchers.hasSize(targetImageViewPointRatios.length),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles(), Matchers.hasSize(targetImageViewPointRatios.length / 2),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getPoints().toArray(Double[]::new),
                   doubleListCloseTo(drawnPointCoordinates.toArray(Double[]::new)), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getObjectCategory(),
                   Matchers.equalTo(drawnBoundingPolygon.getObjectCategory()), saveScreenshot(testinfo));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                   saveScreenshot(testinfo));

        // Move handle.
        robot.clickOn(reloadedBoundingPolygon);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                   Matchers.equalTo(reloadedBoundingPolygon), saveScreenshot(testinfo));

        Point2D dragEndRatiosPoint = new Point2D(0.1, 0.1);

        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), dragEndRatiosPoint);
        WaitForAsyncUtils.waitForFxEvents();

        List<Double> actualRatios = reloadedBoundingPolygon.getRelativePointsInImageView();
        targetImageViewPointRatios[0] = 0.1;
        targetImageViewPointRatios[1] = 0.1;

        verifyThat(actualRatios.toArray(Double[]::new), ratioListCloseTo(targetImageViewPointRatios),
                   saveScreenshot(testinfo));

        // Select a handle and try to select a subsequent invalid handle.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE,
                                        new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                                        new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(1).isEditing(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(2).isEditing(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(3).isEditing(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));

        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.hasSize(1), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.contains(0), saveScreenshot(testinfo));

        // Select two more valid handles.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE,
                                        new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                                        new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(0).isEditing(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(1).isEditing(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(2).isEditing(), Matchers.equalTo(false),
                   saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().get(3).isEditing(), Matchers.equalTo(true),
                   saveScreenshot(testinfo));

        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.hasSize(3), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getEditingIndices(), Matchers.containsInRelativeOrder(0, 2, 6),
                   saveScreenshot(testinfo));

        // Splice.
        robot.press(KeyCode.SHIFT).clickOn(reloadedBoundingPolygon, MouseButton.MIDDLE).release(KeyCode.SHIFT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(reloadedBoundingPolygon.getVertexHandles(), Matchers.hasSize(6), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.getVertexHandles().stream()
                                          .allMatch(BoundingPolygonView.VertexHandle::isEditing),
                   Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(
                reloadedBoundingPolygon.getVertexHandles().stream().map(BoundingPolygonView.VertexHandle::getPointIndex)
                                       .collect(Collectors.toList()),
                Matchers.containsInRelativeOrder(0, 2, 4, 6, 8, 10), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(reloadedBoundingPolygon.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));

        // Remove polygon.
        robot.rightClickOn(reloadedBoundingPolygon);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#add-vertices-context-menu", NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat("#delete-vertices-context-menu", NodeMatchers.isVisible(), saveScreenshot(testinfo));

        timeOutClickOn(robot, "Delete", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.empty(), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                   Matchers.equalTo(null), saveScreenshot(testinfo));

        // Start to construct a new polygon.
        moveAndClickRelativeToImageView(robot, MouseButton.PRIMARY,
                                        new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                                        new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                                        new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                                        new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        // Change drawing mode while construction is in progress.
        timeOutClickOn(robot, "#rectangle-mode-button-icon", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(1), saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class),
                   saveScreenshot(testinfo));

        BoundingPolygonView newBoundingPolygonView = (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(newBoundingPolygonView, Matchers.notNullValue(), saveScreenshot(testinfo));

        verifyThat(newBoundingPolygonView.isConstructing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));

        // Select all handles.
        moveAndClickRelativeToImageView(robot, MouseButton.MIDDLE,
                                        new Point2D(targetImageViewPointRatios[0], targetImageViewPointRatios[1]),
                                        new Point2D(targetImageViewPointRatios[2], targetImageViewPointRatios[3]),
                                        new Point2D(targetImageViewPointRatios[4], targetImageViewPointRatios[5]),
                                        new Point2D(targetImageViewPointRatios[6], targetImageViewPointRatios[7]));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(newBoundingPolygonView.getVertexHandles().stream()
                                         .allMatch(BoundingPolygonView.VertexHandle::isEditing),
                   Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.getEditingIndices(), Matchers.containsInRelativeOrder(0, 2, 4, 6),
                   saveScreenshot(testinfo));

        // Add vertices.
        robot.press(KeyCode.SHIFT).clickOn(newBoundingPolygonView, MouseButton.MIDDLE).release(KeyCode.SHIFT);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(newBoundingPolygonView.getVertexHandles(), Matchers.hasSize(8), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.getVertexHandles().stream()
                                         .allMatch(BoundingPolygonView.VertexHandle::isEditing),
                   Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(
                newBoundingPolygonView.getVertexHandles().stream().map(BoundingPolygonView.VertexHandle::getPointIndex)
                                      .collect(Collectors.toList()),
                Matchers.containsInRelativeOrder(0, 2, 4, 6, 8, 10, 12, 14), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));

        // Select some vertices.
        final BoundingPolygonView.VertexHandle vertexHandle1 = newBoundingPolygonView.getVertexHandles().get(0);
        final BoundingPolygonView.VertexHandle vertexHandle2 = newBoundingPolygonView.getVertexHandles().get(1);

        robot.clickOn(vertexHandle1, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn(vertexHandle2, MouseButton.MIDDLE);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(vertexHandle1.isEditing(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(vertexHandle2.isEditing(), Matchers.equalTo(true), saveScreenshot(testinfo));

        // Delete selected vertices.
        robot.rightClickOn(newBoundingPolygonView);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutClickOn(robot, "#delete-vertices-context-menu", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(newBoundingPolygonView.getVertexHandles(), Matchers.hasSize(6), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.getVertexHandles(),
                   Matchers.not(Matchers.contains(vertexHandle1, vertexHandle2)), saveScreenshot(testinfo));
        verifyThat(
                newBoundingPolygonView.getVertexHandles().stream().map(BoundingPolygonView.VertexHandle::getPointIndex)
                                      .collect(Collectors.toList()),
                Matchers.containsInRelativeOrder(0, 2, 4, 6, 8, 10), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.getVertexHandles().stream()
                                         .allMatch(BoundingPolygonView.VertexHandle::isEditing),
                   Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isEditing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(newBoundingPolygonView.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
    }

    @Test
    void onFreehandDrawing_WhenImageFolderLoaded_ShouldCorrectlyCreatePolygons(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        // Select polygon drawing mode:
        timeOutClickOn(robot, "#freehand-mode-button-icon", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        // Draw a bounding polygon.
        Double[] targetImageViewPointRatios = {0.25, 0.25, 0.1, 0.6, 0.4, 0.75, 0.75, 0.3};
        List<Point2D> screenPoints = IntStream.range(0, targetImageViewPointRatios.length)
                .filter(i -> i % 2 == 0)
                .mapToObj(i -> getScreenPointFromRatios(mainView.getEditorImageView(),
                        new Point2D(targetImageViewPointRatios[i], targetImageViewPointRatios[i+1])))
                .toList();

        robot.moveTo(screenPoints.get(0)).press(MouseButton.PRIMARY);

        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getCurrentBoundingShapes()
                                .size() == 1 && mainView.getCurrentBoundingShapes().get(0) instanceof BoundingFreehandShapeView),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of bounding freehand shapes " +
                                "not found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingFreehandShapeView.class),
                saveScreenshot(testinfo));

        final BoundingFreehandShapeView boundingFreehandShapeView = (BoundingFreehandShapeView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(boundingFreehandShapeView.getElements(), Matchers.not(Matchers.empty()));
        verifyThat(boundingFreehandShapeView.getElements().get(0), Matchers.instanceOf(MoveTo.class));

        verifyThat(boundingFreehandShapeView, NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat(boundingFreehandShapeView.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                Matchers.equalTo(boundingFreehandShapeView), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(), Matchers.equalTo(EditorImagePaneView.DrawingMode.FREEHAND));

        int numPathElements = boundingFreehandShapeView.getElements().size();
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(true));
        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.FREEHAND));
        verifyThat(robot.lookup("#bounding-shape-scene-group").query().isMouseTransparent(), Matchers.is(true));


        robot.moveTo(screenPoints.get(1));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(boundingFreehandShapeView.getElements().size(), Matchers.greaterThan(numPathElements));
        verifyThat(boundingFreehandShapeView.getElements().get(boundingFreehandShapeView.getElements().size() - 1),
                Matchers.instanceOf(LineTo.class));

        verifyThat(boundingFreehandShapeView.getRelativeOutlineRectangle(), Matchers.nullValue());

        robot.moveTo(screenPoints.get(2)).moveTo(screenPoints.get(3)).release(MouseButton.PRIMARY);

        WaitForAsyncUtils.waitForFxEvents();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> mainView.getCurrentBoundingShapes()
                                .size() == 1 && mainView.getCurrentBoundingShapes().get(0) instanceof BoundingPolygonView),
                () -> saveScreenshotAndReturnMessage(testinfo,
                        "Expected number of bounding polygons " +
                                "not found in " +
                                TIMEOUT_DURATION_IN_SEC +
                                " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                        .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                saveScreenshot(testinfo));

        verifyThat(mainView.getEditorImagePane().getCurrentBoundingShapeDrawingMode(),
                Matchers.equalTo(EditorImagePaneView.DrawingMode.NONE));
        verifyThat(mainView.getEditorImagePane().isDrawingInProgress(), Matchers.is(false));
        verifyThat(robot.lookup("#bounding-shape-scene-group").query().isMouseTransparent(), Matchers.is(false));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingPolygonView.class),
                saveScreenshot(testinfo));

        final BoundingPolygonView drawnBoundingPolygon =
                (BoundingPolygonView) mainView.getCurrentBoundingShapes().get(0);
        final List<Double> drawnPointCoordinates = List.of(drawnBoundingPolygon.getPoints().toArray(Double[]::new));
        verifyThat(drawnPointCoordinates.size(), Matchers.greaterThan(targetImageViewPointRatios.length),
                saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon, NodeMatchers.isVisible(), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isEditing),
                Matchers.equalTo(false),
                saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isVisible),
                Matchers.equalTo(true),
                saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.getVertexHandles().stream().allMatch(BoundingPolygonView.VertexHandle::isSelected),
                Matchers.equalTo(false),
                saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isSelected(), Matchers.equalTo(true), saveScreenshot(testinfo));
        verifyThat(drawnBoundingPolygon.isConstructing(), Matchers.equalTo(false), saveScreenshot(testinfo));
        verifyThat(mainView.getEditorImagePane().getBoundingShapeSelectionGroup().getSelectedToggle(),
                Matchers.equalTo(drawnBoundingPolygon), saveScreenshot(testinfo));

        mainView.getEditorSettingsConfig().simplifyRelativeDistanceToleranceProperty().set(1.0);
        WaitForAsyncUtils.waitForFxEvents();

        robot.rightClickOn(drawnBoundingPolygon).clickOn("Simplify");

        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(drawnBoundingPolygon.getRelativePointsInImageView().toArray(Double[]::new),
                ratioListCloseTo(targetImageViewPointRatios), saveScreenshot(testinfo));
    }
}
