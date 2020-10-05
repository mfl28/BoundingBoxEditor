/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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

import com.github.mfl28.boundingboxeditor.ui.BoundingBoxView;
import com.github.mfl28.boundingboxeditor.ui.BoundingShapeViewable;
import com.github.mfl28.boundingboxeditor.utils.MathUtils;
import com.google.gson.annotations.SerializedName;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import java.util.List;
import java.util.Objects;

/**
 * Represents the data-component of a bounding-box. Objects of this class are used to
 * store the "blueprint" of the ui-component of a bounding-box (a {@link com.github.mfl28.boundingboxeditor.ui.BoundingBoxView BoundingBoxView}-object).
 * BoundingBoxData-objects are part of the model-component of the app and can be nested by adding child BoundingShapeData-objects as "parts" of
 * a parent object.
 *
 * @see BoundingShapeData#setParts(List)
 */
public class BoundingBoxData extends BoundingShapeData {
    @SerializedName("bndbox")
    private final Bounds relativeBoundsInImage;

    /**
     * Creates a new object to store the "blueprint" of a {@link com.github.mfl28.boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     *
     * @param category              the category of the bounding-box
     * @param relativeBoundsInImage the rectangular bounds of the bounding-box in relative coordinates
     * @param tags                  the tags that are registered for the bounding-box
     */
    public BoundingBoxData(ObjectCategory category, Bounds relativeBoundsInImage, List<String> tags) {
        super(category, tags);
        this.relativeBoundsInImage = relativeBoundsInImage;
    }

    /**
     * Creates a new object to store the "blueprint" of a {@link com.github.mfl28.boundingboxeditor.ui.BoundingBoxView BoundingBoxView}.
     * All coordinate arguments are considered to be with respect to the image the bounding-box belongs to
     * (considering the image's original measurements).
     *
     * @param category     the category of the bounding-box
     * @param xMinRelative the x-coordinate of the upper left corner of the bounding-box
     * @param yMinRelative the y-coordinate of the upper left corner of the bounding-box
     * @param xMaxRelative the x-coordinate of the lower right corner of the bounding-box
     * @param yMaxRelative the y-coordinate of the lower right corner of the bounding-box
     * @param tags         the tags that are registered for the bounding-box
     */
    public BoundingBoxData(ObjectCategory category, double xMinRelative, double yMinRelative,
                           double xMaxRelative, double yMaxRelative, List<String> tags) {
        super(category, tags);
        this.relativeBoundsInImage = new BoundingBox(xMinRelative, yMinRelative,
                                                     xMaxRelative - xMinRelative, yMaxRelative - yMinRelative);
    }

    /**
     * Returns the x-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMinRelative() {
        return relativeBoundsInImage.getMinX();
    }

    /**
     * Returns the y-coordinate of the upper left corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMinRelative() {
        return relativeBoundsInImage.getMinY();
    }

    /**
     * Returns the x-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the x-coordinate
     */
    public double getXMaxRelative() {
        return relativeBoundsInImage.getMaxX();
    }

    /**
     * Returns the y-coordinate of the lower right corner of the bounding-box
     * with respect to the image it belongs to (considering the image's original measurements).
     *
     * @return the y-coordinate
     */
    public double getYMaxRelative() {
        return relativeBoundsInImage.getMaxY();
    }

    /**
     * Returns the relative bounds of the bounding-box.
     *
     * @return the bounds in the coordinate system defined by the originally loaded image.
     */
    public Bounds getRelativeBoundsInImage() {
        return relativeBoundsInImage;
    }

    public Bounds getAbsoluteBoundsInImage(double imageWidth, double imageHeight) {
        double xMin = getXMinRelative() * imageWidth;
        double yMin = getYMinRelative() * imageHeight;
        double xMax = getXMaxRelative() * imageWidth;
        double yMax = getYMaxRelative() * imageHeight;

        return new BoundingBox(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    @Override
    public BoundingShapeViewable toBoundingShapeView(double imageWidth, double imageHeight) {
        return BoundingBoxView.fromData(this, imageWidth, imageHeight);
    }

    @Override
    public <T> T accept(BoundingShapeDataVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relativeBoundsInImage);
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        }
        if(!(o instanceof BoundingBoxData)) {
            return false;
        }
        if(!super.equals(o)) {
            return false;
        }
        BoundingBoxData that = (BoundingBoxData) o;

        if(relativeBoundsInImage == that.relativeBoundsInImage) {
            return true;
        }

        return MathUtils.doubleAlmostEqual(relativeBoundsInImage.getMinX(), that.relativeBoundsInImage.getMinX()) &&
                MathUtils.doubleAlmostEqual(relativeBoundsInImage.getMinY(), that.relativeBoundsInImage.getMinY()) &&
                MathUtils.doubleAlmostEqual(relativeBoundsInImage.getMaxX(), that.relativeBoundsInImage.getMaxX()) &&
                MathUtils.doubleAlmostEqual(relativeBoundsInImage.getMaxY(), that.relativeBoundsInImage.getMaxY());
    }
}


