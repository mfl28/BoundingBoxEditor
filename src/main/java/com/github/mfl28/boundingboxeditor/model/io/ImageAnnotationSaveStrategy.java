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
package com.github.mfl28.boundingboxeditor.model.io;

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;

import java.nio.file.Path;
import java.security.InvalidParameterException;

/**
 * The interface of an image annotation saving-strategy.
 */
public interface ImageAnnotationSaveStrategy {
    /**
     * Factory method for creating a saving-strategy.
     *
     * @param type the strategy-type specifying the concrete saving-strategy to create
     * @return the saving-strategy with the provided type
     */
    static ImageAnnotationSaveStrategy createStrategy(Type type) {
        if(type.equals(Type.PASCAL_VOC)) {
            return new PVOCSaveStrategy();
        } else if(type.equals(Type.YOLO)) {
            return new YOLOSaveStrategy();
        } else if(type.equals(Type.JSON)) {
            return new JSONSaveStrategy();
        } else {
            throw new InvalidParameterException();
        }
    }

    /**
     * Saves image-annotations to the provided folder-path.
     *
     * @param annotations the collection of image-annotations to save
     * @param destination the path of the directory to which the annotations will be saved
     * @param progress    the progress-property that will be updated during the saving-operation
     * @return an {@link IOResult} containing information about the finished saving
     */
    ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination, DoubleProperty progress);

    enum Type {
        PASCAL_VOC {
            @Override
            public String toString() {
                return "Pascal VOC";
            }
        },
        YOLO {
            @Override
            public String toString() {
                return "YOLO";
            }
        },
        JSON {
            @Override
            public String toString() {
                return "JSON";
            }
        }
    }
}
