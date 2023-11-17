/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.controller;

import com.github.mfl28.boundingboxeditor.BoundingBoxEditorTestBase;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class AnnotationTests extends BoundingBoxEditorTestBase {

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_4).getFile()));
    }
    @Test
    void onLoadAnnotation_YOLO_WhenAnnotationAssociationsProblemsPresent_ShouldNotLoadBoundingBoxes(FxRobot robot,
                                                                                                  TestInfo testinfo) {
        final String inputPath = "/testannotations/yolo/association-annotations";

        waitUntilCurrentImageIsLoaded(testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getImageMetaDataLoadingService(), testinfo);

        verifyThat(mainView.getStatusBar().getCurrentEventMessage(),
                Matchers.startsWith("Successfully loaded 2 image-files from folder "), saveScreenshot(testinfo));

        final File inputFile = new File(getClass().getResource(inputPath).getFile());

        // Load bounding-boxes defined in annotation-file.
        Platform.runLater(() -> controller.initiateAnnotationImport(inputFile, ImageAnnotationLoadStrategy.Type.YOLO));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testinfo);

        final Stage errorReportStage = timeOutGetTopModalStage(robot, "Annotation Import Error Report", testinfo);
        verifyThat(errorReportStage, Matchers.notNullValue(), saveScreenshot(testinfo));

        final String errorReportDialogContentReferenceText =
                "The source does not contain any valid annotations.";
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

        verifyThat(errorInfoEntries, Matchers.hasSize(2), saveScreenshot(testinfo));

        final IOErrorInfoEntry referenceErrorInfoEntry1 =
                new IOErrorInfoEntry("no_associated_file.txt",
                        "No associated image file.");

        final IOErrorInfoEntry referenceErrorInfoEntry2 =
                new IOErrorInfoEntry("test.txt",
                        "More than one associated image file.");

        verifyThat(errorInfoEntries, Matchers.containsInAnyOrder(
                referenceErrorInfoEntry1, referenceErrorInfoEntry2
                ), saveScreenshot(testinfo));

        WaitForAsyncUtils.waitForFxEvents();

        // Close error report dialog.
        timeOutLookUpInStageAndClickOn(robot, errorReportStage, "OK", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertTopModalStageClosed(robot, "Annotation Import Error Report", testinfo);

        verifyThat(mainView.getCurrentBoundingShapes(), Matchers.hasSize(0), saveScreenshot(testinfo));
        verifyThat(model.createImageAnnotationData().imageAnnotations(), Matchers.hasSize(0),
                saveScreenshot(testinfo));
    }
}
