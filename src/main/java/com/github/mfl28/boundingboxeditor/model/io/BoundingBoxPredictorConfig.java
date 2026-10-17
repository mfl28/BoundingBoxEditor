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
    private final BooleanProperty skipDuplicates = new SimpleBooleanProperty(true);
    // A predicted box overlapping an existing shape of the same category at least this much (intersection over
    // union) is considered the same object. Lower values also skip neighbouring objects of the same category that
    // overlap (e.g. people in a crowd).
    private final DoubleProperty duplicateMinIntersectionOverUnion = new SimpleDoubleProperty(0.7);

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

    /**
     * Returns whether predicted boxes that duplicate an existing shape of the same category are skipped.
     *
     * @return true if duplicates are skipped
     */
    public boolean isSkipDuplicates() {
        return skipDuplicates.get();
    }

    public void setSkipDuplicates(boolean skipDuplicates) {
        this.skipDuplicates.set(skipDuplicates);
    }

    /**
     * Returns the minimal intersection over union with an existing shape of the same category from which a
     * predicted box counts as a duplicate.
     *
     * @return the minimal intersection over union, between 0 and 1
     */
    public double getDuplicateMinIntersectionOverUnion() {
        return duplicateMinIntersectionOverUnion.get();
    }

    public void setDuplicateMinIntersectionOverUnion(double duplicateMinIntersectionOverUnion) {
        this.duplicateMinIntersectionOverUnion.set(duplicateMinIntersectionOverUnion);
    }
}
