package com.github.mfl28.boundingboxeditor.model.io.restclients;

public class ModelEntry {
    private final String modelName;
    private final String modelUrl;

    public ModelEntry(String modelName, String modelUrl) {
        this.modelName = modelName;
        this.modelUrl = modelUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelUrl() {
        return modelUrl;
    }
}
