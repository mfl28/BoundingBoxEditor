/*
 * Copyright (C) 2021 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import javafx.geometry.Point2D;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TableViewMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class BoundingBoxDrawingTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onOpeningNewImageFolder_WhenBoundingBoxesExist_ShouldResetCorrectly(FxRobot robot, TestInfo testinfo) {
        waitUntilCurrentImageIsLoaded(testinfo);

        verifyDragAnchorFunctionality(testinfo);
        verifyThat(model.getImageFileNameToMetaDataMap().size(), Matchers.equalTo(4), saveScreenshot(testinfo));
        verifyThat(model.getImageFileNameToMetaDataMap()
                        .entrySet()
                        .stream()
                        .filter(entry -> !entry.getKey()
                                               .equals(model.getCurrentImageFileName()))
                        .allMatch(entry -> entry.getValue().hasDetails()), Matchers.is(true), saveScreenshot(testinfo));

        String testCategoryName = "Test";
        enterNewCategory(robot, testCategoryName, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));

        // Draw a bounding box.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        int drawnBoundingBoxFileIndex = model.getCurrentFileIndex();

        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                                                                      () -> mainView.getCurrentBoundingShapes()
                                                                                    .size() == 1),
                                      () -> saveScreenshotAndReturnMessage(testinfo,
                                                                           "Expected number of bounding boxes not" +
                                                                                   " found in " +
                                                                                   TIMEOUT_DURATION_IN_SEC +
                                                                                   " sec."));

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class),
                   saveScreenshot(testinfo));
        final BoundingBoxView drawnBoundingBox = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);
        verifyThat(model.isSaved(), Matchers.is(true), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#previous-button", testinfo);
        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(false),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getItems().get(drawnBoundingBoxFileIndex)
                           .isHasAssignedBoundingShapes(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#next-button", testinfo);

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasItem(drawnBoundingBox), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));
        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getSelectionModel()
                           .getSelectedItem().isHasAssignedBoundingShapes(), Matchers.is(true),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class),
                   saveScreenshot(testinfo));

        final BoundingBoxView boundingBoxView = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);

        // Move bounding box to top left corner.
        double boundingBoxWidth = boundingBoxView.getWidth();
        double boundingBoxHeight = boundingBoxView.getHeight();
        moveRelativeToImageView(robot, new Point2D(0.5, 0.5), new Point2D(0.0, 0.0));
        WaitForAsyncUtils.waitForFxEvents();

        Point2D imageViewRelativePointTopLeft = getParentPointFromImageViewRatios(new Point2D(0.0, 0.0));
        verifyThat(boundingBoxView.getX(),
                   Matchers.closeTo(imageViewRelativePointTopLeft.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getY(),
                   Matchers.closeTo(imageViewRelativePointTopLeft.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getWidth(), Matchers.closeTo(boundingBoxWidth, MathUtils.DOUBLE_EQUAL_THRESHOLD),
                   saveScreenshot(testinfo));
        verifyThat(boundingBoxView.getHeight(), Matchers.closeTo(boundingBoxHeight, MathUtils.DOUBLE_EQUAL_THRESHOLD),
                   saveScreenshot(testinfo));

        // Move bounding box to bottom right corner.
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(1.0, 1.0));
        WaitForAsyncUtils.waitForFxEvents();

        Point2D imageViewRelativePointBottomRight = getParentPointFromImageViewRatios(new Point2D(1.0, 1.0));
        verifyThat(boundingBoxView.getX() + boundingBoxView.getWidth(),
                   Matchers.closeTo(imageViewRelativePointBottomRight.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getY() + boundingBoxView.getHeight(),
                   Matchers.closeTo(imageViewRelativePointBottomRight.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView.getWidth(), Matchers.closeTo(boundingBoxWidth, MathUtils.DOUBLE_EQUAL_THRESHOLD),
                   saveScreenshot(testinfo));
        verifyThat(boundingBoxView.getHeight(), Matchers.closeTo(boundingBoxHeight, MathUtils.DOUBLE_EQUAL_THRESHOLD),
                   saveScreenshot(testinfo));

        loadImageFolderAndClickKeepCategoriesAndSaveAnnotationOptions(robot, TEST_IMAGE_FOLDER_PATH_2, "No", "No",
                                                                      testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Image loading error report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText = "1 image file could not be loaded.";
        final DialogPane errorReportDialog = (DialogPane) errorReportStage.getScene().getRoot();
        verifyThat(errorReportDialog.getContentText(), Matchers.equalTo(errorReportDialogContentReferenceText),
                   saveScreenshot(testinfo));

        verifyThat(errorReportDialog.getExpandableContent(), Matchers.instanceOf(GridPane.class),
                   saveScreenshot(testinfo));
        verifyThat(((GridPane) errorReportDialog.getExpandableContent()).getChildren().get(0),
                   Matchers.instanceOf(TableView.class), saveScreenshot(testinfo));
        final GridPane errorReportDialogContentPane = (GridPane) errorReportDialog.getExpandableContent();

        verifyThat(errorReportDialogContentPane.getChildren().get(0), Matchers.instanceOf(TableView.class),
                   saveScreenshot(testinfo));

        @SuppressWarnings("unchecked") final TableView<IOErrorInfoEntry> errorInfoTable =
                (TableView<IOErrorInfoEntry>) errorReportDialogContentPane.getChildren().get(0);

        final List<IOErrorInfoEntry> errorInfoEntries = errorInfoTable.getItems();

        verifyThat(errorInfoEntries, Matchers.hasSize(1), saveScreenshot(testinfo));

        final IOErrorInfoEntry referenceErrorInfoEntry1 = new IOErrorInfoEntry("no_image_file.txt",
                                                                               "Invalid or unsupported image file.");

        verifyThat(errorInfoEntries, Matchers.contains(referenceErrorInfoEntry1), saveScreenshot(testinfo));

        timeOutClickOnButtonInDialogStage(robot, errorReportStage, ButtonType.OK, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat("#category-selector", TableViewMatchers.hasNumRows(0), saveScreenshot(testinfo));
        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(0), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().size(), Matchers.equalTo(0),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getImageFileListView().getItems(), Matchers.hasSize(5));

        verifyThat(mainView.getImageFileListView().getItems()
                           .stream().noneMatch(ImageFileListView.FileInfo::isHasAssignedBoundingShapes),
                   Matchers.is(true));

        enterNewCategory(robot, testCategoryName, testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(0),
                   saveScreenshot(testinfo));
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.75, 0.75));
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(mainView.getCurrentBoundingShapes().size(), Matchers.equalTo(1), saveScreenshot(testinfo));
        verifyThat(model.getCategoryToAssignedBoundingShapesCountMap().get(testCategoryName), Matchers.equalTo(1),
                   saveScreenshot(testinfo));

        verifyThat(mainView.getCurrentBoundingShapes().get(0), Matchers.instanceOf(BoundingBoxView.class),
                   saveScreenshot(testinfo));

        final BoundingBoxView boundingBoxView3 = (BoundingBoxView) mainView.getCurrentBoundingShapes().get(0);
        double preResizeMaxX = boundingBoxView3.getX() + boundingBoxView3.getWidth();
        double preResizeMaxY = boundingBoxView3.getY() + boundingBoxView3.getHeight();

        // Resize test:
        moveRelativeToImageView(robot, new Point2D(0.25, 0.25), new Point2D(0.0, 0.0));
        WaitForAsyncUtils.waitForFxEvents();

        imageViewRelativePointTopLeft = getParentPointFromImageViewRatios(new Point2D(0.0, 0.0));
        verifyThat(boundingBoxView3.getX(),
                   Matchers.closeTo(imageViewRelativePointTopLeft.getX(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getY(),
                   Matchers.closeTo(imageViewRelativePointTopLeft.getY(), MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getX() + boundingBoxView3.getWidth(),
                   Matchers.closeTo(preResizeMaxX, MathUtils.DOUBLE_EQUAL_THRESHOLD));
        verifyThat(boundingBoxView3.getY() + boundingBoxView3.getHeight(),
                   Matchers.closeTo(preResizeMaxY, MathUtils.DOUBLE_EQUAL_THRESHOLD));

        // Try to exit application:
        final Stage exitDialogStage = tryExitAndGetDialog(robot, testinfo);

        timeOutLookUpInStageAndClickOn(robot, exitDialogStage, "Cancel", testinfo);

        timeOutAssertTopModalStageClosed(robot, "Exit Application", testinfo);

        // Try exit and check save dialog
        final Stage exitDialogStage2 = tryExitAndGetDialog(robot, testinfo);

        timeOutLookUpInStageAndClickOn(robot, exitDialogStage2, "Yes", testinfo);

        final Stage saveAnnotationsStage = timeOutGetTopModalStage(robot, "Save Annotations", testinfo);

        timeOutLookUpInStageAndClickOn(robot, saveAnnotationsStage, "Cancel", testinfo);

        verifyThat(model.isSaved(), Matchers.is(false), saveScreenshot(testinfo));
    }

    private void verifyDragAnchorFunctionality(TestInfo testinfo) {
        DragAnchor dragAnchor = new DragAnchor();

        dragAnchor.setCoordinates(1.0, 2.0);
        verifyThat(dragAnchor.getX(), Matchers.closeTo(1.0, RATIO_EQUAL_THRESHOLD), saveScreenshot(testinfo));
        verifyThat(dragAnchor.getY(), Matchers.closeTo(2.0, RATIO_EQUAL_THRESHOLD), saveScreenshot(testinfo));

        dragAnchor.setFromMouseEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 32.0, 60.0, 100.0, 200.0,
                                                    MouseButton.PRIMARY, 1, false, false, false, false, false, false,
                                                    false, false, false, false, null));
        verifyThat(dragAnchor.getX(), Matchers.closeTo(32.0, RATIO_EQUAL_THRESHOLD), saveScreenshot(testinfo));
        verifyThat(dragAnchor.getY(), Matchers.closeTo(60.0, RATIO_EQUAL_THRESHOLD), saveScreenshot(testinfo));
    }

    private Stage tryExitAndGetDialog(FxRobot robot, TestInfo testinfo) {
        timeOutClickOn(robot, "File", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "Exit", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        return timeOutGetTopModalStage(robot, "Exit Application", testinfo);
    }
}
