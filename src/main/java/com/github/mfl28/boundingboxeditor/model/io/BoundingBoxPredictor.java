package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictionEntry;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
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
import java.util.stream.Collectors;

public class BoundingBoxPredictor {
    private static final String DEFAULT_IMAGE_STREAM_FORMAT_NAME = "png";
    private static final String NON_EXISTANT_IMAGE_ERROR_MESSAGE = "Image file does not exist.";
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

            try(final InputStream inputStream = createInputStream(imageFile,
                                                                  imageMetaData.getImageWidth(),
                                                                  imageMetaData.getImageHeight())) {
                boundingBoxPredictions = client.predict(inputStream);
            } catch(FileNotFoundException e) {
                errorInfoEntries.add(new IOErrorInfoEntry(imageFile.getName(), NON_EXISTANT_IMAGE_ERROR_MESSAGE));
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

            if(boundingBoxPredictions == null || boundingBoxPredictions.isEmpty()) {
                return new BoundingBoxPredictionResult(
                        1,
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
                                                                         Double.compare(prediction.getScore(),
                                                                                        predictorConfig
                                                                                                .getMinimumScore()) >=
                                                                                 0)
                                                         .map(predictionExtractor::extract)
                                                         .collect(Collectors.toList()));

            return new BoundingBoxPredictionResult(1, errorInfoEntries,
                                                   new ImageAnnotationData(List.of(imageAnnotation), categoryToCount,
                                                                           existingCategoryNameToCategoryMap));
        });
    }

    private InputStream createInputStream(File imageFile, double originalImageWidth,
                                          double originalImageHeight) throws IOException {
        if(shouldResize()) {
            final Image image = new Image(imageFile.toURI().toString(),
                                          predictorConfig.getImageResizeWidth(),
                                          predictorConfig.getImageResizeHeight(),
                                          predictorConfig.getImageResizeKeepRatio(),
                                          true,
                                          false);

            predictedImageWidth = image.getWidth();
            predictedImageHeight = image.getHeight();

            final BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

            try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(bufferedImage, DEFAULT_IMAGE_STREAM_FORMAT_NAME, outputStream);
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
        } else {
            predictedImageWidth = originalImageWidth;
            predictedImageHeight = originalImageHeight;
            return new FileInputStream(imageFile);
        }
    }

    private boolean shouldResize() {
        return predictorConfig.isResizeImages() &&
                !(predictorConfig.getImageResizeWidth() == 0 && predictorConfig.getImageResizeHeight() == 0);
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
                    prediction.getCategoryToBoundingBoxes().entrySet().iterator().next();

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
