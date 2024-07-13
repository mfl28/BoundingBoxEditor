/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CSVSaveStrategyTest {

    /**
     * One image with two annotations gets exported to CSV.
     */
    @Test
    void multipleRectangles() throws IOException {
        ObjectCategory objectCategory = new ObjectCategory("category", Color.YELLOW);

        ImageAnnotation imageAnnotation = new ImageAnnotation(new ImageMetaData("sample.png", "folderName", "url", 100, 200, 0));
        List<BoundingShapeData> boundingShapeDatas = new ArrayList<>();
        boundingShapeDatas.add(new BoundingBoxData(objectCategory, 0,0,0.5,0.5, List.of("tag")));
        boundingShapeDatas.add(new BoundingBoxData(objectCategory, 0,0,0.25,0.25, List.of("tag")));
        imageAnnotation.setBoundingShapeData(boundingShapeDatas);

        ImageAnnotationData annotations = new ImageAnnotationData(List.of(imageAnnotation),
                Map.of("object", 1),
                Map.of("object", objectCategory));
        Path destination = Jimfs.newFileSystem(Configuration.unix()).getPath("annotations.csv");
        ImageAnnotationExportResult save = new CSVSaveStrategy().save(annotations, destination, new SimpleDoubleProperty(0));
        assertTrue(save.getErrorTableEntries().isEmpty());

        String content = Files.readString(destination);
        assertEquals("""
                "name","id","label","xMin","xMax","yMin","yMax"
                "sample.png","0","category","0","50","0","100"
                "sample.png","1","category","0","25","0","50"
                """, content);
    }

    /**
     * Two images with each one annotation gets exported.
     */
    @Test
    void multipleImages() throws IOException {
        ObjectCategory objectCategory = new ObjectCategory("category", Color.YELLOW);

        ImageAnnotation imageAnnotation1 = new ImageAnnotation(
                new ImageMetaData("sample1.png", "folderName", "url", 100, 200, 0),
                List.of(new BoundingBoxData(objectCategory, 0,0,0.5,0.5, List.of("tag"))));

        ImageAnnotation imageAnnotation2 = new ImageAnnotation(
                new ImageMetaData("sample2.png", "folderName", "url", 100, 200, 0),
                List.of(new BoundingBoxData(objectCategory, 0,0,0.25,0.25, List.of("tag"))));

        ImageAnnotationData annotations = new ImageAnnotationData(List.of(imageAnnotation1, imageAnnotation2),
                Map.of("object", 1),
                Map.of("object", objectCategory));
        Path destination = Jimfs.newFileSystem(Configuration.unix()).getPath("annotations.csv");
        ImageAnnotationExportResult save = new CSVSaveStrategy().save(annotations, destination, new SimpleDoubleProperty(0));
        assertTrue(save.getErrorTableEntries().isEmpty());

        String content = Files.readString(destination);
        assertEquals("""
                "name","id","label","xMin","xMax","yMin","yMax"
                "sample1.png","0","category","0","50","0","100"
                "sample2.png","1","category","0","25","0","50"
                """, content);
    }

    /**
     * One image with one annotation should be saved. The annotation uses an unsupported Bounding Shape, so a ErrorTableEntry is expected.
     */
    @Test
    void wrongBoundingShape() throws IOException {
        ObjectCategory objectCategory = new ObjectCategory("category", Color.YELLOW);

        ImageAnnotation imageAnnotation1 = new ImageAnnotation(
                new ImageMetaData("sample1.png", "folderName", "url", 100, 200, 0),
                List.of(new BoundingPolygonData(objectCategory, List.of(0.0, 0.0, 0.5, 0.5), List.of("tag"))));

        ImageAnnotationData annotations = new ImageAnnotationData(List.of(imageAnnotation1),
                Map.of("object", 1),
                Map.of("object", objectCategory));
        Path destination = Jimfs.newFileSystem(Configuration.unix()).getPath("annotations.csv");
        ImageAnnotationExportResult save = new CSVSaveStrategy().save(annotations, destination, new SimpleDoubleProperty(0));
        assertEquals(1, save.getErrorTableEntries().size());

        String content = Files.readString(destination);
        assertEquals("""
                "name","id","label","xMin","xMax","yMin","yMax"
                """, content);
    }
}