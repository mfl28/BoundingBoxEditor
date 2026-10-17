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

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.github.mfl28.boundingboxeditor.utils.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BoundingBoxPredictor {
    private static final String DEFAULT_IMAGE_STREAM_FORMAT_NAME = "png";
    private static final String NON_EXISTENT_IMAGE_ERROR_MESSAGE = "Image file does not exist.";
    private static final String INVALID_PREDICTIONS_ERROR_MESSAGE = " predicted object(s) had an unexpected format "
            + "and were skipped. Expected: {\"<category>\": [xmin, ymin, xmax, ymax], \"score\": <score>}.";
    private static final String OUTSIDE_PREDICTIONS_ERROR_MESSAGE =
            " predicted box(es) did not overlap the image and were skipped.";
    // Stops predicting further images when the server keeps failing (e.g. it is not running), instead of
    // waiting for every request to fail.
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final String TOO_MANY_FAILURES_ERROR_MESSAGE = "Stopped after the predictions for "
            + MAX_CONSECUTIVE_FAILURES + " images in a row failed.";
    private final BoundingBoxPredictorClient client;
    private final BoundingBoxPredictorConfig predictorConfig;
    private double predictedImageWidth;
    private double predictedImageHeight;

    public BoundingBoxPredictor(BoundingBoxPredictorClient client, BoundingBoxPredictorConfig predictorConfig) {
        this.client = client;
        this.predictorConfig = predictorConfig;
    }

    public BoundingBoxPredictionResult predict(File imageFile, ImageMetaData imageMetaData,
                                               Map<String, ObjectCategory> existingCategoryNameToCategoryMap)
            throws Exception {
        return IOOperationTimer.time(() -> {
            final List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();

            final List<BoundingBoxPredictionEntry> boundingBoxPredictions;

            try(final InputStream inputStream = createInputStream(imageFile, imageMetaData)) {
                boundingBoxPredictions = client.predict(inputStream);
            } catch(FileNotFoundException _) {
                errorInfoEntries.add(new IOErrorInfoEntry(imageFile.getName(), NON_EXISTENT_IMAGE_ERROR_MESSAGE));
                return new BoundingBoxPredictionResult(
                        0,
                        errorInfoEntries,
                        ImageAnnotationData.empty()
                );
            } catch(Exception e) {
                errorInfoEntries.add(new IOErrorInfoEntry(client.getName(), e.getMessage()));
                return new BoundingBoxPredictionResult(
                        0,
                        errorInfoEntries,
                        ImageAnnotationData.empty());
            }

            final Map<String, Integer> categoryToCount = new HashMap<>();
            final ImageAnnotation imageAnnotation = new ImageAnnotation(imageMetaData);

            final PredictionExtractor predictionExtractor = new PredictionExtractor(existingCategoryNameToCategoryMap,
                    categoryToCount);

            for(BoundingBoxPredictionEntry prediction : boundingBoxPredictions) {
                if(Double.compare(prediction.score(), predictorConfig.getMinimumScore()) >= 0) {
                    predictionExtractor.extract(prediction).ifPresent(imageAnnotation.getBoundingShapeData()::add);
                }
            }

            predictionExtractor.reportSkippedPredictions(errorInfoEntries);

            return new BoundingBoxPredictionResult(1, errorInfoEntries,
                    new ImageAnnotationData(List.of(imageAnnotation), categoryToCount,
                            existingCategoryNameToCategoryMap));
        });
    }

    /**
     * Predicts bounding boxes for several images, one after the other. Each image's result is passed to the
     * {@link Progress} as soon as it is available, so that the predictions made so far are kept when the run is
     * cancelled.
     *
     * @param imageFiles                        the image files
     * @param fileNameToMetaData                the metadata of the image files, by file name
     * @param existingCategoryNameToCategoryMap the existing categories, which are not changed: new categories are
     *                                          added to a copy, and each image's result contains a copy
     *                                          of the categories created so far
     * @param progress                          receives the results and tells whether to stop
     * @return the combined result of all predicted images
     */
    public BoundingBoxPredictionResult predictAll(List<File> imageFiles, Map<String, ImageMetaData> fileNameToMetaData,
                                                  Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                                  Progress progress) throws Exception {
        return IOOperationTimer.time(() -> {
            final Map<String, ObjectCategory> categoryNameToCategory =
                    new HashMap<>(existingCategoryNameToCategoryMap);
            final List<ImageAnnotation> imageAnnotations = new ArrayList<>();
            final Map<String, Integer> categoryToCount = new HashMap<>();
            final List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();
            int nrPredictedImages = 0;
            int nrConsecutiveFailures = 0;

            for(int i = 0; i < imageFiles.size() && !progress.isCancelled(); ++i) {
                final File imageFile = imageFiles.get(i);
                final BoundingBoxPredictionResult result =
                        predict(imageFile, fileNameToMetaData.get(imageFile.getName()), categoryNameToCategory);
                final ImageAnnotationData data = result.getImageAnnotationData();

                imageAnnotations.addAll(data.imageAnnotations());
                data.categoryNameToBoundingShapeCountMap().forEach((name, count) ->
                        categoryToCount.merge(name, count, Integer::sum));
                errorInfoEntries.addAll(result.getErrorTableEntries());
                nrPredictedImages += result.getNrSuccessfullyProcessedItems();

                progress.onImagePredicted(new BoundingBoxPredictionResult(result.getNrSuccessfullyProcessedItems(),
                        result.getErrorTableEntries(), new ImageAnnotationData(data.imageAnnotations(),
                        data.categoryNameToBoundingShapeCountMap(), new HashMap<>(categoryNameToCategory))),
                        i + 1, imageFiles.size());

                nrConsecutiveFailures = result.getNrSuccessfullyProcessedItems() == 0 ? nrConsecutiveFailures + 1 : 0;

                if(nrConsecutiveFailures == MAX_CONSECUTIVE_FAILURES && i + 1 < imageFiles.size()) {
                    errorInfoEntries.add(new IOErrorInfoEntry(client.getName(), TOO_MANY_FAILURES_ERROR_MESSAGE));
                    break;
                }
            }

            return new BoundingBoxPredictionResult(nrPredictedImages, errorInfoEntries,
                    new ImageAnnotationData(imageAnnotations, categoryToCount, categoryNameToCategory));
        });
    }

    private InputStream createInputStream(File imageFile, ImageMetaData imageMetaData) throws IOException {
        if(shouldResize()) {
            Image image = new Image(
                    imageFile.toURI().toString(),
                    imageMetaData.getOrientation() < 5 ? predictorConfig.getImageResizeWidth() : predictorConfig.getImageResizeHeight(),
                    imageMetaData.getOrientation() < 5 ? predictorConfig.getImageResizeHeight() : predictorConfig.getImageResizeWidth(),
                    predictorConfig.getImageResizeKeepRatio(),
                    true,
                    false);

            if(imageMetaData.getOrientation() != 1) {
                image = ImageUtils.reorientImage(image, imageMetaData.getOrientation());
            }

            predictedImageWidth = image.getWidth();
            predictedImageHeight = image.getHeight();

            return imageToInputStream(image);
        } else {
            predictedImageWidth = imageMetaData.getOrientedWidth();
            predictedImageHeight = imageMetaData.getOrientedHeight();

            if(imageMetaData.getOrientation() != 1) {
                final Image image = new Image(imageFile.toURI().toString(), false);
                return imageToInputStream(ImageUtils.reorientImage(image, imageMetaData.getOrientation()));
            }

            return new FileInputStream(imageFile);
        }
    }

    private boolean shouldResize() {
        return predictorConfig.isResizeImages() &&
                !(predictorConfig.getImageResizeWidth() == 0 && predictorConfig.getImageResizeHeight() == 0);
    }

    private InputStream imageToInputStream(Image image) throws IOException {
        final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, DEFAULT_IMAGE_STREAM_FORMAT_NAME, outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * Receives the results of {@link #predictAll} while it runs.
     */
    public interface Progress {
        /**
         * Returns whether the remaining images should not be predicted.
         *
         * @return true to stop
         */
        boolean isCancelled();

        /**
         * Called after an image was predicted, also when its prediction failed.
         *
         * @param result            the image's result
         * @param nrProcessedImages the number of images processed so far
         * @param nrImages          the number of images to predict
         */
        void onImagePredicted(BoundingBoxPredictionResult result, int nrProcessedImages, int nrImages);
    }

    private class PredictionExtractor {
        private final Map<String, ObjectCategory> existingCategoryNameToCategoryMap;
        private final Map<String, Integer> categoryNameToShapeCount;
        private CaseInsensitiveMap<String, ObjectCategory> mergedCategoryNameToCategoryMap;
        private int nrInvalidPredictions = 0;
        private int nrPredictionsOutsideImage = 0;

        PredictionExtractor(
                Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                Map<String, Integer> categoryNameToShapeCount) {
            this.existingCategoryNameToCategoryMap = existingCategoryNameToCategoryMap;
            this.categoryNameToShapeCount = categoryNameToShapeCount;

            if(predictorConfig.isMergeCategories()) {
                mergedCategoryNameToCategoryMap = new CaseInsensitiveMap<>(existingCategoryNameToCategoryMap);
            }
        }

        /**
         * Converts a prediction into a bounding box, clamped to the image. Predictions without a category and at
         * least four coordinates, and boxes that don't overlap the image, are skipped and counted.
         */
        Optional<BoundingBoxData> extract(BoundingBoxPredictionEntry prediction) {
            final Optional<Map.Entry<String, List<Double>>> boundingBoxCoordinatesEntry =
                    prediction.categoryToBoundingBoxes().entrySet().stream().findFirst();

            if(boundingBoxCoordinatesEntry.isEmpty() || !hasCoordinates(boundingBoxCoordinatesEntry.get().getValue())) {
                ++nrInvalidPredictions;
                return Optional.empty();
            }

            final List<Double> coordinates = boundingBoxCoordinatesEntry.get().getValue();
            // Models may predict boxes that extend past the image; they are cut off at its borders.
            final double xMin = clampToImage(coordinates.get(0) / predictedImageWidth);
            final double yMin = clampToImage(coordinates.get(1) / predictedImageHeight);
            final double xMax = clampToImage(coordinates.get(2) / predictedImageWidth);
            final double yMax = clampToImage(coordinates.get(3) / predictedImageHeight);

            if(!(xMax > xMin && yMax > yMin)) {
                ++nrPredictionsOutsideImage;
                return Optional.empty();
            }

            final String predictedCategory = boundingBoxCoordinatesEntry.get().getKey();

            ObjectCategory objectCategory;

            if(predictorConfig.isMergeCategories()) {
                objectCategory = mergedCategoryNameToCategoryMap.computeIfAbsent(predictedCategory, key -> {
                    final ObjectCategory newCategory = new ObjectCategory(predictedCategory,
                            ColorUtils.createRandomColor());
                    existingCategoryNameToCategoryMap.put(predictedCategory, newCategory);
                    return newCategory;
                });
            } else {
                objectCategory =
                        existingCategoryNameToCategoryMap
                                .computeIfAbsent(predictedCategory, key -> new ObjectCategory(
                                        predictedCategory,
                                        ColorUtils.createRandomColor()));
            }

            categoryNameToShapeCount.merge(objectCategory.getName(), 1, Integer::sum);

            return Optional.of(new BoundingBoxData(objectCategory, xMin, yMin, xMax, yMax, new ArrayList<>()));
        }

        void reportSkippedPredictions(List<IOErrorInfoEntry> errorInfoEntries) {
            if(nrInvalidPredictions != 0) {
                errorInfoEntries.add(new IOErrorInfoEntry(client.getName(),
                        nrInvalidPredictions + INVALID_PREDICTIONS_ERROR_MESSAGE));
            }

            if(nrPredictionsOutsideImage != 0) {
                errorInfoEntries.add(new IOErrorInfoEntry(client.getName(),
                        nrPredictionsOutsideImage + OUTSIDE_PREDICTIONS_ERROR_MESSAGE));
            }
        }

        private static boolean hasCoordinates(List<Double> coordinates) {
            return coordinates != null && coordinates.size() >= 4
                    && coordinates.subList(0, 4).stream().allMatch(value -> value != null && Double.isFinite(value));
        }

        private static double clampToImage(double relativeCoordinate) {
            return Math.clamp(relativeCoordinate, 0.0, 1.0);
        }
    }
}
