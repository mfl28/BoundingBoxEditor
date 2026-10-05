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
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invalid input for the COCO and PNG mask formats: each problem is reported, and the rest is still imported or saved.
 */
@Tag("unit")
class MaskFormatErrorsTest {
    private static final String IMAGE = "street.jpg";

    @Test
    void onLoadingCOCOWithInvalidImagesCategoriesAndAnnotations_ShouldReportEachAndLoadTheValidOne(
            @TempDir Path tempDir) throws Exception {
        final String json = """
                {"images": [
                   {"id": 1, "file_name": "street.jpg", "width": 100, "height": 50},
                   {"id": 2, "file_name": "other.jpg", "width": 100, "height": 50},
                   "not an image",
                   {"id": 3, "width": 100, "height": 50},
                   {"id": 4, "file_name": "zero.jpg", "width": 0, "height": 50},
                   {"id": 1, "file_name": "duplicate.jpg", "width": 100, "height": 50}
                 ],
                 "categories": [
                   {"id": 1, "name": "car", "color": "not a color"},
                   "not a category",
                   {"id": 2, "name": " "},
                   {"name": "no id"}
                 ],
                 "annotations": [
                   {"id": 1, "image_id": 1, "category_id": 1, "bbox": [10, 10, 20, 10]},
                   "not an annotation",
                   {"id": 3, "image_id": 99, "category_id": 1, "bbox": [10, 10, 20, 10]},
                   {"id": 4, "category_id": 1, "bbox": [10, 10, 20, 10]},
                   {"id": 5, "image_id": 1, "category_id": 1, "bbox": [10, 10, 0, 10]},
                   {"id": 6, "image_id": 1, "category_id": 1, "bbox": [200, 10, 20, 10]},
                   {"id": 7, "image_id": 1, "category_id": 1, "segmentation": [5]},
                   {"id": 8, "image_id": 1, "category_id": 1, "segmentation": [[1, 2, 3, "four", 5, 6]]},
                   {"id": 9, "image_id": 1, "category_id": 1, "segmentation": {"size": [50, 100]}},
                   {"id": 10, "image_id": 1, "category_id": 1, "segmentation": {"size": [50, 100], "counts": [10, 5]}},
                   {"id": 11, "image_id": 1, "category_id": 1, "bbox": "no array"},
                   {"id": 12, "image_id": 2, "category_id": 1, "bbox": [10, 10, 20, 10]},
                   {"id": 13, "image_id": 2, "category_id": 1, "bbox": [30, 10, 20, 10]}
                 ]}""";

        final ImageAnnotationImportResult result = loadCOCO(tempDir, json);

        assertEquals(List.of(BoundingBoxData.class), shapesOf(result).stream().map(Object::getClass).toList());
        assertEquals(List.of(
                "Image entry 3 is not an object.",
                "Image entry 4 has no valid \"file_name\".",
                "Image entry 5 has no valid \"width\".",
                "Image entry 6 has the id 1 of another image.",
                "Category entry 2 is not an object.",
                "Category entry 3 has an empty name.",
                "Category entry 4 has no valid \"id\".",
                "Annotation 2 is not an object.",
                "Annotation 3 refers to an unknown image.",
                "Annotation 4 has no valid \"image_id\".",
                "Annotation 5 has neither a segmentation nor a valid bbox.",
                "Annotation 6 has a bbox outside of the image.",
                "Annotation 7 has an invalid polygon.",
                "Annotation 8 contains an invalid number.",
                "Annotation 9 has a mask without counts.",
                "Annotation 10 has an invalid mask: The run lengths cover 15 pixels, but the mask (100x50) has 5000.",
                "Annotation 11 has no valid \"bbox\".",
                // Reported once, although two annotations belong to it.
                "Image other.jpg does not belong to currently loaded image files."), messages(result));
        // An invalid color is replaced by a random one.
        assertNotNull(result.getImageAnnotationData().categoryNameToCategoryMap().get("car").getColor());
    }

    @Test
    void onLoadingFilesThatAreNoCOCOObjects_ShouldReportThem(@TempDir Path tempDir) throws Exception {
        assertEquals(List.of("Invalid COCO file: The file doesn't contain a JSON object."),
                     messages(loadCOCO(tempDir, "[1, 2]")));
        assertTrue(messages(loadCOCO(tempDir, "{\"images\": [")).getFirst().startsWith("Invalid COCO file: "));
        assertEquals(List.of("Missing \"images\" array."), messages(loadCOCO(tempDir, "{\"images\": 5}")));
    }

    @Test
    void onLoadingPNGMasksWithInvalidFiles_ShouldReportEachAndLoadTheValidOne(@TempDir Path tempDir)
            throws Exception {
        Files.writeString(tempDir.resolve("classes.txt"), "car\n");
        final Path objects = Files.createDirectories(tempDir.resolve("SegmentationObject"));
        final Path classes = Files.createDirectories(tempDir.resolve("SegmentationClass"));

        // Valid: object 1 is a car, object 2 has background as category, object 3 an unknown category, and the
        // "void" border (255) is ignored.
        writeMasks(objects, classes, "street", 4, 2, new int[]{1, 1, 2, 3, 255, 0, 0, 0},
                   new int[]{1, 1, 0, 7, 255, 0, 0, 0});
        // No image with this name; two images with this name.
        writeMasks(objects, classes, "unknown", 2, 2, new int[4], new int[4]);
        writeMasks(objects, classes, "twice", 2, 2, new int[4], new int[4]);
        // Object and category masks of different sizes.
        PNGMasks.write(new byte[4], 2, 2, objects.resolve("sizes.png"));
        PNGMasks.write(new byte[6], 3, 2, classes.resolve("sizes.png"));
        // A color image, and a file that isn't an image.
        final BufferedImage rgb = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(rgb, "png", objects.resolve("color.png").toFile());
        ImageIO.write(rgb, "png", classes.resolve("color.png").toFile());
        Files.writeString(objects.resolve("broken.png"), "no image");
        Files.writeString(classes.resolve("broken.png"), "no image");
        // Not a PNG: ignored.
        Files.writeString(objects.resolve("notes.txt"), "ignored");

        final ImageAnnotationImportResult result = ImageAnnotationLoadStrategy
                .createStrategy(ImageAnnotationLoadStrategy.Type.PNG_MASKS)
                .load(tempDir, Set.of(IMAGE, "twice.jpg", "twice.png", "sizes.jpg", "color.jpg", "broken.jpg"),
                      new HashMap<>(), new SimpleDoubleProperty());

        final List<BoundingShapeData> shapes = shapesOf(result);
        assertEquals(1, shapes.size());
        assertEquals(2, ((BoundingMaskData) shapes.getFirst()).getMask().getArea());

        final Map<String, String> errors = result.getErrorTableEntries().stream()
                .collect(Collectors.toMap(IOErrorInfoEntry::getSourceName, IOErrorInfoEntry::getErrorDescription,
                                          (first, second) -> first + " | " + second));
        assertEquals("Not a readable image.", errors.get("broken.png"));
        assertEquals("The mask is not an indexed or grayscale image (it has 3 color channels).",
                     errors.get("color.png"));
        assertEquals("The object and category masks have different sizes.", errors.get("sizes.png"));
        assertEquals("Several images (twice.jpg, twice.png) have the name of the mask.",
                     errors.get("twice.png").replace("twice.png, twice.jpg", "twice.jpg, twice.png"));
        assertEquals("No image of the currently loaded image files has the name of the mask.",
                     errors.get("unknown.png"));
        assertEquals("Object 2 has no valid category (its pixels are background in the category mask). | "
                             + "Object 3 has no valid category (category 7 is not in classes.txt).",
                     errors.get("street.png"));
        assertEquals(6, errors.size());
    }

    @Test
    void onLoadingPNGMasksWithoutCategoryMasks_ShouldReportIt(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("classes.txt"), "car\n");

        final ImageAnnotationImportResult result = ImageAnnotationLoadStrategy
                .createStrategy(ImageAnnotationLoadStrategy.Type.PNG_MASKS)
                .load(tempDir, Set.of(IMAGE), new HashMap<>(), new SimpleDoubleProperty());

        assertEquals(List.of("The folder \"SegmentationClass\" with the category masks is missing."),
                     messages(result));
    }

    @Test
    void onSavingPNGMasksWithProblematicImages_ShouldReportThemAndSaveTheRest(@TempDir Path tempDir)
            throws Exception {
        final ObjectCategory car = new ObjectCategory("car", Color.RED);
        final MaskBitmap pixel = mask(10, 10, 1, 1);
        final List<BoundingShapeData> manyObjects = IntStream.range(0, 255)
                .mapToObj(i -> (BoundingShapeData) new BoundingMaskData(car, mask(20, 20, i % 20, i / 20), List.of()))
                .toList();

        final ImageAnnotationExportResult result = new ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type.PNG_MASKS)
                .save(annotations(car,
                                  // Images whose names only differ in the extension.
                                  new ImageAnnotation(image("cat.jpg", 10, 10), maskShapes(car, pixel)),
                                  new ImageAnnotation(image("cat.png", 10, 10), maskShapes(car, pixel)),
                                  // Unknown size.
                                  new ImageAnnotation(image("unknown.jpg", 0, 0), maskShapes(car, pixel)),
                                  // More objects than an object mask can contain.
                                  new ImageAnnotation(image("crowd.jpg", 20, 20), manyObjects),
                                  // An empty mask is left out.
                                  new ImageAnnotation(image("empty.jpg", 10, 10),
                                                      maskShapes(car, MaskBitmap.empty(10, 10)))),
                      tempDir);

        final Map<String, String> errors = result.getErrorTableEntries().stream()
                .collect(Collectors.toMap(IOErrorInfoEntry::getSourceName, IOErrorInfoEntry::getErrorDescription,
                                          (first, second) -> first));
        assertTrue(errors.get("cat.jpg").contains("would share the mask file \"cat.png\""), errors::toString);
        assertTrue(errors.containsKey("cat.png"));
        assertEquals("The size of the image is unknown.", errors.get("unknown.jpg"));
        assertEquals("The image has 255 masks and polygons, but an object mask can contain at most 254; only its "
                             + "category mask was saved.", errors.get("crowd.jpg"));
        assertEquals(2, result.getNrSuccessfullyProcessedItems());
        assertTrue(Files.exists(tempDir.resolve("SegmentationClass").resolve("crowd.png")));
        assertFalse(Files.exists(tempDir.resolve("SegmentationObject").resolve("crowd.png")));
        assertTrue(Files.exists(tempDir.resolve("SegmentationObject").resolve("empty.png")));
    }

    @Test
    void onSavingPNGMasksWithTooManyCategoriesOrIntoAFile_ShouldReportIt(@TempDir Path tempDir) throws Exception {
        final List<BoundingShapeData> shapes = IntStream.range(0, 255)
                .mapToObj(i -> (BoundingShapeData) new BoundingMaskData(new ObjectCategory("c" + i, Color.RED),
                                                                        mask(20, 20, i % 20, i / 20), List.of()))
                .toList();
        final ImageAnnotationSaver saver = new ImageAnnotationSaver(ImageAnnotationSaveStrategy.Type.PNG_MASKS);

        final ImageAnnotationExportResult tooMany = saver.save(
                new ImageAnnotationData(List.of(new ImageAnnotation(image(IMAGE, 20, 20), shapes)), Map.of(),
                                        Map.of()), tempDir.resolve("masks"));
        assertEquals(List.of("PNG masks can contain at most 254 categories, but the masks and polygons have 255."),
                     messages(tooMany));

        // The destination is a file, so the folders can't be created.
        final Path file = Files.writeString(tempDir.resolve("file"), "");
        final ObjectCategory car = new ObjectCategory("car", Color.RED);
        final ImageAnnotationExportResult intoFile = saver.save(
                annotations(car, new ImageAnnotation(image(IMAGE, 10, 10), maskShapes(car, mask(10, 10, 1, 1)))),
                file);
        assertEquals(0, intoFile.getNrSuccessfullyProcessedItems());
        assertEquals(1, intoFile.getErrorTableEntries().size());
    }

    private static ImageAnnotationImportResult loadCOCO(Path folder, String json) throws Exception {
        final Path file = folder.resolve("instances.json");
        Files.writeString(file, json);
        return ImageAnnotationLoadStrategy.createStrategy(ImageAnnotationLoadStrategy.Type.COCO)
                                          .load(file, Set.of(IMAGE), new HashMap<>(), new SimpleDoubleProperty());
    }

    private static void writeMasks(Path objects, Path classes, String name, int width, int height, int[] objectValues,
                                   int[] classValues) throws Exception {
        PNGMasks.write(toBytes(objectValues), width, height, objects.resolve(name + ".png"));
        PNGMasks.write(toBytes(classValues), width, height, classes.resolve(name + ".png"));
    }

    private static byte[] toBytes(int[] values) {
        final byte[] bytes = new byte[values.length];

        for(int i = 0; i < values.length; ++i) {
            bytes[i] = (byte) values[i];
        }

        return bytes;
    }

    private static MaskBitmap mask(int width, int height, int x, int y) {
        final MutableMask mask = new MutableMask(width, height);
        mask.set(x, y, true);
        return mask.toBitmap();
    }

    private static List<BoundingShapeData> maskShapes(ObjectCategory category, MaskBitmap mask) {
        return List.of(new BoundingMaskData(category, mask, List.of()));
    }

    private static ImageMetaData image(String fileName, int width, int height) {
        return new ImageMetaData(fileName, "folder", "file:/folder/" + fileName, width, height, 3);
    }

    private static ImageAnnotationData annotations(ObjectCategory category, ImageAnnotation... imageAnnotations) {
        return new ImageAnnotationData(List.of(imageAnnotations), Map.of(), Map.of(category.getName(), category));
    }

    private static List<BoundingShapeData> shapesOf(ImageAnnotationImportResult result) {
        return result.getImageAnnotationData().imageAnnotations().stream()
                     .filter(annotation -> annotation.getImageFileName().equals(IMAGE))
                     .findFirst()
                     .map(ImageAnnotation::getBoundingShapeData)
                     .orElse(List.of());
    }

    private static List<String> messages(IOResult result) {
        return result.getErrorTableEntries().stream().map(IOErrorInfoEntry::getErrorDescription).toList();
    }
}
