/*
 * Copyright (C) 2026 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.data.BoundingMaskData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class MaskDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onPaintingMasks_ShouldCreateExtendEraseAndUndoThem(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Road", testinfo);
        timeOutClickOn(robot, "#mask-mode-button-icon", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getEditorImagePane().getDrawingMode(),
                   Matchers.equalTo(EditorImagePaneView.DrawingMode.MASK), saveScreenshot(testinfo));
        verifyThat(robot.lookup("#mask-tool-box").query().isVisible(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(robot.lookup("#bounding-shape-scene-group").query().isMouseTransparent(), Matchers.is(true),
                   saveScreenshot(testinfo));

        // A stroke creates a mask of the selected category, on the pixel grid of the image file.
        paint(robot, new Point2D(0.2, 0.3), new Point2D(0.5, 0.3));
        waitForShapeCount(1, testinfo);

        final BoundingMaskView mask = (BoundingMaskView) mainView.getCurrentBoundingShapes().getFirst();
        final int firstArea = mask.getMask().getArea();
        verifyThat(firstArea, Matchers.greaterThan(0), saveScreenshot(testinfo));
        verifyThat(mask.isSelected(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(mask.getViewData().getObjectCategory().getName(), Matchers.equalTo("Road"),
                   saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("Road"), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat((double) mask.getMask().getImageWidth(),
                   Matchers.equalTo(model.getCurrentImageMetaData().getOrientedWidth()), saveScreenshot(testinfo));

        // The next stroke paints into the same (selected) mask.
        paint(robot, new Point2D(0.2, 0.5), new Point2D(0.5, 0.5));
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        final int extendedArea = mask.getMask().getArea();
        verifyThat(extendedArea, Matchers.greaterThan(firstArea), saveScreenshot(testinfo));

        // Shift erases.
        robot.press(KeyCode.SHIFT);
        paint(robot, new Point2D(0.35, 0.25), new Point2D(0.35, 0.55));
        robot.release(KeyCode.SHIFT);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(mask.getMask().getArea(), Matchers.lessThan(extendedArea), saveScreenshot(testinfo));

        // Undo restores the mask from before erasing.
        robot.interact(controller::onRegisterUndoAction);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(currentMaskAreas(), Matchers.equalTo(List.of(extendedArea)), saveScreenshot(testinfo));

        // After requesting a new mask, a stroke over the existing mask starts another one.
        timeOutClickOn(robot, "#new-mask-button-icon", testinfo);
        paint(robot, new Point2D(0.3, 0.3), new Point2D(0.3, 0.4));
        waitForShapeCount(2, testinfo);
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get("Road"), Matchers.equalTo(2),
                   saveScreenshot(testinfo));

        // Erasing a mask completely removes it.
        timeOutClickOn(robot, "#mask-eraser-button-icon", testinfo);
        mainView.getEditorImagePane().maskBrushSizeProperty().set(EditorImagePaneView.MAXIMUM_MASK_BRUSH_SIZE);
        paint(robot, new Point2D(0.3, 0.25), new Point2D(0.3, 0.45));
        waitForShapeCount(1, testinfo);
        timeOutClickOn(robot, "#mask-eraser-button-icon", testinfo);

        // Outside the mask mode, a mask can be dragged: it moves by the dragged distance, in whole pixels.
        timeOutClickOn(robot, "#rectangle-mode-button-icon", testinfo);
        final BoundingMaskView remaining = (BoundingMaskView) mainView.getCurrentBoundingShapes().getFirst();
        final int minXBefore = remaining.getMask().getMinX();
        final int minYBefore = remaining.getMask().getMinY();
        moveRelativeToImageView(robot, new Point2D(0.45, 0.5), new Point2D(0.55, 0.6));
        WaitForAsyncUtils.waitForFxEvents();
        final double imageWidth = remaining.getMask().getImageWidth();
        final double imageHeight = remaining.getMask().getImageHeight();
        verifyThat((double) remaining.getMask().getMinX() - minXBefore, Matchers.closeTo(0.1 * imageWidth,
                                                                                         imageWidth * 0.01),
                   saveScreenshot(testinfo));
        verifyThat((double) remaining.getMask().getMinY() - minYBefore, Matchers.closeTo(0.1 * imageHeight,
                                                                                         imageHeight * 0.01),
                   saveScreenshot(testinfo));
        verifyThat(remaining.getViewData().getNodeGroup().getTranslateX(), Matchers.equalTo(0.0),
                   saveScreenshot(testinfo));

        // The object tree shows the mask, and its data is saved with the image's annotation.
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        final List<BoundingShapeData> shapes = mainView.extractCurrentBoundingShapeData();
        verifyThat(shapes.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(shapes.getFirst(), Matchers.instanceOf(BoundingMaskData.class), saveScreenshot(testinfo));
    }

    @Test
    void onZoomingWhilePainting_ShouldKeepTheStrokeWhereItWasPainted(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Line", testinfo);
        timeOutClickOn(robot, "#mask-mode-button-icon", testinfo);
        robot.interact(() -> mainView.getEditorImagePane().maskBrushSizeProperty().set(10));

        // A horizontal stroke away from the image's center, zoomed in the middle of it (with the mouse button still
        // pressed). Depending on the platform's scroll direction, this zooms in or out; either way the image moves
        // under the pointer.
        final Point2D pointer = getScreenPointFromRatios(mainView.getEditorImageView(), new Point2D(0.4, 0.75));
        moveRelativeToImageViewNoRelease(robot, new Point2D(0.3, 0.75), new Point2D(0.4, 0.75));
        robot.press(KeyCode.SHORTCUT).scroll(10).release(KeyCode.SHORTCUT);
        WaitForAsyncUtils.waitForFxEvents();
        // The image row under the pointer now (the one the stroke continues on).
        final var imageViewLocal = mainView.getEditorImageView().screenToLocal(pointer);
        final double rowRatioAfterZoom =
                imageViewLocal.getY() / mainView.getEditorImageView().getLayoutBounds().getHeight();
        robot.moveBy(40, 0).release(javafx.scene.input.MouseButton.PRIMARY);
        waitForShapeCount(1, testinfo);

        // All painted pixels lie on the stroke's row before or after the zoom: there is no line from a point before
        // the zoom to another place in the image.
        final var mask = ((BoundingMaskView) mainView.getCurrentBoundingShapes().getFirst()).getMask();
        final double rowBefore = 0.75 * mask.getImageHeight();
        final double rowAfter = Math.clamp(rowRatioAfterZoom, 0, 1) * mask.getImageHeight();
        final double tolerance = 0.04 * mask.getImageHeight();

        for(int y = mask.getMinY(); y < mask.getMinY() + mask.getHeight(); ++y) {
            if(Math.abs(y - rowBefore) <= tolerance || Math.abs(y - rowAfter) <= tolerance) {
                continue;
            }

            for(int x = mask.getMinX(); x < mask.getMinX() + mask.getWidth(); ++x) {
                final int row = y;
                final int column = x;
                Assertions.assertFalse(mask.get(x, y), () -> saveScreenshotAndReturnMessage(testinfo,
                        "Pixel " + column + ", " + row + " is off the stroke's rows (" + rowBefore + " and "
                                + rowAfter + ")."));
            }
        }

        verifyThat((double) mask.getMinX(), Matchers.closeTo(0.3 * mask.getImageWidth(),
                                                             0.03 * mask.getImageWidth()), saveScreenshot(testinfo));
    }

    @Test
    void onZoomingOutWhileDraggingMask_ShouldKeepItWithinTheImage(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Buoy", testinfo);
        timeOutClickOn(robot, "#mask-mode-button-icon", testinfo);
        robot.interact(() -> mainView.getEditorImagePane().maskBrushSizeProperty().set(30));
        paint(robot, new Point2D(0.7, 0.5), new Point2D(0.8, 0.5));
        waitForShapeCount(1, testinfo);
        final BoundingMaskView mask = (BoundingMaskView) mainView.getCurrentBoundingShapes().getFirst();

        // Dragged to the right edge (where it is stopped), then zoomed out with the mouse button still pressed.
        timeOutClickOn(robot, "#rectangle-mode-button-icon", testinfo);
        moveRelativeToImageViewNoRelease(robot, new Point2D(0.75, 0.5), new Point2D(0.99, 0.5));
        robot.press(KeyCode.SHORTCUT).scroll(-10).release(KeyCode.SHORTCUT);
        WaitForAsyncUtils.waitForFxEvents();

        final var imageBounds = mainView.getEditorImageView().getBoundsInParent();
        final var draggedBounds = mask.getViewData().getNodeGroup().getBoundsInParent();
        verifyThat(draggedBounds.getMaxX(), Matchers.lessThanOrEqualTo(imageBounds.getMaxX() + 1),
                   saveScreenshot(testinfo));
        verifyThat(draggedBounds.getMinX(), Matchers.greaterThanOrEqualTo(imageBounds.getMinX() - 1),
                   saveScreenshot(testinfo));

        robot.release(javafx.scene.input.MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents();
        // The mask was moved to the image's right edge.
        verifyThat(mask.getMask().getMinX() + mask.getMask().getWidth(),
                   Matchers.equalTo(mask.getMask().getImageWidth()), saveScreenshot(testinfo));
        verifyThat(mask.getViewData().getNodeGroup().getTranslateX(), Matchers.equalTo(0.0),
                   saveScreenshot(testinfo));
    }

    @Test
    void onHoveringMaskTreeItem_ShouldShowThePreviewClippedToTheMask(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Mast", testinfo);
        timeOutClickOn(robot, "#mask-mode-button-icon", testinfo);
        robot.interact(() -> mainView.getEditorImagePane().maskBrushSizeProperty().set(20));
        // A diagonal stroke: its bounds' upper right and lower left corners are not part of the mask.
        paint(robot, new Point2D(0.2, 0.2), new Point2D(0.5, 0.5));
        waitForShapeCount(1, testinfo);
        final BoundingMaskView mask = (BoundingMaskView) mainView.getCurrentBoundingShapes().getFirst();

        final ObjectTreeElementCell cell = robot.lookup(node -> node instanceof ObjectTreeElementCell treeCell
                && mask.equals(treeCell.getItem())).query();
        robot.moveTo(cell);
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                        () -> cell.getPopOver().isShowing()),
                                      () -> saveScreenshotAndReturnMessage(testinfo, "The preview was not shown."));

        final var clip = cell.getPopOverImageView().getClip();
        verifyThat(clip, Matchers.instanceOf(javafx.scene.image.ImageView.class), saveScreenshot(testinfo));
        final var clipImage = ((javafx.scene.image.ImageView) clip).getImage();
        final var pixels = clipImage.getPixelReader();
        final int width = (int) clipImage.getWidth();
        final int height = (int) clipImage.getHeight();
        verifyThat(pixels.getArgb(width / 2, height / 2) >>> 24, Matchers.equalTo(0xFF), saveScreenshot(testinfo));
        verifyThat(pixels.getArgb(width - 1, 0) >>> 24, Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(pixels.getArgb(0, height - 1) >>> 24, Matchers.equalTo(0), saveScreenshot(testinfo));
        robot.moveTo(mainView.getEditorImageView());
    }

    private void paint(FxRobot robot, Point2D from, Point2D to) {
        moveRelativeToImageView(robot, from, to);
    }

    private List<Integer> currentMaskAreas() {
        return mainView.getCurrentBoundingShapes().stream()
                       .map(shape -> ((BoundingMaskView) shape).getMask().getArea())
                       .toList();
    }

    private void waitForShapeCount(int count, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                        () -> mainView.getCurrentBoundingShapes()
                                                                                      .size() == count),
                                      () -> saveScreenshotAndReturnMessage(testinfo, "Expected " + count
                                              + " masks, found " + mainView.getCurrentBoundingShapes().size() + "."));
    }
}
