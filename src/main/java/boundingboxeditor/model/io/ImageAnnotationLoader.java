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
package boundingboxeditor.model.io;

import boundingboxeditor.model.data.ObjectCategory;
import boundingboxeditor.model.io.results.IOResult;
import boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for loading image-annotation files from a directory into the model-component of
 * the program. This class wraps an object of a class that implements {@link ImageAnnotationLoadStrategy} interface and
 * this class determines the actual way of loading the annotations (e.g. different file-types).
 */
public class ImageAnnotationLoader {
    private final ImageAnnotationLoadStrategy loadStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    /**
     * Creates a new image-annotation loader using a {@link ImageAnnotationLoadStrategy} specified
     * by a {@link ImageAnnotationLoadStrategy.Type}.
     *
     * @param strategy the type specifying the concrete strategy to use for the loading
     */
    public ImageAnnotationLoader(final ImageAnnotationLoadStrategy.Type strategy) {
        loadStrategy = ImageAnnotationLoadStrategy.createStrategy(strategy);
    }

    /**
     * Loads image-annotation files as specified by the wrapped {@link ImageAnnotationLoadStrategy}
     * into the model-component of the program.
     *
     * @param annotationsFolderPath the path of the folder containing the image-annotation files
     * @return an {@link IOResult} containing information about the finished loading
     * @throws IOException if the directory denoted by the path could not be opened
     */
    public ImageAnnotationImportResult load(final Path annotationsFolderPath, final Set<String> filesToLoad,
                                            final Map<String, ObjectCategory> existingCategoryNameToCategoryMap)
            throws Exception {
        return IOOperationTimer.time(() -> loadStrategy
                .load(annotationsFolderPath, filesToLoad, existingCategoryNameToCategoryMap, progress));
    }

    /**
     * Returns a property representing the progress of the loading-operation which can be bound
     * to update the progress of a {@link javafx.concurrent.Service} performing the loading.
     *
     * @return the progress property
     */
    public DoubleProperty progressProperty() {
        return progress;
    }
}
