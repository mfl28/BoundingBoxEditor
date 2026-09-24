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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class YOLOSaveStrategyTest {
    private static final ObjectCategory CATEGORY = new ObjectCategory("catA", Color.YELLOW);

    /**
     * Images whose names only differ by extension (or case) would share an annotation file,
     * so they must be reported as errors instead of silently overwriting each other.
     */
    @Test
    void onSaving_WhenImageFileNamesShareBaseName_ShouldReportClashesAndNotOverwrite(@TempDir Path tempDir)
            throws IOException {
        ImageAnnotationData annotations = createAnnotationData("a.jpg", "a.png", "b.jpg", "C.jpg", "c.png");

        ImageAnnotationExportResult result =
                new YOLOSaveStrategy().save(annotations, tempDir, new SimpleDoubleProperty(0));

        assertEquals(1, result.getNrSuccessfullyProcessedItems());
        assertEquals(List.of("C.jpg", "a.jpg", "a.png", "c.png"),
                result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getSourceName).sorted().toList());
        assertTrue(result.getErrorTableEntries().stream()
                .allMatch(entry -> entry.getErrorDescription().contains("would share the annotation file")));

        assertTrue(Files.exists(tempDir.resolve("b.txt")));
        assertFalse(Files.exists(tempDir.resolve("a.txt")));
        assertFalse(Files.exists(tempDir.resolve("c.txt")));
        assertFalse(Files.exists(tempDir.resolve("C.txt")));
    }

    @Test
    void onSaving_WhenImageFileNameHasNoExtension_ShouldUseFullNameForAnnotationFile(@TempDir Path tempDir)
            throws IOException {
        ImageAnnotationExportResult result = new YOLOSaveStrategy()
                .save(createAnnotationData("image"), tempDir, new SimpleDoubleProperty(0));

        assertTrue(result.getErrorTableEntries().isEmpty());
        assertEquals(1, result.getNrSuccessfullyProcessedItems());
        assertEquals("0 0.25 0.25 0.5 0.5", Files.readString(tempDir.resolve("image.txt")));
    }

    private static ImageAnnotationData createAnnotationData(String... imageFileNames) {
        List<ImageAnnotation> imageAnnotations = Arrays.stream(imageFileNames)
                .map(fileName -> new ImageAnnotation(
                        new ImageMetaData(fileName, "folderName", "url", 100, 100, 0),
                        List.<BoundingShapeData>of(new BoundingBoxData(CATEGORY, 0, 0, 0.5, 0.5, List.of()))))
                .toList();

        return new ImageAnnotationData(imageAnnotations, Map.of(CATEGORY.getName(), imageFileNames.length),
                Map.of(CATEGORY.getName(), CATEGORY));
    }
}
