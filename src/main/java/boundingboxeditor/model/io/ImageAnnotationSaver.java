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

import boundingboxeditor.model.data.ImageAnnotationData;
import boundingboxeditor.model.io.results.IOResult;
import boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.nio.file.Path;

/**
 * Responsible for saving image-annotations.
 * This class wraps an object of a class that implements {@link ImageAnnotationSaveStrategy} interface and
 * this class determines the actual way of saving the annotations (e.g. different file-types).
 */
public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy saveStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    /**
     * Creates a new image-annotation saver using a {@link ImageAnnotationSaveStrategy} specified
     * by a {@link ImageAnnotationSaveStrategy.Type}.
     *
     * @param strategy the type specifying the concrete strategy to use for saving
     */
    public ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type strategy) {
        saveStrategy = ImageAnnotationSaveStrategy.createStrategy(strategy);
    }

    /**
     * Saves the provided annotation as specified by the wrapped {@link ImageAnnotationSaveStrategy}.
     *
     * @param annotations the annotations to save
     * @param destination the path of the destination folder
     * @return an {@link IOResult} containing information about the finished saving
     */
    public ImageAnnotationExportResult save(final ImageAnnotationData annotations, final Path destination)
            throws Exception {
        return IOOperationTimer.time(() -> saveStrategy.save(annotations, destination, progress));
    }

    /**
     * Returns a property representing the progress of the saving-operation which can be bound
     * to update the progress of a {@link javafx.concurrent.Service} performing the saving.
     *
     * @return the progress property
     */
    public DoubleProperty progressProperty() {
        return progress;
    }
}
