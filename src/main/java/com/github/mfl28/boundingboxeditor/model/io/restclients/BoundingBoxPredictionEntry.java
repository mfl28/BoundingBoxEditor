package com.github.mfl28.boundingboxeditor.model.io.restclients;

import java.util.List;
import java.util.Map;

public class BoundingBoxPredictionEntry {
    private final Map<String, List<Double>> categoryToBoundingBoxes;
    private final Double score;

    public BoundingBoxPredictionEntry(Map<String, List<Double>> categoryToBoundingBoxes, Double score) {
        this.categoryToBoundingBoxes = categoryToBoundingBoxes;
        this.score = score;
    }

    public Map<String, List<Double>> getCategoryToBoundingBoxes() {
        return categoryToBoundingBoxes;
    }

    public Double getScore() {
        return score;
    }
}
