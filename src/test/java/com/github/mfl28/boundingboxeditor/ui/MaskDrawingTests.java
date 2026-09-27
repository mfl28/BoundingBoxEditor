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

        // The object tree shows the mask, and its data is saved with the image's annotation.
        verifyThat(mainView.getObjectTree().getRoot().getChildren().size(), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        final List<BoundingShapeData> shapes = mainView.extractCurrentBoundingShapeData();
        verifyThat(shapes.size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(shapes.getFirst(), Matchers.instanceOf(BoundingMaskData.class), saveScreenshot(testinfo));
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
