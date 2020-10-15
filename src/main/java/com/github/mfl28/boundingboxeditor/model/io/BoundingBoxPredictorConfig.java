package com.github.mfl28.boundingboxeditor.model.io;

import javafx.beans.property.*;

public class BoundingBoxPredictorConfig {
    private final BooleanProperty inferenceEnabled = new SimpleBooleanProperty(false);
    private final DoubleProperty minimumScore = new SimpleDoubleProperty(0.5);
    private final BooleanProperty resizeImages = new SimpleBooleanProperty(true);
    private final IntegerProperty maxImageWidth = new SimpleIntegerProperty(400);
    private final IntegerProperty maxImageHeight = new SimpleIntegerProperty(400);
    private final BooleanProperty keepImageRatio = new SimpleBooleanProperty(true);
    private final BooleanProperty mergeCategories = new SimpleBooleanProperty(true);

    public double getMinimumScore() {
        return minimumScore.get();
    }

    public void setMinimumScore(double minimumScore) {
        this.minimumScore.set(minimumScore);
    }

    public DoubleProperty minimumScoreProperty() {
        return minimumScore;
    }

    public boolean isInferenceEnabled() {
        return inferenceEnabled.get();
    }

    public void setInferenceEnabled(boolean inferenceEnabled) {
        this.inferenceEnabled.set(inferenceEnabled);
    }

    public BooleanProperty inferenceEnabledProperty() {
        return inferenceEnabled;
    }

    public int getMaxImageWidth() {
        return maxImageWidth.get();
    }

    public void setMaxImageWidth(int maxImageWidth) {
        this.maxImageWidth.set(maxImageWidth);
    }

    public IntegerProperty maxImageWidthProperty() {
        return maxImageWidth;
    }

    public int getMaxImageHeight() {
        return maxImageHeight.get();
    }

    public void setMaxImageHeight(int maxImageHeight) {
        this.maxImageHeight.set(maxImageHeight);
    }

    public IntegerProperty maxImageHeightProperty() {
        return maxImageHeight;
    }

    public boolean isKeepImageRatio() {
        return keepImageRatio.get();
    }

    public void setKeepImageRatio(boolean keepImageRatio) {
        this.keepImageRatio.set(keepImageRatio);
    }

    public BooleanProperty keepImageRatioProperty() {
        return keepImageRatio;
    }

    public boolean isResizeImages() {
        return resizeImages.get();
    }

    public void setResizeImages(boolean resizeImages) {
        this.resizeImages.set(resizeImages);
    }

    public BooleanProperty resizeImagesProperty() {
        return resizeImages;
    }

    public boolean isMergeCategories() {
        return mergeCategories.get();
    }

    public void setMergeCategories(boolean mergeCategories) {
        this.mergeCategories.set(mergeCategories);
    }

    public BooleanProperty mergeCategoriesProperty() {
        return mergeCategories;
    }
}
