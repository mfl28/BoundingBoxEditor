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
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MaskFormatsTest {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 80;
    private static final String IMAGE = "street.jpg";
    private static final String OTHER_IMAGE = "park.png";

    private final ObjectCategory car = new ObjectCategory("car", Color.RED);
    private final ObjectCategory person = new ObjectCategory("person", Color.BLUE);
    private final ObjectCategory wheel = new ObjectCategory("wheel", Color.GREEN);

    @Test
    void onSavingAndLoadingCOCO_ShouldKeepBoxesPolygonsAndMasks(@TempDir Path tempDir) throws Exception {
        final MaskBitmap carMask = circleMask(40, 40, 15);
        final BoundingMaskData carData = new BoundingMaskData(car, carMask, List.of("difficult"));
        final BoundingBoxData wheelData = new BoundingBoxData(wheel, 0.25, 0.5, 0.375, 0.75, List.of());
        carData.setParts(List.of(wheelData));
        final BoundingPolygonData personData = new BoundingPolygonData(person,
                List.of(0.75, 0.125, 0.875, 0.125, 0.8, 0.5), List.of());
        final Path file = tempDir.resolve("instances.json");

        final ImageAnnotationExportResult exportResult = save(ImageAnnotationSaveStrategy.Type.COCO,
                annotations(new ImageAnnotation(image(IMAGE), List.of(carData, personData))), file);

        assertTrue(exportResult.getErrorTableEntries().isEmpty(), () -> describe(exportResult.getErrorTableEntries()));

        final ImageAnnotationImportResult importResult = load(ImageAnnotationLoadStrategy.Type.COCO, file, IMAGE);
        assertTrue(importResult.getErrorTableEntries().isEmpty(), () -> describe(importResult.getErrorTableEntries()));

        final List<BoundingShapeData> shapes = shapesOf(importResult, IMAGE);
        // Nested shapes become separate annotations (each shape followed by its parts), without tags.
        assertEquals(3, shapes.size());

        final BoundingMaskData loadedMask = assertInstanceOf(BoundingMaskData.class, shapes.get(0));
        assertEquals(carMask, loadedMask.getMask());
        assertEquals("car", loadedMask.getCategoryName());
        assertTrue(loadedMask.getTags().isEmpty());

        final BoundingPolygonData loadedPolygon = assertInstanceOf(BoundingPolygonData.class, shapes.get(2));
        assertEquals(personData.getRelativePointsInImage().size(), loadedPolygon.getRelativePointsInImage().size());

        for(int i = 0; i < personData.getRelativePointsInImage().size(); ++i) {
            assertEquals(personData.getRelativePointsInImage().get(i), loadedPolygon.getRelativePointsInImage().get(i),
                         1e-4);
        }

        final BoundingBoxData loadedBox = assertInstanceOf(BoundingBoxData.class, shapes.get(1));
        assertEquals(0.25, loadedBox.getXMinRelative(), 1e-4);
        assertEquals(0.75, loadedBox.getYMaxRelative(), 1e-4);

        // The category colors are kept.
        assertEquals(Color.RED, importResult.getImageAnnotationData().categoryNameToCategoryMap().get("car")
                                            .getColor());
    }

    @Test
    void onLoadingCOCOWithPartsAndCrowdMasks_ShouldCreateMasks(@TempDir Path tempDir) throws Exception {
        // An object in two parts (two polygons), and an iscrowd mask with uncompressed counts: 4 unset pixels, 6 set
        // ones (the rest of column 0 and the first pixels of column 1), the rest unset.
        final String json = """
                {"images": [{"id": 7, "file_name": "street.jpg", "width": 4, "height": 5}],
                 "categories": [{"id": 1, "name": "car"}],
                 "annotations": [
                   {"id": 1, "image_id": 7, "category_id": 1, "iscrowd": 0,
                    "segmentation": [[0, 0, 2, 0, 2, 2], [4, 3, 4, 5, 2, 5]], "bbox": [0, 0, 4, 5]},
                   {"id": 2, "image_id": 7, "category_id": 1, "iscrowd": 1,
                    "segmentation": {"size": [5, 4], "counts": [1, 6, 13]}, "bbox": [0, 1, 2, 4]}
                 ]}""";
        final Path file = tempDir.resolve("instances.json");
        Files.writeString(file, json);

        final ImageAnnotationImportResult result = load(ImageAnnotationLoadStrategy.Type.COCO, file, IMAGE);

        assertTrue(result.getErrorTableEntries().isEmpty(), () -> describe(result.getErrorTableEntries()));
        final List<BoundingShapeData> shapes = shapesOf(result, IMAGE);
        assertEquals(2, shapes.size());

        final MaskBitmap parts = assertInstanceOf(BoundingMaskData.class, shapes.get(0)).getMask();
        assertTrue(parts.get(1, 0));
        assertTrue(parts.get(3, 4));
        assertFalse(parts.get(0, 4));

        final MaskBitmap crowd = assertInstanceOf(BoundingMaskData.class, shapes.get(1)).getMask();
        assertEquals(6, crowd.getArea());
        assertFalse(crowd.get(0, 0));
        assertTrue(crowd.get(0, 1));
        assertTrue(crowd.get(0, 4));
        assertTrue(crowd.get(1, 1));
        assertFalse(crowd.get(1, 2));
    }

    @Test
    void onLoadingCOCOWithInvalidEntries_ShouldReportThemAndLoadTheRest(@TempDir Path tempDir) throws Exception {
        final String json = """
                {"images": [{"id": 1, "file_name": "street.jpg", "width": 100, "height": 50},
                            {"id": 2, "file_name": "other.jpg", "width": 100, "height": 50}],
                 "categories": [{"id": 1, "name": "car"}],
                 "annotations": [
                   {"id": 1, "image_id": 1, "category_id": 1, "bbox": [10, 10, 20, 10]},
                   {"id": 2, "image_id": 1, "category_id": 9, "bbox": [10, 10, 20, 10]},
                   {"id": 3, "image_id": 1, "category_id": 1, "segmentation": {"size": [10, 10], "counts": "0"}},
                   {"id": 4, "image_id": 1, "category_id": 1, "segmentation": [[1, 2, 3]]},
                   {"id": 5, "image_id": 2, "category_id": 1, "bbox": [10, 10, 20, 10]}
                 ]}""";
        final Path file = tempDir.resolve("instances.json");
        Files.writeString(file, json);

        final ImageAnnotationImportResult result = load(ImageAnnotationLoadStrategy.Type.COCO, file, IMAGE);

        assertEquals(1, shapesOf(result, IMAGE).size());
        final String errors = describe(result.getErrorTableEntries());
        assertEquals(4, result.getErrorTableEntries().size(), errors);
        assertTrue(errors.contains("Annotation 2 refers to an unknown category."), errors);
        assertTrue(errors.contains("Annotation 3 has a mask whose size doesn't match the image"), errors);
        assertTrue(errors.contains("Annotation 4 has a polygon with fewer than 3 points"), errors);
        assertTrue(errors.contains("Image other.jpg does not belong to currently loaded image files."), errors);
    }

    @Test
    void onLoadingInvalidCOCOFile_ShouldReportIt(@TempDir Path tempDir) throws Exception {
        final Path file = tempDir.resolve("instances.json");
        Files.writeString(file, "{\"images\": []}");

        final ImageAnnotationImportResult result = load(ImageAnnotationLoadStrategy.Type.COCO, file, IMAGE);

        assertEquals(0, result.getNrSuccessfullyProcessedItems());
        assertEquals("Missing \"categories\" array.", result.getErrorTableEntries().getFirst().getErrorDescription());
    }

    @Test
    void onSavingAndLoadingPNGMasks_ShouldKeepMasksAndFillPolygons(@TempDir Path tempDir) throws Exception {
        final MaskBitmap carMask = circleMask(40, 40, 15);
        // Overlaps the car; it is saved later, so the shared pixels belong to it.
        final MaskBitmap personMask = circleMask(55, 40, 10);
        final BoundingPolygonData wheelPolygon = new BoundingPolygonData(wheel,
                List.of(0.75, 0.125, 0.875, 0.125, 0.875, 0.375, 0.75, 0.375), List.of());
        final BoundingBoxData box = new BoundingBoxData(car, 0.1, 0.1, 0.2, 0.2, List.of());
        final ImageAnnotationData data = annotations(
                new ImageAnnotation(image(IMAGE), List.of(new BoundingMaskData(car, carMask, List.of()),
                                                          new BoundingMaskData(person, personMask, List.of()),
                                                          wheelPolygon, box)),
                new ImageAnnotation(image(OTHER_IMAGE), List.of(new BoundingMaskData(person, personMask, List.of()))));

        final ImageAnnotationExportResult exportResult = save(ImageAnnotationSaveStrategy.Type.PNG_MASKS, data, tempDir);

        assertEquals(2, exportResult.getNrSuccessfullyProcessedItems());
        assertEquals(1, exportResult.getErrorTableEntries().size());
        assertTrue(exportResult.getErrorTableEntries().getFirst().getErrorDescription()
                               .startsWith("1 bounding box(es) were not saved"));
        assertEquals(List.of("car", "person", "wheel"),
                     Files.readAllLines(tempDir.resolve("classes.txt"), StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(tempDir.resolve("SegmentationObject").resolve("street.png")));
        assertTrue(Files.isRegularFile(tempDir.resolve("SegmentationClass").resolve("park.png")));

        final ImageAnnotationImportResult importResult =
                load(ImageAnnotationLoadStrategy.Type.PNG_MASKS, tempDir, IMAGE, OTHER_IMAGE);

        assertTrue(importResult.getErrorTableEntries().isEmpty(), () -> describe(importResult.getErrorTableEntries()));
        final List<BoundingShapeData> shapes = shapesOf(importResult, IMAGE);
        assertEquals(3, shapes.size());

        final MaskBitmap loadedCar = ((BoundingMaskData) shapes.get(0)).getMask();
        assertEquals("car", shapes.get(0).getCategoryName());
        assertEquals(carMask.getArea() - overlap(carMask, personMask), loadedCar.getArea());
        assertEquals(personMask, ((BoundingMaskData) shapes.get(1)).getMask());
        assertEquals("person", shapes.get(1).getCategoryName());

        final MutableMask filledWheel = new MutableMask(WIDTH, HEIGHT);
        filledWheel.fillPolygon(new double[]{90, 10, 105, 10, 105, 30, 90, 30}, true);
        assertEquals(filledWheel.toBitmap(), ((BoundingMaskData) shapes.get(2)).getMask());
        assertEquals("wheel", shapes.get(2).getCategoryName());

        assertEquals(personMask, ((BoundingMaskData) shapesOf(importResult, OTHER_IMAGE).getFirst()).getMask());
    }

    @Test
    void onLoadingPNGMasksWithoutObjectMasks_ShouldCreateOneMaskPerCategory(@TempDir Path tempDir) throws Exception {
        final ImageAnnotationData data = annotations(new ImageAnnotation(image(IMAGE), List.of(
                new BoundingMaskData(car, circleMask(20, 20, 8), List.of()),
                new BoundingMaskData(car, circleMask(80, 50, 8), List.of()),
                new BoundingMaskData(person, circleMask(50, 20, 8), List.of()))));
        save(ImageAnnotationSaveStrategy.Type.PNG_MASKS, data, tempDir);
        deleteRecursively(tempDir.resolve("SegmentationObject"));

        final ImageAnnotationImportResult result = load(ImageAnnotationLoadStrategy.Type.PNG_MASKS, tempDir, IMAGE);

        final List<BoundingShapeData> shapes = shapesOf(result, IMAGE);
        assertEquals(2, shapes.size());
        assertEquals("car", shapes.get(0).getCategoryName());
        assertEquals(2 * circleMask(20, 20, 8).getArea(), ((BoundingMaskData) shapes.get(0)).getMask().getArea());
        assertEquals("person", shapes.get(1).getCategoryName());
    }

    @Test
    void onLoadingPNGMasksWithoutClassesFile_ShouldReportIt(@TempDir Path tempDir) throws Exception {
        final ImageAnnotationImportResult result = load(ImageAnnotationLoadStrategy.Type.PNG_MASKS, tempDir, IMAGE);

        assertEquals(0, result.getNrSuccessfullyProcessedItems());
        assertTrue(result.getErrorTableEntries().getFirst().getErrorDescription().contains("classes.txt"));
    }

    @Test
    void onSavingMasksInFormatWithoutMasks_ShouldLeaveThemOutAndReportIt(@TempDir Path tempDir) throws Exception {
        final BoundingBoxData carBox = new BoundingBoxData(car, 0.1, 0.1, 0.5, 0.5, List.of());
        final BoundingMaskData nestedMask = new BoundingMaskData(wheel, circleMask(20, 20, 5), List.of());
        carBox.setParts(List.of(nestedMask));
        final ImageAnnotationData data = annotations(new ImageAnnotation(image(IMAGE), List.of(
                carBox, new BoundingMaskData(person, circleMask(80, 40, 10), List.of()))));

        final ImageAnnotationExportResult result = save(ImageAnnotationSaveStrategy.Type.YOLO, data, tempDir);

        assertEquals(1, result.getErrorTableEntries().size());
        assertTrue(result.getErrorTableEntries().getFirst().getErrorDescription()
                         .startsWith("2 mask(s) were not saved, because the YOLO format can't contain masks"));
        assertEquals(1, Files.readAllLines(tempDir.resolve("street.txt")).size());
        // The annotations themselves are unchanged.
        assertEquals(List.of(nestedMask), carBox.getParts());
    }

    @Test
    void onRemovingMasks_ShouldCopyOnlyShapesThatContainMasks() {
        final BoundingBoxData plainBox = new BoundingBoxData(car, 0.1, 0.1, 0.5, 0.5, List.of());
        final BoundingBoxData boxWithMask = new BoundingBoxData(car, 0.1, 0.1, 0.5, 0.5, List.of("occluded"));
        final BoundingMaskData mask = new BoundingMaskData(wheel, circleMask(20, 20, 5), List.of());
        mask.setParts(List.of(new BoundingBoxData(wheel, 0.1, 0.1, 0.2, 0.2, List.of())));
        boxWithMask.setParts(List.of(mask, plainBox));
        final AtomicInteger nrRemoved = new AtomicInteger();

        final List<BoundingShapeData> result = ImageAnnotationSaver.withoutMasks(List.of(plainBox, boxWithMask),
                                                                                 nrRemoved);

        assertEquals(1, nrRemoved.get());
        assertSame(plainBox, result.get(0));
        assertNotSame(boxWithMask, result.get(1));
        assertEquals(List.of(plainBox), result.get(1).getParts());
        assertEquals(List.of("occluded"), result.get(1).getTags());
    }

    private static MaskBitmap circleMask(double centerX, double centerY, double radius) {
        final MutableMask mask = new MutableMask(WIDTH, HEIGHT);
        mask.fillCircle(centerX, centerY, radius, true);
        return mask.toBitmap();
    }

    private static int overlap(MaskBitmap first, MaskBitmap second) {
        int count = 0;

        for(int y = 0; y < HEIGHT; ++y) {
            for(int x = 0; x < WIDTH; ++x) {
                if(first.get(x, y) && second.get(x, y)) {
                    ++count;
                }
            }
        }

        return count;
    }

    private static ImageMetaData image(String fileName) {
        return new ImageMetaData(fileName, "folder", "file:/folder/" + fileName, WIDTH, HEIGHT, 3);
    }

    private ImageAnnotationData annotations(ImageAnnotation... imageAnnotations) {
        final Map<String, ObjectCategory> categories = new HashMap<>();
        List.of(car, person, wheel).forEach(category -> categories.put(category.getName(), category));
        return new ImageAnnotationData(List.of(imageAnnotations), Map.of(), categories);
    }

    private static ImageAnnotationExportResult save(ImageAnnotationSaveStrategy.Type type, ImageAnnotationData data,
                                                    Path destination) throws Exception {
        return new ImageAnnotationSaver(type).save(data, destination);
    }

    private static ImageAnnotationImportResult load(ImageAnnotationLoadStrategy.Type type, Path source,
                                                    String... imageFileNames) throws Exception {
        return ImageAnnotationLoadStrategy.createStrategy(type).load(source, Set.of(imageFileNames), new HashMap<>(),
                                                                     new SimpleDoubleProperty());
    }

    private static List<BoundingShapeData> shapesOf(ImageAnnotationImportResult result, String imageFileName) {
        return result.getImageAnnotationData().imageAnnotations().stream()
                     .filter(annotation -> annotation.getImageFileName().equals(imageFileName))
                     .findFirst()
                     .orElseThrow()
                     .getBoundingShapeData();
    }

    private static String describe(List<IOErrorInfoEntry> entries) {
        return entries.stream().map(entry -> entry.getSourceName() + ": " + entry.getErrorDescription())
                      .reduce("", (first, second) -> first + "\n" + second);
    }

    private static void deleteRecursively(Path folder) throws Exception {
        try(var paths = Files.walk(folder)) {
            for(Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
