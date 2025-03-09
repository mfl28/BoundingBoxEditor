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

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.github.mfl28.boundingboxeditor.model.data.BoundingBoxData;
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.io.data.CSVRow;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.DoubleProperty;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saving-strategy to export annotations to a CSV file.
 * <p>
 * The CSVSaveStrategy supports {@link BoundingBoxData} only.
 */
public class CSVSaveStrategy implements ImageAnnotationSaveStrategy {
    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        final int totalNrAnnotations = annotations.imageAnnotations().size();
        final AtomicInteger nrProcessedAnnotations = new AtomicInteger();

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();

        try (var writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            var csvMapper = new CsvMapper();
            var csvSchema = csvMapper.schemaFor(CSVRow.class).withHeader();

            try (var valuesWriter = csvMapper.writer(csvSchema).writeValues(writer)) {
                valuesWriter.writeAll(
                        annotations.imageAnnotations().stream()
                                .flatMap(
                                        imageAnnotation -> {
                                            progress.set(1.0 * nrProcessedAnnotations.getAndIncrement() / totalNrAnnotations);

                                            return imageAnnotation.getBoundingShapeData().stream()
                                                    .filter(BoundingBoxData.class::isInstance)
                                                    .map(boundingShapeData -> Pair.of(imageAnnotation, (BoundingBoxData) boundingShapeData));
                                        })
                                .map(pair -> CSVRow.fromData(pair.getLeft(), pair.getRight()))
                                .toList()
                );
            }
        } catch (IOException e) {
            errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(), e.getMessage()));
        }

        return new ImageAnnotationExportResult(
                errorEntries.isEmpty() ? totalNrAnnotations : 0,
                errorEntries
        );
    }

}
