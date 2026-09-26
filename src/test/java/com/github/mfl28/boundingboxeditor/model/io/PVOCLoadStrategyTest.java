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
import java.util.Set;

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
