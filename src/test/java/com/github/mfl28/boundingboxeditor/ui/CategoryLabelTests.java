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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
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
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class CategoryLabelTests extends BoundingBoxEditorTestBase {
    private static final String CATEGORY_NAME = "Sailboat";

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onShowCategoryLabelsSetting_ShouldShowTheCategoryNameAtEachVisibleShape(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);
        enterNewCategory(robot, CATEGORY_NAME, testinfo);
        moveRelativeToImageView(robot, new Point2D(0.2, 0.3), new Point2D(0.5, 0.6));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 1, "drawn box", testinfo);

        final BoundingShapeViewData viewData = mainView.getCurrentBoundingShapes().getFirst().getViewData();
        final Label label = viewData.getCategoryLabel();
        verifyThat(label.isVisible(), Matchers.is(false), saveScreenshot(testinfo));

        // Switched on in the settings.
        timeOutClickOn(robot, "#file-menu", testinfo);
        timeOutClickOn(robot, "#file-settings-menu-item", testinfo);
        final Stage settingsStage = timeOutGetTopModalStage(robot, "Settings", testinfo);
        final CheckBox showCategoryLabelsControl = mainView.getEditorSettingsView().getShowCategoryLabelsControl();
        verifyThat(showCategoryLabelsControl.isSelected(), Matchers.is(false), saveScreenshot(testinfo));
        robot.clickOn(showCategoryLabelsControl);
        timeOutClickOnButtonInDialogStage(robot, settingsStage, ButtonType.OK, testinfo);
        timeOutAssertNoTopModelStage(robot, testinfo);

        verifyThat(mainView.getEditorSettingsConfig().isShowCategoryLabels(), Matchers.is(true),
                   saveScreenshot(testinfo));
        verifyThat(label.isVisible(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(label.getText(), Matchers.equalTo(CATEGORY_NAME), saveScreenshot(testinfo));
        verifyThat(label.getStyle(), Matchers.containsString("-fx-background-color: rgba("), saveScreenshot(testinfo));

        // Just above the box's left end, and within the image.
        final Bounds shapeBounds = viewData.getBaseShape().getBoundsInParent();
        final Bounds labelBounds = label.getBoundsInParent();
        final Bounds imageBounds = mainView.getEditorImageView().getBoundsInParent();
        verifyThat(labelBounds.getMaxY(), Matchers.closeTo(shapeBounds.getMinY(), 1), saveScreenshot(testinfo));
        verifyThat(labelBounds.getMinX(), Matchers.closeTo(shapeBounds.getMinX(), 1), saveScreenshot(testinfo));
        verifyThat(imageBounds.contains(labelBounds), Matchers.is(true), saveScreenshot(testinfo));

        // Hidden together with its shape.
        robot.interact(() -> mainView.getObjectTree().setToggleIconStateForAllTreeItems(false));
        verifyThat(label.isVisible(), Matchers.is(false), saveScreenshot(testinfo));
        robot.interact(() -> mainView.getObjectTree().setToggleIconStateForAllTreeItems(true));
        verifyThat(label.isVisible(), Matchers.is(true), saveScreenshot(testinfo));

        // A shape drawn afterwards gets a label, too.
        moveRelativeToImageView(robot, new Point2D(0.99, 0.0), new Point2D(1.0, 0.2));
        waitUntil(() -> mainView.getCurrentBoundingShapes().size() == 2, "second box", testinfo);
        final BoundingShapeViewData secondViewData = mainView.getCurrentBoundingShapes().get(1).getViewData();
        verifyThat(secondViewData.getCategoryLabel().isVisible(), Matchers.is(true), saveScreenshot(testinfo));
        // At the image's upper edge there is no room above the box, so the label is inside it, and at the right
        // edge it is moved to the left to stay within the image.
        final Bounds secondLabelBounds = secondViewData.getCategoryLabel().getBoundsInParent();
        verifyThat(secondLabelBounds.getMinY(),
                   Matchers.closeTo(secondViewData.getBaseShape().getBoundsInParent().getMinY(), 1),
                   saveScreenshot(testinfo));
        verifyThat(secondLabelBounds.getMaxX(), Matchers.lessThanOrEqualTo(imageBounds.getMaxX() + 1e-6),
                   saveScreenshot(testinfo));
        verifyThat(secondLabelBounds.getMinX(),
                   Matchers.lessThan(secondViewData.getBaseShape().getBoundsInParent().getMinX()),
                   saveScreenshot(testinfo));
    }

    private void waitUntil(Callable<Boolean> condition, String description, TestInfo testinfo) {
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      condition),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected state not reached: " +
                                                                                   description));
    }
}
