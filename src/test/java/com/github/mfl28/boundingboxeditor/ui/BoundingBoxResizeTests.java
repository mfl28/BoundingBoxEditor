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
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
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
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Resizes a box with each of its eight handles.
 */
@Tag("ui")
class BoundingBoxResizeTests extends BoundingBoxEditorTestBase {
    // The box covers 0.3 to 0.6 of the image in both directions; each handle is dragged outward by 0.1.
    private static final double BOX_MIN = 0.3;
    private static final double BOX_MAX = 0.6;
    private static final double DELTA = 0.1;
    private static final double TOLERANCE = 1.5;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onDraggingEachResizeHandle_ShouldMoveOnlyItsEdges(FxRobot robot, TestInfo testinfo) {
        final BoundingBoxView box = drawBox(robot, testinfo);

        // {handle x, handle y} as fractions of the box (0 = left/top, 0.5 = middle, 1 = right/bottom), and the
        // direction in which each of the box's edges (minX, minY, maxX, maxY) moves: -1 outward to the left/top, +1
        // outward to the right/bottom, 0 not at all.
        final double[][] handles = {
                {0, 0, -1, -1, 0, 0},     // NW
                {0.5, 0, 0, -1, 0, 0},    // N
                {1, 0, 0, -1, 1, 0},      // NE
                {1, 0.5, 0, 0, 1, 0},     // E
                {1, 1, 0, 0, 1, 1},       // SE
                {0.5, 1, 0, 0, 0, 1},     // S
                {0, 1, -1, 0, 0, 1},      // SW
                {0, 0.5, -1, 0, 0, 0}     // W
        };

        for(double[] handle : handles) {
            resetBox(robot, box);
            final Bounds image = mainView.getEditorImageView().getBoundsInParent();
            final double[] before = {box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight()};
            final double dx = DELTA * image.getWidth();
            final double dy = DELTA * image.getHeight();
            // Outward: towards the side of the handle.
            final double moveX = handle[0] == 0 ? -dx : handle[0] == 1 ? dx : 0;
            final double moveY = handle[1] == 0 ? -dy : handle[1] == 1 ? dy : 0;

            dragHandle(robot, box, handle[0], handle[1], moveX, moveY);

            final double[] after = {box.getX(), box.getY(), box.getX() + box.getWidth(), box.getY() + box.getHeight()};
            final double[] expected = {before[0] + (handle[2] != 0 ? moveX : 0), before[1] + (handle[3] != 0 ? moveY : 0),
                    before[2] + (handle[4] != 0 ? moveX : 0), before[3] + (handle[5] != 0 ? moveY : 0)};
            final String handleName = "handle at (" + handle[0] + ", " + handle[1] + ")";

            for(int i = 0; i < 4; ++i) {
                final int edge = i;
                Assertions.assertEquals(expected[i], after[i], TOLERANCE,
                                        () -> saveScreenshotAndReturnMessage(testinfo, handleName + ", edge " + edge
                                                + ": expected " + expected[edge] + " but was " + after[edge]));
            }
        }
    }

    @Test
    void onDraggingResizeHandlesBeyondTheImage_ShouldStopAtItsEdges(FxRobot robot, TestInfo testinfo) {
        final BoundingBoxView box = drawBox(robot, testinfo);
        final Bounds image = mainView.getEditorImageView().getBoundsInParent();

        // The lower right corner far beyond the image's lower right corner.
        dragHandleTo(robot, box, 1, 1, image.getMaxX() + 40, image.getMaxY() + 40);
        verifyThat(box.getX() + box.getWidth(), Matchers.closeTo(image.getMaxX(), TOLERANCE), saveScreenshot(testinfo));
        verifyThat(box.getY() + box.getHeight(), Matchers.closeTo(image.getMaxY(), TOLERANCE),
                   saveScreenshot(testinfo));

        // The upper left corner far beyond the image's upper left corner.
        dragHandleTo(robot, box, 0, 0, image.getMinX() - 40, image.getMinY() - 40);
        verifyThat(box.getX(), Matchers.closeTo(image.getMinX(), TOLERANCE), saveScreenshot(testinfo));
        verifyThat(box.getY(), Matchers.closeTo(image.getMinY(), TOLERANCE), saveScreenshot(testinfo));

        // An edge handle can't move its edge past the opposite edge.
        resetBox(robot, box);
        final double minY = box.getY();
        dragHandleTo(robot, box, 0.5, 1, box.getX() + box.getWidth() / 2, minY - 50);
        verifyThat(box.getY(), Matchers.closeTo(minY, TOLERANCE), saveScreenshot(testinfo));
        verifyThat(box.getHeight(), Matchers.lessThan(TOLERANCE), saveScreenshot(testinfo));
    }

    private BoundingBoxView drawBox(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, "Box", testinfo);
        moveRelativeToImageView(robot, new Point2D(BOX_MIN, BOX_MIN), new Point2D(BOX_MAX, BOX_MAX));
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                        () -> mainView.getCurrentBoundingShapes()
                                                                                      .size() == 1),
                                      () -> saveScreenshotAndReturnMessage(testinfo, "The box was not drawn."));
        final BoundingBoxView box = (BoundingBoxView) mainView.getCurrentBoundingShapes().getFirst();
        verifyThat(box.isSelected(), Matchers.is(true), saveScreenshot(testinfo));
        return box;
    }

    private void resetBox(FxRobot robot, BoundingBoxView box) {
        robot.interact(() -> {
            final Bounds image = mainView.getEditorImageView().getBoundsInParent();
            box.setX(image.getMinX() + BOX_MIN * image.getWidth());
            box.setY(image.getMinY() + BOX_MIN * image.getHeight());
            box.setWidth((BOX_MAX - BOX_MIN) * image.getWidth());
            box.setHeight((BOX_MAX - BOX_MIN) * image.getHeight());
        });
    }

    private void dragHandle(FxRobot robot, BoundingBoxView box, double fractionX, double fractionY, double moveX,
                            double moveY) {
        final double handleX = box.getX() + fractionX * box.getWidth();
        final double handleY = box.getY() + fractionY * box.getHeight();
        dragHandleTo(robot, box, fractionX, fractionY, handleX + moveX, handleY + moveY);
    }

    /**
     * Drags the handle at a position of the box (as fractions of its size) to a point in the box's coordinate system.
     */
    private void dragHandleTo(FxRobot robot, BoundingBoxView box, double fractionX, double fractionY, double toX,
                              double toY) {
        final Point2D handle = box.localToScreen(box.getX() + fractionX * box.getWidth(),
                                                 box.getY() + fractionY * box.getHeight());
        final Point2D target = box.localToScreen(toX, toY);
        robot.moveTo(handle).press(MouseButton.PRIMARY).moveTo(target).release(MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
