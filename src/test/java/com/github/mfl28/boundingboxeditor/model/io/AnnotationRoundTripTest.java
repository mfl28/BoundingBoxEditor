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
import com.github.mfl28.boundingboxeditor.model.data.ImageAnnotationData;
import com.github.mfl28.boundingboxeditor.model.data.ImageMetaData;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests: annotations loaded from the reference files and saved again must reproduce the
 * reference files byte for byte. This mirrors the export tests in ControllerTests without the UI, so the
 * annotation I/O code can be refactored safely.
 */
@Tag("unit")
class AnnotationRoundTripTest {
    private static final String IMAGE_FOLDER = "/testimages/1";
    private static final String PVOC_REFERENCE_FILE =
            "/testannotations/pvoc/reference/austin-neill-685084-unsplash_jpg_A.xml";
    private static final String JSON_REFERENCE_FILE = "/testannotations/json/reference/annotations.json";

    @Test
    void onSavingLoadedPVOCReference_ShouldReproduceReference(@TempDir Path tempDir) throws Exception {
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.PASCAL_VOC, PVOC_REFERENCE_FILE);

        save(ImageAnnotationSaveStrategy.Type.PASCAL_VOC, annotations, tempDir);

        assertSameFileContents(resource(PVOC_REFERENCE_FILE).getParent(), tempDir);
    }

    @Test
    void onSavingLoadedYOLOReference_ShouldReproduceReference(@TempDir Path tempDir) throws Exception {
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.YOLO, "/testannotations/yolo/reference");

        save(ImageAnnotationSaveStrategy.Type.YOLO, annotations, tempDir);

        assertSameFileContents(resource("/testannotations/yolo/reference"), tempDir);
    }

    @Test
    void onSavingLoadedJSONReference_ShouldReproduceReference(@TempDir Path tempDir) throws Exception {
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.JSON, JSON_REFERENCE_FILE);

        Path actualFile = tempDir.resolve("annotations.json");
        save(ImageAnnotationSaveStrategy.Type.JSON, annotations, actualFile);

        assertSameContent(resource(JSON_REFERENCE_FILE), actualFile);
    }

    @Test
    void onSavingLoadedCSVReference_ShouldReproduceReference(@TempDir Path tempDir) throws Exception {
        final String reference = "/testannotations/csv/reference/annotations.csv";
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.CSV, reference);

        Path actualFile = tempDir.resolve("annotations.csv");
        save(ImageAnnotationSaveStrategy.Type.CSV, annotations, actualFile);

        assertSameContent(resource(reference), actualFile);
    }

    @Test
    void onSavingNestedJSONReferenceAsCSV_ShouldIncludeNestedBoxes(@TempDir Path tempDir) throws Exception {
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.JSON, JSON_REFERENCE_FILE);

        Path actualFile = tempDir.resolve("annotations.csv");
        save(ImageAnnotationSaveStrategy.Type.CSV, annotations, actualFile);

        assertSameContent(resource("/testannotations/csv/reference/annotations_with_parts.csv"), actualFile);
    }

    @Test
    void onSavingNestedPVOCReferenceAsYOLO_ShouldIncludeNestedShapes(@TempDir Path tempDir) throws Exception {
        ImageAnnotationData annotations = load(ImageAnnotationLoadStrategy.Type.PASCAL_VOC, PVOC_REFERENCE_FILE);

        save(ImageAnnotationSaveStrategy.Type.YOLO, annotations, tempDir);

        assertSameFileContents(resource("/testannotations/yolo/reference_with_parts"), tempDir);
    }

    /**
     * Loads annotations like the application does after an image folder was opened: the loaded annotations get the
     * metadata of the actual image files and are collected in a map keyed by image file name (as in the model).
     */
    private static ImageAnnotationData load(ImageAnnotationLoadStrategy.Type type, String source) throws Exception {
        final Map<String, ImageMetaData> imageMetaData = loadImageMetaData();

        final ImageAnnotationImportResult result = ImageAnnotationLoadStrategy.createStrategy(type)
                .load(resource(source), imageMetaData.keySet(), new HashMap<>(), new SimpleDoubleProperty(0));

        assertTrue(result.getErrorTableEntries().isEmpty(), () -> "Unexpected load errors: " +
                result.getErrorTableEntries().stream().map(entry -> entry.getSourceName() + ": " +
                        entry.getErrorDescription()).toList());

        final Map<String, ImageAnnotation> imageFileNameToAnnotation = new HashMap<>();

        for(ImageAnnotation annotation : result.getImageAnnotationData().imageAnnotations()) {
            annotation.setImageMetaData(imageMetaData.get(annotation.getImageFileName()));
            imageFileNameToAnnotation.put(annotation.getImageFileName(), annotation);
        }

        return new ImageAnnotationData(imageFileNameToAnnotation.values(),
                result.getImageAnnotationData().categoryNameToBoundingShapeCountMap(),
                result.getImageAnnotationData().categoryNameToCategoryMap());
    }

    private static void save(ImageAnnotationSaveStrategy.Type type, ImageAnnotationData annotations,
                             Path destination) {
        final ImageAnnotationExportResult result = ImageAnnotationSaveStrategy.createStrategy(type)
                .save(annotations, destination, new SimpleDoubleProperty(0));

        assertTrue(result.getErrorTableEntries().isEmpty(), () -> "Unexpected save errors: " +
                result.getErrorTableEntries().stream().map(entry -> entry.getSourceName() + ": " +
                        entry.getErrorDescription()).toList());
    }

    private static Map<String, ImageMetaData> loadImageMetaData() throws Exception {
        final Map<String, ImageMetaData> imageMetaData = new HashMap<>();

        try(Stream<Path> imageFiles = Files.list(resource(IMAGE_FOLDER))) {
            for(File imageFile : imageFiles.map(Path::toFile).toList()) {
                imageMetaData.put(imageFile.getName(), ImageMetaData.fromFile(imageFile));
            }
        }

        return imageMetaData;
    }

    private static void assertSameFileContents(Path expectedDirectory, Path actualDirectory) throws IOException {
        final List<String> expectedFileNames = listFileNames(expectedDirectory);

        assertEquals(expectedFileNames, listFileNames(actualDirectory));

        for(String fileName : expectedFileNames) {
            assertSameContent(expectedDirectory.resolve(fileName), actualDirectory.resolve(fileName));
        }
    }

    private static void assertSameContent(Path expected, Path actual) throws IOException {
        assertEquals(Files.readString(expected), Files.readString(actual), () -> "Content differs: " + actual);
    }

    private static List<String> listFileNames(Path directory) throws IOException {
        try(Stream<Path> files = Files.list(directory)) {
            return files.map(file -> file.getFileName().toString()).sorted().toList();
        }
    }

    private static Path resource(String path) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(AnnotationRoundTripTest.class.getResource(path)).toURI());
    }
}
