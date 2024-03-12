/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationSaveStrategy;
import javafx.application.Platform;
import javafx.scene.image.Image;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testfx.api.FxAssert.verifyThat;

@Tag("ui")
class JpegExifDataTests extends BoundingBoxEditorTestBase {
    @Start
    void start(Stage stage) {
        super.onStart(stage);
        controller.loadImageFiles(new File(getClass().getResource(TEST_EXIF_IMAGE_FOLDER_PATH).getFile()));
    }

    @Test
    void onLoadJpegsWithExifData_ShouldCorrectlyHandleOrientation(TestInfo testInfo, FxRobot robot, @TempDir Path tempDir) throws IOException {
        waitUntilCurrentImageIsLoaded(testInfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo("0.jpg"));

        final Image reference = mainView.getCurrentImage();
        verifyThat(reference.getUrl(), Matchers.endsWith("0.jpg"), saveScreenshot(testInfo));

        for(int i = 1; i <= 8; ++i) {
            loadNextImageAndCompareToReference(i + ".jpg", reference, testInfo, robot);
        }

        Platform.runLater(() -> controller
                .initiateAnnotationImport(new File(getClass().getResource("/testannotations/exif/json/annotations.json").getFile()),
                        ImageAnnotationLoadStrategy.Type.JSON));
        WaitForAsyncUtils.waitForFxEvents();
        timeOutAssertServiceSucceeded(controller.getAnnotationImportService(), testInfo);

        final Path jsonOutputDir = Files.createDirectory(tempDir.resolve("json"));
        final Path jsonReferenceDir = new File(getClass().getResource("/testannotations/exif/json").getFile()).toPath();
        exportAnnotationsAndCompareWithReference(ImageAnnotationSaveStrategy.Type.JSON, jsonOutputDir, jsonReferenceDir, testInfo);

        final Path pvocOutputDir = Files.createDirectory(tempDir.resolve("pvoc"));
        final Path pvocReferenceDir = new File(getClass().getResource("/testannotations/exif/pvoc").getFile()).toPath();
        exportAnnotationsAndCompareWithReference(ImageAnnotationSaveStrategy.Type.PASCAL_VOC, pvocOutputDir, pvocReferenceDir, testInfo);

        final Path yoloOutputDir = Files.createDirectory(tempDir.resolve("yolo"));
        final Path yoloReferenceDir = new File(getClass().getResource("/testannotations/exif/yolo").getFile()).toPath();
        exportAnnotationsAndCompareWithReference(ImageAnnotationSaveStrategy.Type.YOLO, yoloOutputDir, yoloReferenceDir, testInfo);
    }

    private void loadNextImageAndCompareToReference(String expectedFilename, Image referenceImage, TestInfo testInfo, FxRobot robot) {
        timeOutClickOn(robot, "#next-button", testInfo);

        waitUntilCurrentImageIsLoaded(testInfo);
        WaitForAsyncUtils.waitForFxEvents();

        verifyThat(model.getCurrentImageFileName(), Matchers.equalTo(expectedFilename), saveScreenshot(testInfo));
        verifyThat(compareImages(mainView.getCurrentImage(), referenceImage), Matchers.is(true), saveScreenshot(testInfo));
    }

    private void exportAnnotationsAndCompareWithReference(ImageAnnotationSaveStrategy.Type strategyType, Path outputFolder, Path referenceFolder, TestInfo testInfo) throws IOException {
        Path outputPath = strategyType == ImageAnnotationSaveStrategy.Type.JSON ? outputFolder.resolve("annotations.json") : outputFolder;

        Platform.runLater(
                () -> controller.initiateAnnotationExport(outputPath.toFile(), strategyType));
        WaitForAsyncUtils.waitForFxEvents();

        timeOutAssertServiceSucceeded(controller.getAnnotationExportService(), testInfo);

        try(final Stream<Path> referenceFilePaths = Files.list(referenceFolder)) {
            final List<Path> referenceFiles = referenceFilePaths.toList();

            Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                            () -> referenceFiles.stream().allMatch(referenceFilePath -> Files.exists(outputFolder.resolve(referenceFilePath.getFileName())))),
                    () -> saveScreenshotAndReturnMessage(testInfo,
                            "Expected output-files were not created within " +
                                    TIMEOUT_DURATION_IN_SEC + " sec."));

            final Map<Path, byte[]> referencePathToBytesMap = referenceFiles.stream().collect(Collectors.toMap(Function.identity(), filePath -> {
                try {
                    return Files.readAllBytes(filePath);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }));

            Assertions.assertDoesNotThrow(() -> WaitForAsyncUtils.waitFor(TIMEOUT_DURATION_IN_SEC, TimeUnit.SECONDS,
                            () -> referencePathToBytesMap.entrySet().stream().allMatch(entry -> {
                                try {
                                    return Arrays.equals(entry.getValue(), Files.readAllBytes(outputFolder.resolve(entry.getKey().getFileName())));
                                } catch(IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })),
                    () -> saveScreenshotAndReturnMessage(testInfo,
                            "Expected annotation output-files " +
                                    "content were not created within " +
                                    TIMEOUT_DURATION_IN_SEC + " sec."));
        }
    }
}
