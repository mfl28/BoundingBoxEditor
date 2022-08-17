/*
 * Copyright (C) 2022 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import javafx.beans.property.*;

public class BoundingBoxPredictorConfig {
    private final BooleanProperty inferenceEnabled = new SimpleBooleanProperty(false);
    private final DoubleProperty minimumScore = new SimpleDoubleProperty(0.5);
    private final BooleanProperty resizeImages = new SimpleBooleanProperty(true);
    private final IntegerProperty imageResizeWidth = new SimpleIntegerProperty(600);
    private final IntegerProperty imageResizeHeight = new SimpleIntegerProperty(600);
    private final BooleanProperty imageResizeKeepRatio = new SimpleBooleanProperty(true);
    private final BooleanProperty mergeCategories = new SimpleBooleanProperty(true);

    public double getMinimumScore() {
        return minimumScore.get();
    }

    public void setMinimumScore(double minimumScore) {
        this.minimumScore.set(minimumScore);
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

    public int getImageResizeWidth() {
        return imageResizeWidth.get();
    }

    public void setImageResizeWidth(int imageResizeWidth) {
        this.imageResizeWidth.set(imageResizeWidth);
    }

    public int getImageResizeHeight() {
        return imageResizeHeight.get();
    }

    public void setImageResizeHeight(int imageResizeHeight) {
        this.imageResizeHeight.set(imageResizeHeight);
    }

    public boolean getImageResizeKeepRatio() {
        return imageResizeKeepRatio.get();
    }

    public void setImageResizeKeepRatio(boolean imageResizeKeepRatio) {
        this.imageResizeKeepRatio.set(imageResizeKeepRatio);
    }

    public boolean isResizeImages() {
        return resizeImages.get();
    }

    public void setResizeImages(boolean resizeImages) {
        this.resizeImages.set(resizeImages);
    }

    public boolean isMergeCategories() {
        return mergeCategories.get();
    }

    public void setMergeCategories(boolean mergeCategories) {
        this.mergeCategories.set(mergeCategories);
    }
}
