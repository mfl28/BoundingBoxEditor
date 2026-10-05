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

import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.Tag;
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

@Tag("unit")
class PVOCLoadStrategyTest {
    /**
     * A malformed number must only affect the file (or object) it is in, not the whole import.
     */
    @Test
    void onLoading_WhenNumbersAreMalformed_ShouldReportThemAndLoadTheOtherFiles(@TempDir Path tempDir)
            throws IOException {
        Files.writeString(tempDir.resolve("valid.xml"), annotation("valid.jpg", "3", "0", "10"));
        Files.writeString(tempDir.resolve("bad-flag.xml"), annotation("bad-flag.jpg", "3", "yes", "10"));
        Files.writeString(tempDir.resolve("bad-coordinate.xml"), annotation("bad-coordinate.jpg", "3", "0", ""));
        Files.writeString(tempDir.resolve("bad-depth.xml"), annotation("bad-depth.jpg", "three", "0", "10"));

        final ImageAnnotationImportResult result = new PVOCLoadStrategy().load(tempDir,
                Set.of("valid.jpg", "bad-flag.jpg", "bad-coordinate.jpg", "bad-depth.jpg"), new HashMap<>(),
                new SimpleDoubleProperty(0));

        assertEquals(List.of("valid.jpg"), result.getImageAnnotationData().imageAnnotations().stream()
                                                 .map(annotation -> annotation.getImageFileName()).toList());
        assertEquals(List.of("bad-coordinate.xml", "bad-depth.xml", "bad-flag.xml"),
                     result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getSourceName).sorted().toList());
        assertTrue(result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getErrorDescription)
                         .anyMatch(message -> message.contains("\"yes\"") && message.contains("truncated")),
                   () -> result.getErrorTableEntries().toString());
    }

    @Test
    void onLoading_WhenObjectHasActions_ShouldImportTheSetOnesAsTags(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("actions.xml"), annotationWithObject("actions.jpg", """
                <name>person</name>
                <actions>
                    <jumping>1</jumping>
                    <running>0</running>
                </actions>
                <bndbox><xmin>10</xmin><ymin>10</ymin><xmax>50</xmax><ymax>50</ymax></bndbox>
                """));

        final ImageAnnotationImportResult result = new PVOCLoadStrategy().load(tempDir, Set.of("actions.jpg"),
                new HashMap<>(), new SimpleDoubleProperty(0));

        assertTrue(result.getErrorTableEntries().isEmpty(), () -> result.getErrorTableEntries().toString());
        assertEquals(List.of("action: jumping"), result.getImageAnnotationData().imageAnnotations().iterator().next()
                                                     .getBoundingShapeData().getFirst().getTags());
    }

    @Test
    void onLoading_WhenObjectsAreInvalid_ShouldReportEachFile(@TempDir Path tempDir) throws IOException {
        final String box = "<bndbox><xmin>10</xmin><ymin>10</ymin><xmax>50</xmax><ymax>50</ymax></bndbox>";
        final String polygon = "<polygon><x>10</x><y>10</y><x>50</x><y>10</y><x>30</x><y>40</y></polygon>";
        final Map<String, String> objects = Map.of(
                "valid", "<name>cat</name>" + box,
                "both", "<name>cat</name>" + box + polygon,
                "neither", "<name>cat</name>",
                "incomplete-box", "<name>cat</name><bndbox><xmin>10</xmin><ymin>10</ymin><xmax>50</xmax></bndbox>",
                "uneven-polygon", "<name>cat</name><polygon><x>10</x><y>10</y><x>50</x></polygon>",
                "blank-name", "<name> </name>" + box,
                "box-outside", "<name>cat</name><bndbox><xmin>10</xmin><ymin>10</ymin><xmax>150</xmax><ymax>50</ymax>"
                        + "</bndbox>",
                "polygon-outside", "<name>cat</name><polygon><x>10</x><y>10</y><x>50</x><y>10</y><x>30</x><y>140</y>"
                        + "</polygon>");

        for(Map.Entry<String, String> object : objects.entrySet()) {
            Files.writeString(tempDir.resolve(object.getKey() + ".xml"),
                              annotationWithObject(object.getKey() + ".jpg", object.getValue()));
        }

        // Refers to an image that is not loaded.
        Files.writeString(tempDir.resolve("other.xml"), annotationWithObject("other.jpg", objects.get("valid")));

        final ImageAnnotationImportResult result = new PVOCLoadStrategy().load(tempDir,
                objects.keySet().stream().map(name -> name + ".jpg").collect(Collectors.toSet()), new HashMap<>(),
                new SimpleDoubleProperty(0));

        assertEquals(List.of("valid.jpg"), result.getImageAnnotationData().imageAnnotations().stream()
                                                 .map(annotation -> annotation.getImageFileName()).toList());
        final Map<String, String> errors = result.getErrorTableEntries().stream()
                .collect(Collectors.toMap(IOErrorInfoEntry::getSourceName, IOErrorInfoEntry::getErrorDescription));
        assertEquals(8, errors.size(), errors::toString);
        assertTrue(errors.get("both.xml").contains("Contains \"bndbox\"- and \"polygon\"-elements."), errors::toString);
        assertTrue(errors.get("neither.xml").contains("Missing \"bndbox\"- or \"polygon\"-element."),
                   errors::toString);
        assertTrue(errors.get("incomplete-box.xml").contains("Missing element: ymax"), errors::toString);
        assertTrue(errors.get("uneven-polygon.xml").contains("Invalid polygon element."), errors::toString);
        assertTrue(errors.get("blank-name.xml").contains("Blank object name"), errors::toString);
        assertTrue(errors.get("box-outside.xml").contains("Invalid bounding-box bounds"), errors::toString);
        assertTrue(errors.get("polygon-outside.xml").contains("Invalid bounding-polygon point coordinates"),
                   errors::toString);
        assertTrue(errors.get("other.xml").contains("does not belong to the currently loaded images"),
                   errors::toString);
    }

    private static String annotationWithObject(String fileName, String object) {
        return """
                <annotation>
                    <folder>images</folder>
                    <filename>%s</filename>
                    <size>
                        <width>100</width>
                        <height>100</height>
                        <depth>3</depth>
                    </size>
                    <object>
                        %s
                    </object>
                </annotation>
                """.formatted(fileName, object);
    }

    private static String annotation(String fileName, String depth, String truncated, String xMin) {
        return """
                <annotation>
                    <folder>images</folder>
                    <filename>%s</filename>
                    <size>
                        <width>100</width>
                        <height>100</height>
                        <depth>%s</depth>
                    </size>
                    <object>
                        <name>cat</name>
                        <truncated>%s</truncated>
                        <bndbox>
                            <xmin>%s</xmin>
                            <ymin>10</ymin>
                            <xmax>50</xmax>
                            <ymax>50</ymax>
                        </bndbox>
                    </object>
                </annotation>
                """.formatted(fileName, depth, truncated, xMin);
    }
}
