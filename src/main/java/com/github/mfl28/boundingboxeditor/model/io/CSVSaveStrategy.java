/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import javafx.beans.property.DoubleProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Saving-strategy to export annotations to a CSV file.
 * <p>
 * The CSVSaveStrategy supports {@link BoundingBoxData} only.
 */
public class CSVSaveStrategy implements ImageAnnotationSaveStrategy {
    private static final String FILE_NAME_SERIALIZED_NAME = "filename";
    private static final String WIDTH_SERIALIZED_NAME = "width";
    private static final String HEIGHT_SERIALIZED_NAME = "height";
    private static final String CLASS_SERIALIZED_NAME = "class";
    private static final String MIN_X_SERIALIZED_NAME = "xmin";
    private static final String MAX_X_SERIALIZED_NAME = "xmax";
    private static final String MIN_Y_SERIALIZED_NAME = "ymin";
    private static final String MAX_Y_SERIALIZED_NAME = "ymax";

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        final int totalNrAnnotations = annotations.imageAnnotations().size();
        int nrProcessedAnnotations = 0;

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();

        try (ICSVWriter writer = new CSVWriterBuilder(Files.newBufferedWriter(destination, StandardCharsets.UTF_8)).build()) {
            String[] header = {
                    FILE_NAME_SERIALIZED_NAME,
                    WIDTH_SERIALIZED_NAME,
                    HEIGHT_SERIALIZED_NAME,
                    CLASS_SERIALIZED_NAME,
                    MIN_X_SERIALIZED_NAME,
                    MIN_Y_SERIALIZED_NAME,
                    MAX_X_SERIALIZED_NAME,
                    MAX_Y_SERIALIZED_NAME};

            writer.writeNext(header);

            for (var imageAnnotation : annotations.imageAnnotations()) {
                for (var boundingShapeData : imageAnnotation.getBoundingShapeData()) {
                    if (boundingShapeData instanceof BoundingBoxData boundingBoxData) {
                        writer.writeNext(buildLine(imageAnnotation, boundingBoxData));
                    }

                    progress.set(1.0 * nrProcessedAnnotations++ / totalNrAnnotations);
                }
            }
        } catch (IOException e) {
            errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(), e.getMessage()));
        }

        return new ImageAnnotationExportResult(
                errorEntries.isEmpty() ? totalNrAnnotations : 0,
                errorEntries
        );
    }

    private static String[] buildLine(ImageAnnotation imageAnnotation, BoundingBoxData boundingBoxData) {
        double imageWidth = imageAnnotation.getImageMetaData().getImageWidth();
        double imageHeight = imageAnnotation.getImageMetaData().getImageHeight();

        var bounds = boundingBoxData.getAbsoluteBoundsInImage(imageWidth, imageHeight);

        return new String[]{
                imageAnnotation.getImageFileName(),
                String.valueOf((int) Math.round(imageWidth)),
                String.valueOf((int) Math.round(imageHeight)),
                boundingBoxData.getCategoryName(),
                String.valueOf((int) Math.round(bounds.getMinX())),
                String.valueOf((int) Math.round(bounds.getMinY())),
                String.valueOf((int) Math.round(bounds.getMaxX())),
                String.valueOf((int) Math.round(bounds.getMaxY()))
        };
    }
}
