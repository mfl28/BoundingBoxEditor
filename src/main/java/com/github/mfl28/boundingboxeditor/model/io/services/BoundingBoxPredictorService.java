package com.github.mfl28.boundingboxeditor.model.io.services;

import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictor;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorClient;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.results.BoundingBoxPredictionResult;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.util.Map;

public class BoundingBoxPredictorService extends Service<BoundingBoxPredictionResult> {
    private final ObjectProperty<File> imageFile = new SimpleObjectProperty<>(this, "imageFile");
    private final ObjectProperty<ImageMetaData> imageMetaData = new SimpleObjectProperty<>(this, "imageMetaData");

    private final ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMap =
            new SimpleObjectProperty<>(this, "categoryNameToCategoryMap");

    private final ObjectProperty<BoundingBoxPredictorClient> predictorClient = new SimpleObjectProperty<>(this,
                                                                                                          "predictorClient");
    private final ObjectProperty<BoundingBoxPredictorConfig> boundingBoxPredictorConfig =
            new SimpleObjectProperty<>(this, "boundingBoxPredictorConfig");

    public File getImageFile() {
        return imageFile.get();
    }

    public void setImageFile(File imageFile) {
        this.imageFile.set(imageFile);
    }

    public ObjectProperty<File> imageFileProperty() {
        return imageFile;
    }

    public ImageMetaData getImageMetaData() {
        return imageMetaData.get();
    }

    public void setImageMetaData(ImageMetaData imageMetaDate) {
        this.imageMetaData.set(imageMetaDate);
    }

    public ObjectProperty<ImageMetaData> imageMetaDataProperty() {
        return imageMetaData;
    }

    public Map<String, ObjectCategory> getCategoryNameToCategoryMap() {
        return categoryNameToCategoryMap.get();
    }

    public void setCategoryNameToCategoryMap(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        this.categoryNameToCategoryMap.set(categoryNameToCategoryMap);
    }

    public ObjectProperty<Map<String, ObjectCategory>> categoryNameToCategoryMapProperty() {
        return categoryNameToCategoryMap;
    }

    public BoundingBoxPredictorClient getPredictorClient() {
        return predictorClient.get();
    }

    public void setPredictorClient(BoundingBoxPredictorClient predictorClient) {
        this.predictorClient.set(predictorClient);
    }

    public ObjectProperty<BoundingBoxPredictorClient> predictorClientProperty() {
        return predictorClient;
    }

    public BoundingBoxPredictorConfig getBoundingBoxPredictorConfig() {
        return boundingBoxPredictorConfig.get();
    }

    public void setBoundingBoxPredictorConfig(BoundingBoxPredictorConfig boundingBoxPredictorConfig) {
        this.boundingBoxPredictorConfig.set(boundingBoxPredictorConfig);
    }

    public ObjectProperty<BoundingBoxPredictorConfig> boundingBoxPredictorConfigProperty() {
        return boundingBoxPredictorConfig;
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
