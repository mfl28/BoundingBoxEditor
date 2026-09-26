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
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CSVLoadStrategyTest {

    /**
     * Rows with an invalid image size or with bounds outside the image must be reported instead of being
     * imported with infinite, NaN or out-of-range coordinates.
     */
    @Test
    void onLoading_WhenRowsHaveInvalidBounds_ShouldReportThemAndLoadOnlyValidRows(@TempDir Path tempDir)
            throws IOException {
        Path csvFile = tempDir.resolve("annotations.csv");
        Files.writeString(csvFile, """
                filename,width,height,class,xmin,ymin,xmax,ymax
                valid.jpg,100,200,catA,10,20,50,100
                valid.jpg,100,200,catA,10,20,10,20
                zero-size.jpg,0,200,catB,0,0,0,0
                swapped.jpg,100,200,catC,50,20,10,100
                outside.jpg,100,200,catD,10,20,150,100
                """);

        ImageAnnotationImportResult result = new CSVLoadStrategy().load(csvFile,
                Set.of("valid.jpg", "zero-size.jpg", "swapped.jpg", "outside.jpg"),
                new HashMap<>(), new SimpleDoubleProperty(0));

        assertEquals(List.of("outside.jpg", "swapped.jpg", "zero-size.jpg"),
                result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getSourceName).sorted().toList());

        assertEquals(1, result.getNrSuccessfullyProcessedItems());

        ImageAnnotation annotation = result.getImageAnnotationData().imageAnnotations().iterator().next();
        assertEquals("valid.jpg", annotation.getImageFileName());
        assertEquals(List.of(new BoundingBox(0.1, 0.1, 0.4, 0.4), new BoundingBox(0.1, 0.1, 0, 0)),
                annotation.getBoundingShapeData().stream()
                        .map(shape -> ((BoundingBoxData) shape).getRelativeBoundsInImage())
                        .toList());

        Map<String, ObjectCategory> categories = result.getImageAnnotationData().categoryNameToCategoryMap();
        assertEquals(Set.of("catA"), categories.keySet());
    }

    @Test
    void onLoading_WhenRequiredColumnIsMissing_ShouldReportErrorAndLoadNothing(@TempDir Path tempDir)
            throws IOException {
        Path csvFile = tempDir.resolve("annotations.csv");
        Files.writeString(csvFile, """
                filename,width,height,class,xmin,ymin,xmax
                valid.jpg,100,200,catA,10,20,50
                """);

        ImageAnnotationImportResult result = new CSVLoadStrategy().load(csvFile, Set.of("valid.jpg"),
                new HashMap<>(), new SimpleDoubleProperty(0));

        assertEquals(0, result.getNrSuccessfullyProcessedItems());
        assertEquals(List.of("annotations.csv"),
                result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getSourceName).toList());
        assertTrue(result.getErrorTableEntries().getFirst().getErrorDescription().contains("ymax"),
                () -> result.getErrorTableEntries().getFirst().getErrorDescription());
    }

    /**
     * A row whose values can't be read must be reported, and the rows after it must still be imported.
     */
    @Test
    void onLoading_WhenRowHasMalformedValue_ShouldReportItAndLoadTheOtherRows(@TempDir Path tempDir)
            throws IOException {
        Path csvFile = tempDir.resolve("annotations.csv");
        Files.writeString(csvFile, """
                filename,width,height,class,xmin,ymin,xmax,ymax
                first.jpg,100,200,catA,10,20,50,100
                broken.jpg,100,200,catA,abc,20,50,100
                last.jpg,100,200,catA,10,20,50,100
                """);

        ImageAnnotationImportResult result = new CSVLoadStrategy().load(csvFile,
                Set.of("first.jpg", "broken.jpg", "last.jpg"), new HashMap<>(), new SimpleDoubleProperty(0));

        assertEquals(Set.of("first.jpg", "last.jpg"), result.getImageAnnotationData().imageAnnotations().stream()
                .map(ImageAnnotation::getImageFileName).collect(Collectors.toSet()));
        assertEquals(1, result.getErrorTableEntries().size());
        assertEquals("annotations.csv", result.getErrorTableEntries().getFirst().getSourceName());
        assertTrue(result.getErrorTableEntries().getFirst().getErrorDescription().contains("line 3"),
                () -> result.getErrorTableEntries().getFirst().getErrorDescription());
    }

    /**
     * Other tools often write fractional pixel coordinates.
     */
    @Test
    void onLoading_WhenCoordinatesHaveDecimals_ShouldLoadThem(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("annotations.csv");
        Files.writeString(csvFile, """
                filename,width,height,class,xmin,ymin,xmax,ymax
                valid.jpg,100,200,catA,10.4,20.6,50.5,100
                """);

        ImageAnnotationImportResult result = new CSVLoadStrategy().load(csvFile, Set.of("valid.jpg"),
                new HashMap<>(), new SimpleDoubleProperty(0));

        assertEquals(List.of(), result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getErrorDescription)
                .toList());
        assertEquals(1, result.getNrSuccessfullyProcessedItems());
    }
}
