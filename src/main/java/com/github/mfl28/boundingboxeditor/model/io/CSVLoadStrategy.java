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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CSVLoadStrategy implements ImageAnnotationLoadStrategy {

    private static boolean filterRow(Set<String> filesToLoad, CSVRow csvRow, List<IOErrorInfoEntry> errorInfoEntries) {
        if (filesToLoad.contains(csvRow.filename())) {
            return true;
        }

        errorInfoEntries.add(new IOErrorInfoEntry(csvRow.filename(),
                "Image " + csvRow.filename() +
                        " does not belong to currently loaded image files."));

        return false;
    }

    private static void updateAnnotations(
            CSVRow csvRow, Map<String, ImageAnnotation> filenameAnnotationMap,
            Map<String, ObjectCategory> categoryNameToCategoryMap,
            Map<String, Integer> categoryNameToShapeCountMap) {
        var filename = csvRow.filename();

        var boundingBoxData = createBoundingBox(csvRow, categoryNameToCategoryMap);

        var imageAnnotation = filenameAnnotationMap.computeIfAbsent(
                filename, key -> new ImageAnnotation(new ImageMetaData(key)));

        imageAnnotation.getBoundingShapeData().add(boundingBoxData);
        categoryNameToShapeCountMap.merge(boundingBoxData.getCategoryName(), 1, Integer::sum);
    }

    private static void validateBounds(CSVRow csvRow) {
        if (csvRow.width() <= 0 || csvRow.height() <= 0) {
            throw new InvalidAnnotationFormatException("Invalid image size " + csvRow.width() + "x"
                    + csvRow.height() + ".");
        }

        if (csvRow.xMin() < 0 || csvRow.xMin() > csvRow.xMax() || csvRow.xMax() > csvRow.width()
                || csvRow.yMin() < 0 || csvRow.yMin() > csvRow.yMax()
                || csvRow.yMax() > csvRow.height()) {
            throw new InvalidAnnotationFormatException("Invalid bounding-box bounds (xmin=" + csvRow.xMin()
                    + ", ymin=" + csvRow.yMin() + ", xmax=" + csvRow.xMax() + ", ymax=" + csvRow.yMax()
                    + ") for the given image size " + csvRow.width() + "x" + csvRow.height() + ".");
        }
    }

    private static BoundingBoxData createBoundingBox(CSVRow csvRow, Map<String, ObjectCategory> existingCategoryNameToCategoryMap) {
        validateBounds(csvRow);

        var objectCategory = existingCategoryNameToCategoryMap.computeIfAbsent(csvRow.categoryName(),
                name -> new ObjectCategory(name, ColorUtils.createRandomColor()));

        double xMinRelative = (double) csvRow.xMin() / csvRow.width();
        double yMinRelative = (double) csvRow.yMin() / csvRow.height();
        double xMaxRelative = (double) csvRow.xMax() / csvRow.width();
        double yMaxRelative = (double) csvRow.yMax() / csvRow.height();

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

        // The file is opened here (not by Jackson) so that it's closed even if reading the header already fails,
        // e.g. because of a missing column. Otherwise the file handle leaked (and the file stayed locked on Windows).
        try (InputStream inputStream = Files.newInputStream(path);
             MappingIterator<CSVRow> it = csvMapper
                .readerFor(CSVRow.class)
                .with(csvSchema)
                .without(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE)
                .without(CsvParser.Feature.ALLOW_TRAILING_COMMA)
                .with(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS)
                .with(CsvParser.Feature.FAIL_ON_MISSING_HEADER_COLUMNS)
                .readValues(inputStream)) {
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
                        } catch (InvalidAnnotationFormatException exception) {
                            errorInfoEntries.add(new IOErrorInfoEntry(csvRow.filename(),
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

