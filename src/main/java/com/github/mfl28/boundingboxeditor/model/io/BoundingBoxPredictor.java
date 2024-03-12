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

public class BoundingBoxPredictor {
    private static final String DEFAULT_IMAGE_STREAM_FORMAT_NAME = "png";
    private static final String NON_EXISTENT_IMAGE_ERROR_MESSAGE = "Image file does not exist.";
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
            } catch(FileNotFoundException e) {
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

            imageAnnotation.getBoundingShapeData()
                    .addAll(boundingBoxPredictions.stream()
                            .filter(prediction ->
                                    Double.compare(prediction.score(),
                                            predictorConfig
                                                    .getMinimumScore()) >=
                                            0)
                            .map(predictionExtractor::extract).toList());

            return new BoundingBoxPredictionResult(1, errorInfoEntries,
                    new ImageAnnotationData(List.of(imageAnnotation), categoryToCount,
                            existingCategoryNameToCategoryMap));
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

    private class PredictionExtractor {
        private final Map<String, ObjectCategory> existingCategoryNameToCategoryMap;
        private final Map<String, Integer> categoryNameToShapeCount;
        private CaseInsensitiveMap<String, ObjectCategory> mergedCategoryNameToCategoryMap;

        public PredictionExtractor(
                Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                Map<String, Integer> categoryNameToShapeCount) {
            this.existingCategoryNameToCategoryMap = existingCategoryNameToCategoryMap;
            this.categoryNameToShapeCount = categoryNameToShapeCount;

            if(predictorConfig.isMergeCategories()) {
                mergedCategoryNameToCategoryMap = new CaseInsensitiveMap<>(existingCategoryNameToCategoryMap);
            }
        }

        public BoundingBoxData extract(BoundingBoxPredictionEntry prediction) {
            final Map.Entry<String, List<Double>> boundingBoxCoordinatesEntry =
                    prediction.categoryToBoundingBoxes().entrySet().iterator().next();

            final String predictedCategory = boundingBoxCoordinatesEntry.getKey();

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

            double xMin = boundingBoxCoordinatesEntry.getValue().get(0) / predictedImageWidth;
            double yMin = boundingBoxCoordinatesEntry.getValue().get(1) / predictedImageHeight;
            double xMax = boundingBoxCoordinatesEntry.getValue().get(2) / predictedImageWidth;
            double yMax = boundingBoxCoordinatesEntry.getValue().get(3) / predictedImageHeight;

            categoryNameToShapeCount.merge(objectCategory.getName(), 1, Integer::sum);

            return new BoundingBoxData(objectCategory, xMin, yMin, xMax, yMax, new ArrayList<>());
        }
    }
}
