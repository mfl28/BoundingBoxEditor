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

import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingMaskData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingPolygonData;
import com.github.mfl28.boundingboxeditor.model.data.BoundingShapeData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsible for saving image-annotations.
 * This class wraps an object of a class that implements {@link ImageAnnotationSaveStrategy} interface and
 * this class determines the actual way of saving the annotations (e.g. different file-types).
 */
public class ImageAnnotationSaver {
    private final ImageAnnotationSaveStrategy.Type type;
    private final ImageAnnotationSaveStrategy saveStrategy;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    /**
     * Creates a new image-annotation saver using a {@link ImageAnnotationSaveStrategy} specified
     * by a {@link ImageAnnotationSaveStrategy.Type}.
     *
     * @param strategy the type specifying the concrete strategy to use for saving
     */
    public ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type strategy) {
        this.type = strategy;
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
        if(type.supportsMasks()) {
            return IOOperationTimer.time(() -> saveStrategy.save(annotations, destination, progress));
        }

        // Formats without masks get the annotations without them, and the report says how many were left out.
        final AtomicInteger nrRemovedMasks = new AtomicInteger();
        final List<ImageAnnotation> annotationsWithoutMasks = annotations.imageAnnotations().stream()
                .map(annotation -> new ImageAnnotation(annotation.getImageMetaData(),
                                                       withoutMasks(annotation.getBoundingShapeData(),
                                                                    nrRemovedMasks)))
                .toList();

        final ImageAnnotationExportResult result = IOOperationTimer.time(() -> saveStrategy.save(
                new ImageAnnotationData(annotationsWithoutMasks, annotations.categoryNameToBoundingShapeCountMap(),
                                        annotations.categoryNameToCategoryMap()),
                destination, progress));

        if(nrRemovedMasks.get() == 0) {
            return result;
        }

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>(result.getErrorTableEntries());
        errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(),
                String.format("%d mask(s) were not saved, because the %s format can't contain masks (masks nested "
                                      + "in them were left out too). Export as COCO or PNG masks to save them.",
                              nrRemovedMasks.get(), type)));

        final ImageAnnotationExportResult resultWithNote =
                new ImageAnnotationExportResult(result.getNrSuccessfullyProcessedItems(), errorEntries);
        resultWithNote.setTimeTakenInMilliseconds(result.getTimeTakenInMilliseconds());
        return resultWithNote;
    }

    /**
     * Returns the shapes without the masks (and the shapes nested in masks). The provided shapes are not changed:
     * shapes that contain masks among their parts are copied.
     *
     * @param shapes       the shapes
     * @param nrRemoved    counts the removed masks (including the ones nested in removed masks)
     * @return the shapes without masks
     */
    static List<BoundingShapeData> withoutMasks(List<BoundingShapeData> shapes, AtomicInteger nrRemoved) {
        final List<BoundingShapeData> result = new ArrayList<>(shapes.size());

        for(BoundingShapeData shape : shapes) {
            if(shape instanceof BoundingMaskData) {
                nrRemoved.addAndGet((int) shape.flatten().filter(BoundingMaskData.class::isInstance).count());
                continue;
            }

            if(shape.flatten().noneMatch(BoundingMaskData.class::isInstance)) {
                result.add(shape);
                continue;
            }

            final BoundingShapeData copy = switch(shape) {
                case BoundingBoxData box -> new BoundingBoxData(box.getCategory(), box.getXMinRelative(),
                        box.getYMinRelative(), box.getXMaxRelative(), box.getYMaxRelative(), box.getTags());
                case BoundingPolygonData polygon -> new BoundingPolygonData(polygon.getCategory(),
                        polygon.getRelativePointsInImage(), polygon.getTags());
                case BoundingMaskData mask -> mask;
            };

            copy.setParts(withoutMasks(shape.getParts(), nrRemoved));
            result.add(copy);
        }

        return result;
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
