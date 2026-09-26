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
package com.github.mfl28.boundingboxeditor.model.io.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;

/**
 * A row of a CSV annotation file: one bounding box in absolute pixel coordinates. Sizes and coordinates written
 * with decimals are rounded when read.
 */
@JsonPropertyOrder({ "filename", "width", "height", "class" , "xmin", "ymin", "xmax", "ymax" })
public record CSVRow(
        @JsonProperty(required = true) String filename,
        @JsonProperty(required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int width,
        @JsonProperty(required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int height,
        @JsonProperty(value = "class", required = true) String categoryName,
        @JsonProperty(value = "xmin", required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int xMin,
        @JsonProperty(value = "ymin", required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int yMin,
        @JsonProperty(value = "xmax", required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int xMax,
        @JsonProperty(value = "ymax", required = true) @JsonDeserialize(using = RoundingIntDeserializer.class) int yMax) {

    public static CSVRow fromData(ImageAnnotation imageAnnotation, BoundingBoxData boundingBoxData) {
        double imageWidth = imageAnnotation.getImageMetaData().getImageWidth();
        double imageHeight = imageAnnotation.getImageMetaData().getImageHeight();

        var bounds = boundingBoxData.getAbsoluteBoundsInImage(imageWidth, imageHeight);

        return new CSVRow(
                imageAnnotation.getImageFileName(),
                (int) Math.round(imageWidth),
                (int) Math.round(imageHeight),
                boundingBoxData.getCategoryName(),
                (int) Math.round(bounds.getMinX()),
                (int) Math.round(bounds.getMinY()),
                (int) Math.round(bounds.getMaxX()),
                (int) Math.round(bounds.getMaxY()));
    }
}
