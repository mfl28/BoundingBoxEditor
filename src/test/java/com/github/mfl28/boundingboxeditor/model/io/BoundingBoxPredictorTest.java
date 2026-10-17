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
package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests turning a server's predictions into bounding boxes, with a fake client. Resizing is switched off, so the
 * image file is sent as it is and its size comes from the metadata (no JavaFX toolkit needed).
 */
@Tag("unit")
class BoundingBoxPredictorTest {
    private static final double IMAGE_WIDTH = 200;
    private static final double IMAGE_HEIGHT = 100;

    @TempDir
    Path tempDir;

    @Test
    void onPrediction_ShouldClampBoxesToTheImageAndSkipInvalidOnes() throws Exception {
        final List<BoundingShapeData> shapes = predict(List.of(
                entry("inside", 0.9, 20.0, 10.0, 100.0, 50.0),
                entry("partlyOutside", 0.9, -10.0, 50.0, 250.0, 120.0),
                entry("fullyOutside", 0.9, 300.0, 10.0, 400.0, 50.0),
                entry("tooFewCoordinates", 0.9, 1.0, 2.0, 3.0),
                entry("notFinite", 0.9, Double.NaN, 2.0, 3.0, 4.0),
                new BoundingBoxPredictionEntry(Map.of(), 0.9),
                entry("lowScore", 0.1, 20.0, 10.0, 100.0, 50.0)),
                errors -> {
                    assertEquals(2, errors.size(), errors::toString);
                    assertTrue(errors.stream().allMatch(error -> error.getSourceName().equals("FakeServe")));
                    assertTrue(errors.get(0).getErrorDescription().startsWith("3 predicted object(s)"),
                               errors.get(0).getErrorDescription());
                    assertTrue(errors.get(1).getErrorDescription().startsWith("1 predicted box(es)"),
                               errors.get(1).getErrorDescription());
                });

        assertEquals(List.of("inside", "partlyOutside"), shapes.stream().map(BoundingShapeData::getCategoryName).toList());
        assertBounds(new BoundingBox(0.1, 0.1, 0.4, 0.4), shapes.get(0));
        assertBounds(new BoundingBox(0, 0.5, 1, 0.5), shapes.get(1));
    }

    @Test
    void onPrediction_WhenAllPredictionsAreValid_ShouldReportNoErrors() throws Exception {
        final List<BoundingShapeData> shapes = predict(List.of(entry("inside", 0.9, 0.0, 0.0, 200.0, 100.0)),
                                                       errors -> assertEquals(List.of(), errors));

        assertBounds(new BoundingBox(0, 0, 1, 1), shapes.getFirst());
    }

    @Test
    void onPredictAll_ShouldReuseNewCategoriesAcrossImagesAndReportEachImage() throws Exception {
        final BoundingBoxPredictorClient client = fakeClient();
        when(client.predict(any())).thenReturn(List.of(entry("dog", 0.9, 20.0, 10.0, 100.0, 50.0)));
        final Map<String, ObjectCategory> existingCategories = new HashMap<>();
        final List<BoundingBoxPredictionResult> imageResults = new ArrayList<>();
        final List<Integer> progress = new ArrayList<>();

        final BoundingBoxPredictionResult result = new BoundingBoxPredictor(client, predictorConfig())
                .predictAll(List.of(imageFile("a.jpg"), imageFile("b.jpg")), metaData("a.jpg", "b.jpg"),
                            existingCategories, new BoundingBoxPredictor.Progress() {
                            @Override
                            public boolean isCancelled() {
                                return false;
                            }

                            @Override
                            public void onImagePredicted(BoundingBoxPredictionResult imageResult,
                                                         int nrProcessedImages, int nrImages) {
                                imageResults.add(imageResult);
                                progress.add(nrProcessedImages);
                                assertEquals(2, nrImages);
                            }
                        });

        assertEquals(2, result.getNrSuccessfullyProcessedItems());
        assertEquals(Map.of("dog", 2), result.getImageAnnotationData().categoryNameToBoundingShapeCountMap());
        assertEquals(List.of(1, 2), progress);
        assertTrue(existingCategories.isEmpty(), "The passed categories must not be changed.");

        final ObjectCategory firstDog = imageResults.get(0).getImageAnnotationData().categoryNameToCategoryMap()
                                                    .get("dog");
        assertSame(firstDog, imageResults.get(1).getImageAnnotationData().categoryNameToCategoryMap().get("dog"));
        assertSame(firstDog, imageResults.get(1).getImageAnnotationData().imageAnnotations().iterator().next()
                                         .getBoundingShapeData().getFirst().getCategory());
        assertEquals(List.of("a.jpg", "b.jpg"), result.getImageAnnotationData().imageAnnotations().stream()
                                                      .map(ImageAnnotation::getImageFileName).toList());
    }

    @Test
    void onPredictAll_WhenCancelled_ShouldStopBeforeTheNextImage() throws Exception {
        final BoundingBoxPredictorClient client = fakeClient();
        when(client.predict(any())).thenReturn(List.of());
        final List<Integer> progress = new ArrayList<>();

        final BoundingBoxPredictionResult result = new BoundingBoxPredictor(client, predictorConfig())
                .predictAll(List.of(imageFile("a.jpg"), imageFile("b.jpg"), imageFile("c.jpg")),
                            metaData("a.jpg", "b.jpg", "c.jpg"), new HashMap<>(), new BoundingBoxPredictor.Progress() {
                            @Override
                            public boolean isCancelled() {
                                return !progress.isEmpty();
                            }

                            @Override
                            public void onImagePredicted(BoundingBoxPredictionResult imageResult,
                                                         int nrProcessedImages, int nrImages) {
                                progress.add(nrProcessedImages);
                            }
                        });

        assertEquals(1, result.getNrSuccessfullyProcessedItems());
        assertEquals(List.of(1), progress);
    }

    @Test
    void onPredictAll_WhenRequestsKeepFailing_ShouldStopAndReportIt() throws Exception {
        final BoundingBoxPredictorClient client = fakeClient();
        when(client.predict(any())).thenThrow(new IllegalStateException("Could not connect."));
        final List<String> fileNames = List.of("a.jpg", "b.jpg", "c.jpg", "d.jpg", "e.jpg");
        final List<File> imageFiles = new ArrayList<>();

        for(String fileName : fileNames) {
            imageFiles.add(imageFile(fileName));
        }

        final BoundingBoxPredictionResult result = new BoundingBoxPredictor(client, predictorConfig())
                .predictAll(imageFiles, metaData(fileNames.toArray(String[]::new)), new HashMap<>(),
                            new BoundingBoxPredictor.Progress() {
                                @Override
                                public boolean isCancelled() {
                                    return false;
                                }

                                @Override
                                public void onImagePredicted(BoundingBoxPredictionResult imageResult,
                                                             int nrProcessedImages, int nrImages) {
                                    // Not needed.
                                }
                            });

        assertEquals(0, result.getNrSuccessfullyProcessedItems());
        assertEquals(List.of("Could not connect.", "Could not connect.", "Could not connect.",
                             "Stopped after the predictions for 3 images in a row failed."),
                     result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getErrorDescription).toList());
    }

    private List<BoundingShapeData> predict(List<BoundingBoxPredictionEntry> predictions,
                                            ErrorCheck errorCheck) throws Exception {
        final BoundingBoxPredictorClient client = mock(BoundingBoxPredictorClient.class);
        when(client.predict(any())).thenReturn(predictions);
        when(client.getName()).thenReturn("FakeServe");

        final BoundingBoxPredictorConfig predictorConfig = new BoundingBoxPredictorConfig();
        predictorConfig.setResizeImages(false);

        final File imageFile = Files.writeString(tempDir.resolve("image.jpg"), "image-bytes").toFile();
        final ImageMetaData imageMetaData = new ImageMetaData("image.jpg", "folder", imageFile.toURI().toString(),
                                                              IMAGE_WIDTH, IMAGE_HEIGHT, 3);

        final BoundingBoxPredictionResult result = new BoundingBoxPredictor(client, predictorConfig)
                .predict(imageFile, imageMetaData, new HashMap<>());

        errorCheck.check(result.getErrorTableEntries());
        assertEquals(1, result.getNrSuccessfullyProcessedItems());
        return result.getImageAnnotationData().imageAnnotations().iterator().next().getBoundingShapeData();
    }

    private static BoundingBoxPredictionEntry entry(String category, double score, Double... coordinates) {
        return new BoundingBoxPredictionEntry(Map.of(category, Arrays.asList(coordinates)), score);
    }

    private static void assertBounds(Bounds expected, BoundingShapeData shape) {
        final Bounds actual = ((BoundingBoxData) shape).getRelativeBoundsInImage();
        assertAll(() -> assertEquals(expected.getMinX(), actual.getMinX(), 1e-9),
                  () -> assertEquals(expected.getMinY(), actual.getMinY(), 1e-9),
                  () -> assertEquals(expected.getWidth(), actual.getWidth(), 1e-9),
                  () -> assertEquals(expected.getHeight(), actual.getHeight(), 1e-9));
    }

    @FunctionalInterface
    private interface ErrorCheck {
        void check(List<IOErrorInfoEntry> errors);
    }

    private static BoundingBoxPredictorClient fakeClient() {
        final BoundingBoxPredictorClient client = mock(BoundingBoxPredictorClient.class);
        when(client.getName()).thenReturn("FakeServe");
        return client;
    }

    private static BoundingBoxPredictorConfig predictorConfig() {
        final BoundingBoxPredictorConfig predictorConfig = new BoundingBoxPredictorConfig();
        predictorConfig.setResizeImages(false);
        return predictorConfig;
    }

    private File imageFile(String fileName) throws IOException {
        return Files.writeString(tempDir.resolve(fileName), "image-bytes").toFile();
    }

    private Map<String, ImageMetaData> metaData(String... fileNames) {
        final Map<String, ImageMetaData> fileNameToMetaData = new HashMap<>();

        for(String fileName : fileNames) {
            fileNameToMetaData.put(fileName, new ImageMetaData(fileName, "folder",
                    tempDir.resolve(fileName).toUri().toString(), IMAGE_WIDTH, IMAGE_HEIGHT, 3));
        }

        return fileNameToMetaData;
    }
}
