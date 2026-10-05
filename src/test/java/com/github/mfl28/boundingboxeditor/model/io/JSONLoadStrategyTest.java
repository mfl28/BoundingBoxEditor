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

import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotation;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class JSONLoadStrategyTest {
    @Test
    void onLoading_WhenEntriesOrPolygonsAreInvalid_ShouldReportThemAndLoadTheRest(@TempDir Path tempDir)
            throws Exception {
        final String json = """
                [
                  {"image": {"fileName": "a.jpg"}, "objects": [
                    {"category": {"name": "cat", "color": "#FF0000"}, "polygon": [0.1, 0.1, 0.5, 0.1, 0.3, 0.4],
                     "tags": []},
                    {"category": {"name": "cat", "color": "#FF0000"}, "polygon": [0.1, "x", 0.5, 0.1, 0.3, 0.4],
                     "tags": []},
                    {"polygon": [0.1, 0.1, 0.5, 0.1, 0.3, 0.4], "tags": []}
                  ]},
                  {"objects": []},
                  {"image": {"fileName": "b.jpg"}}
                ]""";
        final Path file = tempDir.resolve("annotations.json");
        Files.writeString(file, json);

        final ImageAnnotationImportResult result = new JSONLoadStrategy().load(file, Set.of("a.jpg", "b.jpg"),
                new HashMap<>(), new SimpleDoubleProperty(0));

        assertEquals(List.of("a.jpg"), result.getImageAnnotationData().imageAnnotations().stream()
                                             .map(ImageAnnotation::getImageFileName).toList());
        assertEquals(1, result.getImageAnnotationData().imageAnnotations().iterator().next().getBoundingShapeData()
                              .size());

        final List<String> errors = result.getErrorTableEntries().stream()
                                          .map(IOErrorInfoEntry::getErrorDescription).toList();
        assertEquals(4, errors.size(), errors::toString);
        assertTrue(errors.contains("Missing images element."), errors::toString);
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("Missing objects") && error.contains("b.jpg")),
                   errors::toString);
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("Invalid coordinate value(s) in polygon")
                && error.contains("a.jpg")), errors::toString);
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("Missing category")), errors::toString);
    }
}
