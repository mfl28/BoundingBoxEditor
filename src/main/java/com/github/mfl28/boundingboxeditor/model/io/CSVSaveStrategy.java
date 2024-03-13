/*
 * Copyright (C) 2023 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
 *
 * The CSVSaveStrategy supports {@link BoundingBoxData} only.
 */
public class CSVSaveStrategy implements ImageAnnotationSaveStrategy {
    private static final String FILE_NAME_SERIALIZED_NAME = "name";
    private static final String ID_SERIALIZED_NAME = "id";
    private static final String LABEL_SERIALIZED_NAME = "label";
    private static final String MIN_X_SERIALIZED_NAME = "xMin";
    private static final String MAX_X_SERIALIZED_NAME = "xMax";
    private static final String MIN_Y_SERIALIZED_NAME = "yMin";
    private static final String MAX_Y_SERIALIZED_NAME = "yMax";
    public static final String UNSUPPORTED_BOUNDING_SHAPE = "CSV can export Rectangles only";

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        final int totalNrAnnotations = annotations.imageAnnotations().size();
        int nrProcessedAnnotations = 0;

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();

        try(ICSVWriter writer = new CSVWriterBuilder(Files.newBufferedWriter(destination, StandardCharsets.UTF_8)).build()) {
            String[] header = {FILE_NAME_SERIALIZED_NAME, ID_SERIALIZED_NAME, LABEL_SERIALIZED_NAME, MIN_X_SERIALIZED_NAME, MAX_X_SERIALIZED_NAME, MIN_Y_SERIALIZED_NAME, MAX_Y_SERIALIZED_NAME};
            writer.writeNext(header);
            for (ImageAnnotation imageAnnotation : annotations.imageAnnotations()) {
                for (BoundingShapeData boundingShapeData : imageAnnotation.getBoundingShapeData()) {
                    if (boundingShapeData instanceof BoundingBoxData boundingBoxData) {
                        double xMin = imageAnnotation.getImageMetaData().getImageWidth() * boundingBoxData.getXMinRelative();
                        double xMax = imageAnnotation.getImageMetaData().getImageWidth() * boundingBoxData.getXMaxRelative();
                        double yMin = imageAnnotation.getImageMetaData().getImageHeight() * boundingBoxData.getYMinRelative();
                        double yMax = imageAnnotation.getImageMetaData().getImageHeight() * boundingBoxData.getYMaxRelative();
                        String[] line = { imageAnnotation.getImageFileName(), String.valueOf(nrProcessedAnnotations), boundingShapeData.getCategoryName(), String.valueOf((int) xMin), String.valueOf((int) xMax), String.valueOf((int) yMin), String.valueOf((int) yMax)};
                        writer.writeNext(line);
                    } else {
                        errorEntries.add(new IOErrorInfoEntry(imageAnnotation.getImageFileName(), UNSUPPORTED_BOUNDING_SHAPE));
                    }
                    progress.set(1.0 * nrProcessedAnnotations++ / totalNrAnnotations);
                }
            }
        } catch(IOException e) {
            errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(), e.getMessage()));
        }

        return new ImageAnnotationExportResult(
                errorEntries.isEmpty() ? totalNrAnnotations : 0,
                errorEntries
        );
    }
}
