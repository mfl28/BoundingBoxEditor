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
package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictor;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import java.io.File;
import java.util.Map;

public class BoundingBoxPredictorService extends IoService<BoundingBoxPredictionResult> {
    private final ObjectProperty<File> imageFile = new SimpleObjectProperty<>(this, "imageFile");
    private final ObjectProperty<ImageMetaData> imageMetaData = new SimpleObjectProperty<>(this, "imageMetaData");

    private final ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMap =
            new SimpleObjectProperty<>(this, "categoryNameToCategoryMap");

    private final ObjectProperty<BoundingBoxPredictorClient> predictorClient = new SimpleObjectProperty<>(this,
                                                                                                          "predictorClient");
    private final ObjectProperty<BoundingBoxPredictorConfig> boundingBoxPredictorConfig =
            new SimpleObjectProperty<>(this, "boundingBoxPredictorConfig");


    public void setImageFile(File imageFile) {
        this.imageFile.set(imageFile);
    }

    public void setImageMetaData(ImageMetaData imageMetaDate) {
        this.imageMetaData.set(imageMetaDate);
    }


    public void setCategoryNameToCategoryMap(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.categoryNameToCategoryMap.set(categoryNameToCategoryMap);
    }

    public void setPredictorClient(BoundingBoxPredictorClient predictorClient) {
        this.predictorClient.set(predictorClient);
    }


    public void setBoundingBoxPredictorConfig(BoundingBoxPredictorConfig boundingBoxPredictorConfig) {
        this.boundingBoxPredictorConfig.set(boundingBoxPredictorConfig);
    }

    @Override
    protected Task<BoundingBoxPredictionResult> createTask() {
        return new Task<>() {
            @Override
            protected BoundingBoxPredictionResult call() throws Exception {
                return new BoundingBoxPredictor(predictorClient.get(), boundingBoxPredictorConfig.get())
                        .predict(imageFile.get(), imageMetaData.get(), categoryNameToCategoryMap.get());
            }
        };
    }
}
