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

import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.DoubleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.Map;
import java.util.Set;

/**
 * The interface of an image annotation loading-strategy.
 */
public interface ImageAnnotationLoadStrategy {
    /**
     * Factory method for creating a concrete loading-strategy.
     *
     * @param type the strategy-type specifying the concrete loading-strategy to create
     * @return the loading-strategy with the provided type
     */
    static ImageAnnotationLoadStrategy createStrategy(Type type) {
        if(type.equals(Type.PASCAL_VOC)) {
            return new PVOCLoadStrategy();
        } else if(type.equals(Type.YOLO)) {
            return new YOLOLoadStrategy();
        } else if(type.equals(Type.JSON)) {
            return new JSONLoadStrategy();
        } else {
            throw new InvalidParameterException();
        }
    }

    /**
     * Loads image-annotation files from the provided directory path into
     * the program.
     *
     * @param path        the path of the directory containing the image-annotation files
     * @param filesToLoad the set of files whose annotations can be imported
     * @param progress    the progress-property that will be updated during the loading-operation
     * @return an {@link IOResult} containing information about the finished loading
     * @throws IOException if the directory denoted by the path could not be opened
     */
    ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                     Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                     DoubleProperty progress) throws IOException;

    enum Type {PASCAL_VOC, YOLO, JSON}

    @SuppressWarnings("serial")
    class InvalidAnnotationFormatException extends RuntimeException {
        InvalidAnnotationFormatException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    class AnnotationToNonExistentImageException extends RuntimeException {
        AnnotationToNonExistentImageException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    class AnnotationAssociationException extends RuntimeException {
        AnnotationAssociationException(String message) { super(message); }
    }
}
