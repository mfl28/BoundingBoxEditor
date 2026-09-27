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

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Saves masks and polygons as PNG masks (see {@link PNGMasks}), one object mask and one category mask per image.
 * Polygons are filled; boxes are not saved. Where objects overlap, a pixel belongs to the one saved later (nested
 * objects come after the ones containing them).
 */
public class PNGMaskSaveStrategy implements ImageAnnotationSaveStrategy {
    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        progress.set(0);
        final Queue<IOErrorInfoEntry> errorEntries = new ConcurrentLinkedQueue<>();
        final List<ImageAnnotation> imageAnnotations = List.copyOf(annotations.imageAnnotations());
        final List<String> categoryNames = imageAnnotations.stream()
                .flatMap(annotation -> annotation.getBoundingShapeData().stream())
                .flatMap(BoundingShapeData::flatten)
                .filter(shape -> !(shape instanceof BoundingBoxData))
                .map(BoundingShapeData::getCategoryName)
                .distinct()
                .sorted()
                .toList();

        if(categoryNames.size() > PNGMasks.MAXIMUM_INDEX) {
            return new ImageAnnotationExportResult(0, List.of(new IOErrorInfoEntry(
                    destination.getFileName().toString(), "PNG masks can contain at most "
                            + PNGMasks.MAXIMUM_INDEX + " categories, but the masks and polygons have "
                            + categoryNames.size() + ".")));
        }

        final Map<String, Integer> categoryIndices = new HashMap<>();

        for(int i = 0; i < categoryNames.size(); ++i) {
            categoryIndices.put(categoryNames.get(i), i + 1);
        }

        try {
            Files.createDirectories(destination.resolve(PNGMasks.OBJECT_FOLDER_NAME));
            Files.createDirectories(destination.resolve(PNGMasks.CLASS_FOLDER_NAME));
            Files.write(destination.resolve(PNGMasks.CLASSES_FILE_NAME), categoryNames, StandardCharsets.UTF_8);
        } catch(IOException e) {
            return new ImageAnnotationExportResult(0, List.of(new IOErrorInfoEntry(
                    destination.getFileName().toString(), e.getMessage())));
        }

        // Images whose names only differ in the extension would share their mask files.
        final Map<String, List<String>> baseNameToFileNames = imageAnnotations.stream()
                .map(ImageAnnotation::getImageFileName)
                .collect(Collectors.groupingBy(FilenameUtils::getBaseName));
        final AtomicInteger nrSaved = new AtomicInteger();
        final AtomicInteger nrProcessed = new AtomicInteger();
        final AtomicInteger nrSkippedBoxes = new AtomicInteger();

        imageAnnotations.parallelStream().forEach(annotation -> {
            final String fileName = annotation.getImageFileName();
            final List<String> sameBaseName = baseNameToFileNames.get(FilenameUtils.getBaseName(fileName));

            if(sameBaseName.size() > 1) {
                errorEntries.add(new IOErrorInfoEntry(fileName, "Images " + String.join(", ", sameBaseName)
                        + " would share the mask file \"" + FilenameUtils.getBaseName(fileName)
                        + PNGMasks.PNG_EXTENSION + "\"; none of them were saved."));
            } else {
                try {
                    saveImageMasks(annotation, destination, categoryIndices, nrSkippedBoxes, errorEntries);
                    nrSaved.incrementAndGet();
                } catch(IOException e) {
                    errorEntries.add(new IOErrorInfoEntry(fileName, e.getMessage()));
                }
            }

            progress.set(1.0 * nrProcessed.incrementAndGet() / imageAnnotations.size());
        });

        final List<IOErrorInfoEntry> errors = new ArrayList<>(errorEntries);

        if(nrSkippedBoxes.get() > 0) {
            errors.add(new IOErrorInfoEntry(destination.getFileName().toString(), nrSkippedBoxes.get()
                    + " bounding box(es) were not saved: PNG masks only contain masks and polygons."));
        }

        progress.set(1.0);
        return new ImageAnnotationExportResult(nrSaved.get(), errors);
    }

    private static void saveImageMasks(ImageAnnotation annotation, Path destination,
                                       Map<String, Integer> categoryIndices, AtomicInteger nrSkippedBoxes,
                                       Queue<IOErrorInfoEntry> errorEntries) throws IOException {
        final int width = (int) Math.round(annotation.getOrientedImageWidth());
        final int height = (int) Math.round(annotation.getOrientedImageHeight());

        if(width <= 0 || height <= 0) {
            throw new IOException("The size of the image is unknown.");
        }

        final byte[] objects = new byte[width * height];
        final byte[] classes = new byte[width * height];
        int objectIndex = 0;

        for(BoundingShapeData shape : annotation.getBoundingShapeData().stream()
                                                .flatMap(BoundingShapeData::flatten).toList()) {
            final MaskBitmap mask = switch(shape) {
                case BoundingBoxData _ -> null;
                case BoundingPolygonData polygon -> {
                    final MutableMask filled = new MutableMask(width, height);
                    filled.fillPolygon(polygon.getAbsolutePointsInImage(width, height).stream()
                                              .mapToDouble(Double::doubleValue).toArray(), true);
                    yield filled.toBitmap();
                }
                case BoundingMaskData maskData -> maskData.getMaskForImageSize(width, height);
            };

            if(mask == null) {
                nrSkippedBoxes.incrementAndGet();
                continue;
            }

            if(mask.isEmpty()) {
                continue;
            }

            ++objectIndex;
            final byte objectValue = (byte) Math.min(objectIndex, PNGMasks.MAXIMUM_INDEX);
            final byte classValue = (byte) (int) categoryIndices.get(shape.getCategoryName());

            for(int y = mask.getMinY(); y < mask.getMinY() + mask.getHeight(); ++y) {
                for(int x = mask.getMinX(); x < mask.getMinX() + mask.getWidth(); ++x) {
                    if(mask.get(x, y)) {
                        objects[y * width + x] = objectValue;
                        classes[y * width + x] = classValue;
                    }
                }
            }
        }

        final String maskFileName = FilenameUtils.getBaseName(annotation.getImageFileName()) + PNGMasks.PNG_EXTENSION;
        PNGMasks.write(classes, width, height, destination.resolve(PNGMasks.CLASS_FOLDER_NAME).resolve(maskFileName));

        if(objectIndex > PNGMasks.MAXIMUM_INDEX) {
            errorEntries.add(new IOErrorInfoEntry(annotation.getImageFileName(), "The image has " + objectIndex
                    + " masks and polygons, but an object mask can contain at most " + PNGMasks.MAXIMUM_INDEX
                    + "; only its category mask was saved."));
            return;
        }

        PNGMasks.write(objects, width, height, destination.resolve(PNGMasks.OBJECT_FOLDER_NAME).resolve(maskFileName));
    }
}
