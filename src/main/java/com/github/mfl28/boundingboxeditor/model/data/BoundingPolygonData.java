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
package com.github.mfl28.boundingboxeditor.model.data;

import com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView;
import com.github.mfl28.boundingboxeditor.ui.BoundingShapeViewable;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents the data-component of a bounding-polygon. Objects of this class are used to
 * store the "blueprint" of the ui-component of a {@link com.github.mfl28.boundingboxeditor.ui.BoundingPolygonView}-object).
 * BoundingPolygonData-objects are part of the model-component of the app and can be nested by adding child BoundingShapeData-objects as "parts" of
 * a parent object.
 *
 * @see BoundingShapeData#setParts(List)
 */
public final class BoundingPolygonData extends BoundingShapeData {
    @SerializedName("polygon")
    private final List<Double> relativePointsInImage;

    public BoundingPolygonData(ObjectCategory category, List<Double> points, List<String> tags) {
        super(category, tags);
        this.relativePointsInImage = points;
    }

    public static List<Double> absoluteToRelativePoints(List<Double> absolutePoints, double width, double height) {
        List<Double> relativePoints = new ArrayList<>(absolutePoints.size());

        for(int i = 0; i < absolutePoints.size(); i += 2) {
            relativePoints.add(absolutePoints.get(i) / width);
            relativePoints.add(absolutePoints.get(i + 1) / height);
        }

        return relativePoints;
    }

    public static List<Double> relativeToAbsolutePoints(List<Double> relativePoints, double width, double height) {
        List<Double> absolutePoints = new ArrayList<>(relativePoints.size());

        for(int i = 0; i < relativePoints.size(); i += 2) {
            absolutePoints.add(relativePoints.get(i) * width);
            absolutePoints.add(relativePoints.get(i + 1) * height);
        }

        return absolutePoints;
    }

    public List<Double> getRelativePointsInImage() {
        return relativePointsInImage;
    }

    public List<Double> getAbsolutePointsInImage(double imageWidth, double imageHeight) {
        return BoundingPolygonData.relativeToAbsolutePoints(relativePointsInImage,
                                                            imageWidth,
                                                            imageHeight);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight) {
        return BoundingPolygonView.fromData(this, imageWidth, imageHeight);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relativePointsInImage);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }

        if(!(o instanceof BoundingPolygonData that)) {
            return false;
        }

        if(!super.equals(o)) {
            return false;
        }

        if(relativePointsInImage == that.relativePointsInImage) {
            return true;
        }

        if((relativePointsInImage == null) || (that.relativePointsInImage == null)) {
            return false;
        }

        if(relativePointsInImage.size() != that.relativePointsInImage.size()) {
            return false;
        }

        for(int i = 0; i != relativePointsInImage.size(); ++i) {
            if(!MathUtils.doubleAlmostEqual(relativePointsInImage.get(i), that.relativePointsInImage.get(i))) {
                return false;
            }
        }

        return true;
    }
}
