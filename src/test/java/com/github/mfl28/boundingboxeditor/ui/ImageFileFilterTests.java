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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.testfx.api.FxAssert.verifyThat;

/**
 * Tests filtering the image file list and navigating within the filtered list.
 */
@Tag("ui")
class ImageFileFilterTests extends BoundingBoxEditorTestBase {
    private static final String AUSTIN = "austin-neill-685084-unsplash.jpg";
    private static final String CALEB = "caleb-george-316073-unsplash.jpg";
    private static final String NICO = "nico-bhlr-1067059-unsplash.jpg";
    private static final String TYLER = "tyler-nix-582593-unsplash.jpg";

    @TempDir
    Path tempDir;

    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_IMAGE_FOLDER_PATH_1).getFile()));
    }

    @Test
    void onFilterChanges_ShouldShowMatchingImagesAndNavigateWithinThem(FxRobot robot, TestInfo testinfo)
            throws IOException {
        waitUntilCurrentImageIsLoaded(testinfo);
        importAnnotations(testinfo);

        final ImageFileExplorerView explorer = mainView.getImageFileExplorer();
        final ImageFileFilterView filterView = explorer.getImageFileFilterView();

        verifyThat(shownFileNames(), Matchers.contains(AUSTIN, CALEB, NICO, TYLER), saveScreenshot(testinfo));
        verifyThat(explorer.getFilterMatchLabel().isVisible(), Matchers.is(false), saveScreenshot(testinfo));

        // Annotated images: the unannotated one is skipped when navigating.
        timeOutClickOn(robot, "#image-file-filter-button", testinfo);
        timeOutClickOn(robot, "#image-file-filter-status-annotated", testinfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(shownFileNames(), Matchers.contains(AUSTIN, NICO, TYLER), saveScreenshot(testinfo));
        verifyThat(explorer.getFilterMatchLabel().isVisible(), Matchers.is(true), saveScreenshot(testinfo));
        verifyThat(explorer.getFilterMatchLabel().getText(), Matchers.equalTo("3 of 4 images match"),
                   saveScreenshot(testinfo));
        verifyThat(indexLabelText(), Matchers.equalTo("1 | 3"), saveScreenshot(testinfo));

        // The first click outside the filter popup only closes it.
        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();
        timeOutClickOn(robot, "#next-button", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(NICO), saveScreenshot(testinfo));
        verifyThat(indexLabelText(), Matchers.equalTo("2 | 3"), saveScreenshot(testinfo));

        Platform.runLater(controller::onRegisterNextImageFileRequested);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(TYLER), saveScreenshot(testinfo));
        verifyThat(mainView.getNextImageNavigationButton().isDisabled(), Matchers.is(true), saveScreenshot(testinfo));

        // Not annotated: the current image doesn't match, so the only matching image is shown instead.
        timeOutClickOn(robot, "#image-file-filter-button", testinfo);
        timeOutClickOn(robot, "#image-file-filter-status-not-annotated", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(CALEB), saveScreenshot(testinfo));
        verifyThat(shownFileNames(), Matchers.contains(CALEB), saveScreenshot(testinfo));
        verifyThat(indexLabelText(), Matchers.equalTo("1 | 1"), saveScreenshot(testinfo));

        // Categories, matching any or all of them.
        timeOutClickOn(robot, "#image-file-filter-status-all", testinfo);
        checkCategory(filterView, "Flag");
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(NICO), saveScreenshot(testinfo));
        verifyThat(shownFileNames(), Matchers.contains(NICO, TYLER), saveScreenshot(testinfo));

        checkCategory(filterView, "Boat");
        verifyThat(shownFileNames(), Matchers.contains(AUSTIN, NICO, TYLER), saveScreenshot(testinfo));

        timeOutClickOn(robot, "#image-file-filter-category-all", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(shownFileNames(), Matchers.contains(NICO), saveScreenshot(testinfo));

        // Clearing the filter shows all images again, the current image stays.
        timeOutClickOn(robot, "#image-file-filter-clear-button", testinfo);
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(shownFileNames(), Matchers.contains(AUSTIN, CALEB, NICO, TYLER), saveScreenshot(testinfo));
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(NICO), saveScreenshot(testinfo));
        verifyThat(indexLabelText(), Matchers.equalTo("3 | 4"), saveScreenshot(testinfo));
        verifyThat(explorer.getFilterMatchLabel().isVisible(), Matchers.is(false), saveScreenshot(testinfo));

        // The search field filters by file name.
        robot.push(KeyCode.ESCAPE);
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn(explorer.getImageFileSearchField()).write("TYL");
        WaitForAsyncUtils.waitForFxEvents();
        verifyThat(shownFileNames(), Matchers.contains(TYLER), saveScreenshot(testinfo));
        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(TYLER), saveScreenshot(testinfo));
    }

    private void importAnnotations(TestInfo testinfo) throws IOException {
        final Path annotationFile = tempDir.resolve("annotations.json");
        Files.writeString(annotationFile, "[" + String.join(",",
                annotation(AUSTIN, "Boat"), annotation(NICO, "Boat", "Flag"), annotation(TYLER, "Flag")) + "]");

        Platform.runLater(() -> controller.initiateAnnotationImport(annotationFile.toFile(),
                                                                    ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();
        Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                        () -> model.getImageFileNameToAnnotationMap().size() == 3),
                () -> saveScreenshotAndReturnMessage(testinfo, "Annotations were not imported."));
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static String annotation(String fileName, String... categories) {
        final List<String> objects = Arrays.stream(categories)
                .map(category -> "{\"bndbox\":{\"minX\":0.1,\"minY\":0.1,\"maxX\":0.4,\"maxY\":0.4},"
                        + "\"category\":{\"name\":\"" + category + "\",\"color\":\"#C31D8F\"},\"tags\":[]}")
                .toList();

        return "{\"image\":{\"fileName\":\"" + fileName + "\"},\"objects\":[" + String.join(",", objects) + "]}";
    }

    private void checkCategory(ImageFileFilterView filterView, String categoryName) {
        Platform.runLater(() -> {
            final ObjectCategory category = model.getObjectCategories().stream()
                                                 .filter(item -> Objects.equals(item.getName(), categoryName))
                                                 .findFirst().orElseThrow();
            filterView.getCategoryList().getCheckModel().check(category);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private List<String> shownFileNames() {
        return mainView.getImageFileListView().getItems().stream().map(ImageFileListView.FileInfo::getFileName)
                       .toList();
    }

    private String indexLabelText() {
        return mainView.getEditor().getEditorToolBar().getIndexLabel().getText();
    }
}
