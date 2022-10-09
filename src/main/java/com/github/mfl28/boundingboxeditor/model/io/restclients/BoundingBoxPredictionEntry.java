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
package com.github.mfl28.boundingboxeditor.model.io.restclients;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BoundingBoxPredictionEntry {
    private final Map<String, List<Double>> categoryToBoundingBoxes;
    private final Double score;

    public BoundingBoxPredictionEntry(Map<String, List<Double>> categoryToBoundingBoxes, Double score) {
        this.categoryToBoundingBoxes = categoryToBoundingBoxes;
        this.score = score;
    }

    public Map<String, List<Double>> categoryToBoundingBoxes() {
        return categoryToBoundingBoxes;
    }

    public Double score() {
        return score;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if(obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        BoundingBoxPredictionEntry that = (BoundingBoxPredictionEntry) obj;

        return Objects.equals(this.categoryToBoundingBoxes, that.categoryToBoundingBoxes) &&
                Objects.equals(this.score, that.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryToBoundingBoxes, score);
    }
}
