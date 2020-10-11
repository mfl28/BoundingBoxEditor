package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundingBoxPredictor {
    private final BoundingBoxPredictorClient client;
    private final BoundingBoxPredictorConfig predictorConfig;

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

            try {
                boundingBoxPredictions = client.predict(imageFile);
            } catch(Exception e) {
                errorInfoEntries.add(new IOErrorInfoEntry("Torch serve model", e.getMessage()));
                return new BoundingBoxPredictionResult(
                        0,
                        errorInfoEntries,
                        ImageAnnotationData.empty());
            }

            if(boundingBoxPredictions == null || boundingBoxPredictions.isEmpty()) {
                errorInfoEntries.add(new IOErrorInfoEntry("Torch serve model",
                                                          "No bounding boxes predicted for image " +
                                                                  imageFile.getName()));

                return new BoundingBoxPredictionResult(
                        0,
                        errorInfoEntries,
                        ImageAnnotationData.empty());
            }

            final Map<String, Integer> categoryToCount = new HashMap<>();
            final ImageAnnotation imageAnnotation = new ImageAnnotation(imageMetaData);

            for(BoundingBoxPredictionEntry entry : boundingBoxPredictions) {
                // TODO: nicer

                if(entry.getScore() < predictorConfig.getMinimumScore()) {
                    continue;
                }

                var boundingbox = entry.getCategoryToBoundingBoxes().entrySet().iterator().next();
                ObjectCategory objectCategory = existingCategoryNameToCategoryMap.computeIfAbsent(boundingbox.getKey(),
                                                                                                  key -> new ObjectCategory(
                                                                                                          boundingbox
                                                                                                                  .getKey(),
                                                                                                          ColorUtils
                                                                                                                  .createRandomColor()));
                double xMin = boundingbox.getValue().get(0) / imageAnnotation.getImageWidth();
                double yMin = boundingbox.getValue().get(1) / imageAnnotation.getImageHeight();
                double xMax = boundingbox.getValue().get(2) / imageAnnotation.getImageWidth();
                double yMax = boundingbox.getValue().get(3) / imageAnnotation.getImageHeight();

                imageAnnotation.getBoundingShapeData().add(new BoundingBoxData(objectCategory, xMin, yMin, xMax, yMax,
                                                                               new ArrayList<>()));
                categoryToCount.merge(objectCategory.getName(), 1, Integer::sum);
            }

            return new BoundingBoxPredictionResult(1, errorInfoEntries,
                                                   new ImageAnnotationData(List.of(imageAnnotation), categoryToCount,
                                                                           existingCategoryNameToCategoryMap));
        });
    }
}
