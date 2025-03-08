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

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSVSaveStrategyTest {

    /**
     * One image with two annotations gets exported to CSV.
     */
    @Test
    void testCSVSaving(@TempDir Path tempDir) throws IOException {
        var objectCategory1 = new ObjectCategory("catA", Color.YELLOW);
        var objectCategory2 = new ObjectCategory("catB", Color.BLUE);
        var imageMetaData1 = new ImageMetaData("sample1.png", "folderName", "url", 100, 200, 0);
        var imageMetaData2 = new ImageMetaData("sample2.png", "folderName", "url", 400, 300, 0);

        List<BoundingShapeData> boundingShapeData1 = List.of(
                new BoundingBoxData(objectCategory1, 0, 0, 0.5, 0.5, List.of("tag")),
                new BoundingBoxData(objectCategory2, 0, 0, 0.25, 0.25, List.of("tag")),
                new BoundingPolygonData(objectCategory1, List.of(0.0, 0.0, 0.5, 0.5), Collections.emptyList())
        );

        var imageAnnotation1 = new ImageAnnotation(imageMetaData1, boundingShapeData1);

        List<BoundingShapeData> boundingShapeData2 = List.of(
                new BoundingBoxData(objectCategory2, 0.1, 0, 0.5, 0.2, List.of("tag"))
        );

        var imageAnnotation2 = new ImageAnnotation(imageMetaData2, boundingShapeData2);

        ImageAnnotationData annotations = new ImageAnnotationData(
                List.of(imageAnnotation1, imageAnnotation2),
                Map.of("catA", 2, "catB", 2),
                Map.of("catA", objectCategory1, "catB", objectCategory2));

        Path destination = tempDir.resolve("annotations.csv");

        ImageAnnotationExportResult save = new CSVSaveStrategy().save(annotations, destination, new SimpleDoubleProperty(0));

        assertTrue(save.getErrorTableEntries().isEmpty());

        String content = Files.readString(destination);

        assertEquals("""
                filename,width,height,class,xmin,ymin,xmax,ymax
                sample1.png,100,200,catA,0,0,50,100
                sample1.png,100,200,catB,0,0,25,50
                sample2.png,400,300,catB,40,0,200,60
                """, content);
    }
}