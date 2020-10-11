package com.github.mfl28.boundingboxeditor.model.io;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class BoundingBoxPredictorConfig {
    private final DoubleProperty minimumScore = new SimpleDoubleProperty(0.5);

    public double getMinimumScore() {
        return minimumScore.get();
    }

    public DoubleProperty minimumScoreProperty() {
        return minimumScore;
    }

    public void setMinimumScore(double minimumScore) {
        this.minimumScore.set(minimumScore);
    }
}
