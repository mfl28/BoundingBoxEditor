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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvReadException;
import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.data.CSVRow;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import javafx.beans.property.DoubleProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CSVLoadStrategy implements ImageAnnotationLoadStrategy {

    private static boolean filterRow(Set<String> filesToLoad, CSVRow csvRow, List<IOErrorInfoEntry> errorInfoEntries) {
        if (filesToLoad.contains(csvRow.getFilename())) {
            return true;
        }

        errorInfoEntries.add(new IOErrorInfoEntry(csvRow.getFilename(),
                "Image " + csvRow.getFilename() +
                        " does not belong to currently loaded image files."));

        return false;
    }

    private static void updateAnnotations(
            CSVRow csvRow, Map<String, ImageAnnotation> filenameAnnotationMap,
            Map<String, ObjectCategory> categoryNameToCategoryMap,
            Map<String, Integer> categoryNameToShapeCountMap) {
        var filename = csvRow.getFilename();

        var imageAnnotation = filenameAnnotationMap.computeIfAbsent(
                filename, key -> new ImageAnnotation(new ImageMetaData(key)));

        var boundingBoxData = createBoundingBox(csvRow, categoryNameToCategoryMap);

        imageAnnotation.getBoundingShapeData().add(boundingBoxData);
        categoryNameToShapeCountMap.merge(boundingBoxData.getCategoryName(), 1, Integer::sum);
    }

    private static BoundingBoxData createBoundingBox(CSVRow csvRow, Map<String, ObjectCategory> existingCategoryNameToCategoryMap) {
        var objectCategory = existingCategoryNameToCategoryMap.computeIfAbsent(csvRow.getCategoryName(),
                name -> new ObjectCategory(name, ColorUtils.createRandomColor()));

        double xMinRelative = (double) csvRow.getXMin() / csvRow.getWidth();
        double yMinRelative = (double) csvRow.getYMin() / csvRow.getHeight();
        double xMaxRelative = (double) csvRow.getXMax() / csvRow.getWidth();
        double yMaxRelative = (double) csvRow.getYMax() / csvRow.getHeight();

        return new BoundingBoxData(
                objectCategory, xMinRelative, yMinRelative, xMaxRelative, yMaxRelative,
                Collections.emptyList());
    }

    @Override
    public ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                            Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                            DoubleProperty progress) throws IOException {
        final Map<String, Integer> categoryNameToBoundingShapesCountMap = new HashMap<>();
        final List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();
        final Map<String, ImageAnnotation> filenameAnnotationMap = new HashMap<>();

        progress.set(0);

        final var csvMapper = new CsvMapper();
        final var csvSchema = csvMapper.schemaFor(CSVRow.class)
                .withHeader()
                .withColumnReordering(true)
                .withStrictHeaders(true);

        try (MappingIterator<CSVRow> it = csvMapper
                .readerFor(CSVRow.class)
                .with(csvSchema)
                .without(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .without(CsvParser.Feature.ALLOW_TRAILING_COMMA)
                .with(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS)
                .with(CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS)
                .readValues(path.toFile())) {
            it.forEachRemaining(csvRow -> {
                        try {
                            if (filterRow(filesToLoad, csvRow, errorInfoEntries)) {
                                updateAnnotations(csvRow, filenameAnnotationMap,
                                        existingCategoryNameToCategoryMap,
                                        categoryNameToBoundingShapesCountMap);
                            }

                        } catch (RuntimeJsonMappingException exception) {
                            errorInfoEntries.add(new IOErrorInfoEntry(path.getFileName().toString(),
                                    exception.getMessage()));
                        }
                    }
            );
        } catch (CsvReadException exception) {
            errorInfoEntries.add(new IOErrorInfoEntry(path.getFileName().toString(),
                    exception.getMessage()));
        }

        var imageAnnotationData = new ImageAnnotationData(
                filenameAnnotationMap.values(), categoryNameToBoundingShapesCountMap,
                existingCategoryNameToCategoryMap);

        progress.set(1.0);

        return new ImageAnnotationImportResult(
                imageAnnotationData.imageAnnotations().size(),
                errorInfoEntries,
                imageAnnotationData
        );
    }

}

