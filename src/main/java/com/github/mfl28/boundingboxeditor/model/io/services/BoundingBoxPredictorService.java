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
package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictor;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Predicts bounding boxes for one or more images. Each image's result is passed to the
 * {@link #setOnImagePredicted image handler} on the JavaFX thread as soon as it is available, so that a cancelled
 * run keeps the predictions made so far. The service's value is the combined result of all predicted images.
 */
public class BoundingBoxPredictorService extends IoService<BoundingBoxPredictionResult> {
    private final ObjectProperty<List<File>> imageFiles = new SimpleObjectProperty<>(this, "imageFiles");
    private final ObjectProperty<Map<String, ImageMetaData>> fileNameToMetaData =
            new SimpleObjectProperty<>(this, "fileNameToMetaData");

    private final ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMap =
            new SimpleObjectProperty<>(this, "categoryNameToCategoryMap");

    private final ObjectProperty<BoundingBoxPredictorClient> predictorClient = new SimpleObjectProperty<>(this,
                                                                                                          "predictorClient");
    private final ObjectProperty<BoundingBoxPredictorConfig> boundingBoxPredictorConfig =
            new SimpleObjectProperty<>(this, "boundingBoxPredictorConfig");
    private final ObjectProperty<Consumer<BoundingBoxPredictionResult>> onImagePredicted =
            new SimpleObjectProperty<>(this, "onImagePredicted", result -> {});

    public void setImageFiles(List<File> imageFiles) {
        this.imageFiles.set(imageFiles);
    }

    public void setFileNameToMetaData(Map<String, ImageMetaData> fileNameToMetaData) {
        this.fileNameToMetaData.set(fileNameToMetaData);
    }

    /**
     * Sets the categories that predicted categories are matched with. The map is not changed.
     *
     * @param categoryNameToCategoryMap the existing categories
     */
    public void setCategoryNameToCategoryMap(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.categoryNameToCategoryMap.set(categoryNameToCategoryMap);
    }

    public void setPredictorClient(BoundingBoxPredictorClient predictorClient) {
        this.predictorClient.set(predictorClient);
    }


    public void setBoundingBoxPredictorConfig(BoundingBoxPredictorConfig boundingBoxPredictorConfig) {
        this.boundingBoxPredictorConfig.set(boundingBoxPredictorConfig);
    }

    /**
     * Sets what to do with each image's result. It runs on the JavaFX thread, also after the run was cancelled for
     * an image whose prediction had already finished.
     *
     * @param onImagePredicted receives the result of one image
     */
    public void setOnImagePredicted(Consumer<BoundingBoxPredictionResult> onImagePredicted) {
        this.onImagePredicted.set(onImagePredicted);
    }

    @Override
    protected Task<BoundingBoxPredictionResult> createTask() {
        final List<File> files = List.copyOf(imageFiles.get());
        // Copied on the JavaFX thread, because the model's maps must not be read while it changes them.
        final Map<String, ImageMetaData> metaData = new HashMap<>(fileNameToMetaData.get());
        final Map<String, ObjectCategory> categories = new HashMap<>(categoryNameToCategoryMap.get());
        final BoundingBoxPredictor predictor =
                new BoundingBoxPredictor(predictorClient.get(), boundingBoxPredictorConfig.get());
        final Consumer<BoundingBoxPredictionResult> imageHandler = onImagePredicted.get();

        return new Task<>() {
            @Override
            protected BoundingBoxPredictionResult call() throws Exception {
                final Task<BoundingBoxPredictionResult> task = this;

                return predictor.predictAll(files, metaData, categories, new BoundingBoxPredictor.Progress() {
                    @Override
                    public boolean isCancelled() {
                        return task.isCancelled();
                    }

                    @Override
                    public void onImagePredicted(BoundingBoxPredictionResult result, int nrProcessedImages,
                                                 int nrImages) {
                        updateProgress(nrProcessedImages, nrImages);
                        Platform.runLater(() -> imageHandler.accept(result));
                    }
                });
            }
        };
    }
}
