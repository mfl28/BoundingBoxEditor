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
package com.github.mfl28.boundingboxeditor.model.data;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * A class holding all data of an image-annotation, including metadata of the image and all {@link BoundingShapeData}-objects assigned to it.
 * There will be at most one ImageAnnotation-object for each loaded image.
 */
public class ImageAnnotation {
    @SerializedName("image")
    private ImageMetaData imageMetaData;
    @SerializedName("objects")
    private List<BoundingShapeData> boundingShapeData = new ArrayList<>();

    /**
     * Creates a new image-annotation
     *
     * @param imageMetaData the metadata of the annotated image
     */
    public ImageAnnotation(ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
    }

    /**
     * Creates a new image-annotation
     *
     * @param imageMetaData the metadata of the annotated image
     */
    public ImageAnnotation(ImageMetaData imageMetaData, List<BoundingShapeData> boundingShapeData) {
        this.imageMetaData = imageMetaData;
        this.boundingShapeData = boundingShapeData;
    }


    /**
     * Returns the annotation's bounding-shape data.
     *
     * @return list of data of bounding-shapes
     */
    public List<BoundingShapeData> getBoundingShapeData() {
        return boundingShapeData;
    }

    /**
     * Sets the annotation's bounding-shape data.
     *
     * @param boundingShapeData the list of data of bounding-shapes to set
     */
    public void setBoundingShapeData(List<BoundingShapeData> boundingShapeData) {
        this.boundingShapeData = boundingShapeData;
    }

    /**
     * Returns metadata of the annotated image-file.
     *
     * @return the image metadata
     */
    public ImageMetaData getImageMetaData() {
        return imageMetaData;
    }

    public void setImageMetaData(ImageMetaData imageMetaData) {
        this.imageMetaData = imageMetaData;
    }

    /**
     * Returns the name of the annotated image-file.
     *
     * @return the filename
     */
    public String getImageFileName() {
        return imageMetaData.getFileName();
    }

    /**
     * Returns the height of the oriented annotated image.
     *
     * @return the height of the image
     */
    public double getOrientedImageHeight() {
        return imageMetaData.getOrientedHeight();
    }

    /**
     * Returns the depth (= number of channels) of the annotated image.
     *
     * @return the depth of the image
     */
    public int getImageDepth() {
        return imageMetaData.getImageDepth();
    }

    /**
     * Returns the width of the oriented annotated image.
     *
     * @return the width of the image
     */
    public double getOrientedImageWidth() {
        return imageMetaData.getOrientedWidth();
    }

    /**
     * Returns the name of the annotated image's containing folder.
     *
     * @return the folder-name
     */
    public String getContainingFolderName() {
        return imageMetaData.getFolderName();
    }
}
