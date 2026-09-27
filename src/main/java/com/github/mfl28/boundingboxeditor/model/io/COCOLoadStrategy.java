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
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.google.gson.*;
import javafx.beans.property.DoubleProperty;
import javafx.scene.paint.Color;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.github.mfl28.boundingboxeditor.model.io.COCOSaveStrategy.*;

/**
 * Loads annotations from a file in the COCO object detection format (see {@link COCOSaveStrategy}):
 * <ul>
 *     <li>an annotation without {@code segmentation} becomes a box (from its {@code bbox});</li>
 *     <li>one with a single polygon becomes a polygon;</li>
 *     <li>one with several polygons (an object in several parts) becomes a mask of all of them;</li>
 *     <li>one with a run-length {@code segmentation} (compressed or, as for {@code iscrowd} annotations, not)
 *     becomes a mask.</li>
 * </ul>
 * Invalid images, categories and annotations are reported, and the rest is imported.
 */
public class COCOLoadStrategy implements ImageAnnotationLoadStrategy {
    @Override
    public ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                            Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                            DoubleProperty progress) throws IOException {
        progress.set(0);
        final String sourceName = path.getFileName().toString();
        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();
        final JsonObject root;

        try(Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final JsonElement element = JsonParser.parseReader(reader);

            if(!element.isJsonObject()) {
                throw new JsonParseException("The file doesn't contain a JSON object.");
            }

            root = element.getAsJsonObject();
        } catch(JsonParseException exception) {
            errorEntries.add(new IOErrorInfoEntry(sourceName, "Invalid COCO file: " + exception.getMessage()));
            return emptyResult(errorEntries, existingCategoryNameToCategoryMap);
        }

        final LoadContext context = new LoadContext(sourceName, filesToLoad, existingCategoryNameToCategoryMap,
                                                    errorEntries);

        for(String key : List.of(IMAGES_KEY, CATEGORIES_KEY, ANNOTATIONS_KEY)) {
            if(!root.has(key) || !root.get(key).isJsonArray()) {
                errorEntries.add(new IOErrorInfoEntry(sourceName, "Missing \"" + key + "\" array."));
                return emptyResult(errorEntries, existingCategoryNameToCategoryMap);
            }
        }

        final Map<Long, ImageEntry> images = readImages(root.getAsJsonArray(IMAGES_KEY), context);
        final Map<Long, JsonObject> categories = readCategories(root.getAsJsonArray(CATEGORIES_KEY), context);
        final JsonArray annotations = root.getAsJsonArray(ANNOTATIONS_KEY);
        final Map<String, ImageAnnotation> fileNameToAnnotation = new LinkedHashMap<>();
        final Map<String, Integer> categoryNameToShapeCount = new HashMap<>();
        final Set<String> reportedOtherImages = new HashSet<>();

        for(int i = 0; i < annotations.size(); ++i) {
            final JsonElement element = annotations.get(i);
            final String annotationName = "Annotation " + (i + 1);

            try {
                if(!element.isJsonObject()) {
                    throw new InvalidAnnotationFormatException(annotationName + " is not an object.");
                }

                final JsonObject annotation = element.getAsJsonObject();
                final ImageEntry image = images.get(readId(annotation, IMAGE_ID_KEY, annotationName));

                if(image == null) {
                    throw new InvalidAnnotationFormatException(annotationName + " refers to an unknown image.");
                }

                if(!filesToLoad.contains(image.fileName())) {
                    if(reportedOtherImages.add(image.fileName())) {
                        errorEntries.add(new IOErrorInfoEntry(image.fileName(), "Image " + image.fileName()
                                + " does not belong to currently loaded image files."));
                    }

                    continue;
                }

                final JsonObject category = categories.get(readId(annotation, CATEGORY_ID_KEY, annotationName));

                if(category == null) {
                    throw new InvalidAnnotationFormatException(annotationName + " refers to an unknown category.");
                }

                final BoundingShapeData shape = createShape(annotation, image, context.categoryFor(category),
                                                            annotationName);
                fileNameToAnnotation.computeIfAbsent(image.fileName(),
                                                     name -> new ImageAnnotation(new ImageMetaData(name)))
                                    .getBoundingShapeData().add(shape);
                categoryNameToShapeCount.merge(shape.getCategoryName(), 1, Integer::sum);
            } catch(InvalidAnnotationFormatException | IllegalArgumentException | IllegalStateException |
                    UnsupportedOperationException exception) {
                errorEntries.add(new IOErrorInfoEntry(sourceName, exception.getMessage()));
            }

            progress.set((i + 1.0) / annotations.size());
        }

        progress.set(1.0);
        return new ImageAnnotationImportResult(fileNameToAnnotation.size(), errorEntries,
                                               new ImageAnnotationData(fileNameToAnnotation.values(),
                                                                       categoryNameToShapeCount,
                                                                       existingCategoryNameToCategoryMap));
    }

    private static ImageAnnotationImportResult emptyResult(List<IOErrorInfoEntry> errorEntries,
                                                           Map<String, ObjectCategory> categories) {
        return new ImageAnnotationImportResult(0, errorEntries,
                                               new ImageAnnotationData(Collections.emptyList(),
                                                                       Collections.emptyMap(), categories));
    }

    private static Map<Long, ImageEntry> readImages(JsonArray imagesArray, LoadContext context) {
        final Map<Long, ImageEntry> images = new HashMap<>();

        for(int i = 0; i < imagesArray.size(); ++i) {
            final String imageName = "Image entry " + (i + 1);

            try {
                if(!imagesArray.get(i).isJsonObject()) {
                    throw new InvalidAnnotationFormatException(imageName + " is not an object.");
                }

                final JsonObject image = imagesArray.get(i).getAsJsonObject();
                final long id = readId(image, ID_KEY, imageName);
                final String fileName = FilenameUtils.getName(readString(image, FILE_NAME_KEY, imageName));
                final int width = readPositiveInt(image, WIDTH_KEY, imageName);
                final int height = readPositiveInt(image, HEIGHT_KEY, imageName);

                if(images.putIfAbsent(id, new ImageEntry(fileName, width, height)) != null) {
                    throw new InvalidAnnotationFormatException(imageName + " has the id " + id
                                                                       + " of another image.");
                }
            } catch(InvalidAnnotationFormatException | IllegalStateException | UnsupportedOperationException |
                    NumberFormatException exception) {
                context.errorEntries().add(new IOErrorInfoEntry(context.sourceName(), exception.getMessage()));
            }
        }

        return images;
    }

    private static Map<Long, JsonObject> readCategories(JsonArray categoriesArray, LoadContext context) {
        final Map<Long, JsonObject> categories = new HashMap<>();

        for(int i = 0; i < categoriesArray.size(); ++i) {
            final String categoryName = "Category entry " + (i + 1);

            try {
                if(!categoriesArray.get(i).isJsonObject()) {
                    throw new InvalidAnnotationFormatException(categoryName + " is not an object.");
                }

                final JsonObject category = categoriesArray.get(i).getAsJsonObject();
                final String name = readString(category, NAME_KEY, categoryName);

                if(name.isBlank()) {
                    throw new InvalidAnnotationFormatException(categoryName + " has an empty name.");
                }

                categories.put(readId(category, ID_KEY, categoryName), category);
            } catch(InvalidAnnotationFormatException | IllegalStateException | UnsupportedOperationException |
                    NumberFormatException exception) {
                context.errorEntries().add(new IOErrorInfoEntry(context.sourceName(), exception.getMessage()));
            }
        }

        return categories;
    }

    private static BoundingShapeData createShape(JsonObject annotation, ImageEntry image, ObjectCategory category,
                                                 String annotationName) {
        final JsonElement segmentation = annotation.get(SEGMENTATION_KEY);

        if(segmentation != null && segmentation.isJsonObject()) {
            return new BoundingMaskData(category, readRunLengthMask(segmentation.getAsJsonObject(), image,
                                                                     annotationName), new ArrayList<>());
        }

        if(segmentation != null && segmentation.isJsonArray() && !segmentation.getAsJsonArray().isEmpty()) {
            final List<double[]> polygons = new ArrayList<>();

            for(JsonElement polygonElement : segmentation.getAsJsonArray()) {
                polygons.add(readPolygon(polygonElement, image, annotationName));
            }

            if(polygons.size() == 1) {
                final double[] points = polygons.getFirst();
                final List<Double> relativePoints = new ArrayList<>(points.length);

                for(int i = 0; i < points.length; i += 2) {
                    relativePoints.add(points[i] / image.width());
                    relativePoints.add(points[i + 1] / image.height());
                }

                return new BoundingPolygonData(category, relativePoints, new ArrayList<>());
            }

            // An object in several parts.
            final MutableMask mask = new MutableMask(image.width(), image.height());
            polygons.forEach(points -> mask.fillPolygon(points, true));
            return new BoundingMaskData(category, mask.toBitmap(), new ArrayList<>());
        }

        return createBox(annotation, image, category, annotationName);
    }

    private static BoundingBoxData createBox(JsonObject annotation, ImageEntry image, ObjectCategory category,
                                             String annotationName) {
        final double[] bbox = readNumbers(annotation, BBOX_KEY, annotationName);

        if(bbox.length != 4 || bbox[2] <= 0 || bbox[3] <= 0) {
            throw new InvalidAnnotationFormatException(annotationName
                                                               + " has neither a segmentation nor a valid bbox.");
        }

        final double minX = Math.clamp(bbox[0] / image.width(), 0, 1);
        final double minY = Math.clamp(bbox[1] / image.height(), 0, 1);
        final double maxX = Math.clamp((bbox[0] + bbox[2]) / image.width(), 0, 1);
        final double maxY = Math.clamp((bbox[1] + bbox[3]) / image.height(), 0, 1);

        if(minX >= maxX || minY >= maxY) {
            throw new InvalidAnnotationFormatException(annotationName + " has a bbox outside of the image.");
        }

        return new BoundingBoxData(category, minX, minY, maxX, maxY, new ArrayList<>());
    }

    private static double[] readPolygon(JsonElement polygonElement, ImageEntry image, String annotationName) {
        if(!polygonElement.isJsonArray()) {
            throw new InvalidAnnotationFormatException(annotationName + " has an invalid polygon.");
        }

        final JsonArray array = polygonElement.getAsJsonArray();

        if(array.size() < 6 || array.size() % 2 != 0) {
            throw new InvalidAnnotationFormatException(annotationName
                                                               + " has a polygon with fewer than 3 points or an odd "
                                                               + "number of coordinates.");
        }

        final double[] points = new double[array.size()];

        for(int i = 0; i < points.length; ++i) {
            final double value = readNumber(array.get(i), annotationName);
            // Polygons are often written up to the image's edge (e.g. x = width), or slightly beyond it.
            points[i] = Math.clamp(value, 0, i % 2 == 0 ? image.width() : image.height());
        }

        return points;
    }

    private static MaskBitmap readRunLengthMask(JsonObject segmentation, ImageEntry image, String annotationName) {
        final double[] size = readNumbers(segmentation, SIZE_KEY, annotationName);

        if(size.length != 2 || size[0] != image.height() || size[1] != image.width()) {
            throw new InvalidAnnotationFormatException(annotationName + " has a mask whose size doesn't match the "
                                                               + "image (" + image.width() + "x" + image.height()
                                                               + ").");
        }

        final JsonElement counts = segmentation.get(COUNTS_KEY);
        final long[] runLengths;

        if(counts != null && counts.isJsonPrimitive() && counts.getAsJsonPrimitive().isString()) {
            runLengths = COCORunLengthEncoding.decode(counts.getAsString());
        } else if(counts != null && counts.isJsonArray()) {
            final JsonArray array = counts.getAsJsonArray();
            runLengths = new long[array.size()];

            for(int i = 0; i < runLengths.length; ++i) {
                runLengths[i] = (long) readNumber(array.get(i), annotationName);
            }
        } else {
            throw new InvalidAnnotationFormatException(annotationName + " has a mask without counts.");
        }

        try {
            return MaskBitmap.fromColumnMajorRunLengths(runLengths, image.width(), image.height());
        } catch(IllegalArgumentException exception) {
            throw new InvalidAnnotationFormatException(annotationName + " has an invalid mask: "
                                                               + exception.getMessage());
        }
    }

    private static long readId(JsonObject object, String key, String objectName) {
        final JsonElement element = object.get(key);

        if(element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new InvalidAnnotationFormatException(objectName + " has no valid \"" + key + "\".");
        }

        return element.getAsLong();
    }

    private static String readString(JsonObject object, String key, String objectName) {
        final JsonElement element = object.get(key);

        if(element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new InvalidAnnotationFormatException(objectName + " has no valid \"" + key + "\".");
        }

        return element.getAsString();
    }

    private static int readPositiveInt(JsonObject object, String key, String objectName) {
        final JsonElement element = object.get(key);

        if(element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()
                || element.getAsInt() <= 0) {
            throw new InvalidAnnotationFormatException(objectName + " has no valid \"" + key + "\".");
        }

        return element.getAsInt();
    }

    private static double[] readNumbers(JsonObject object, String key, String objectName) {
        final JsonElement element = object.get(key);

        if(element == null || !element.isJsonArray()) {
            throw new InvalidAnnotationFormatException(objectName + " has no valid \"" + key + "\".");
        }

        final JsonArray array = element.getAsJsonArray();
        final double[] values = new double[array.size()];

        for(int i = 0; i < values.length; ++i) {
            values[i] = readNumber(array.get(i), objectName);
        }

        return values;
    }

    private static double readNumber(JsonElement element, String objectName) {
        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()
                || !Double.isFinite(element.getAsDouble())) {
            throw new InvalidAnnotationFormatException(objectName + " contains an invalid number.");
        }

        return element.getAsDouble();
    }

    private record ImageEntry(String fileName, int width, int height) {
    }

    private record LoadContext(String sourceName, Set<String> filesToLoad,
                               Map<String, ObjectCategory> categoryNameToCategory,
                               List<IOErrorInfoEntry> errorEntries) {
        ObjectCategory categoryFor(JsonObject category) {
            final String name = category.get(NAME_KEY).getAsString();

            return categoryNameToCategory.computeIfAbsent(name, key -> {
                Color color = null;

                if(category.has(COLOR_KEY)) {
                    try {
                        color = Color.web(category.get(COLOR_KEY).getAsString());
                    } catch(IllegalArgumentException | IllegalStateException | UnsupportedOperationException _) {
                        // Invalid colors are replaced by random ones.
                    }
                }

                return new ObjectCategory(key, color != null ? color : ColorUtils.createRandomColor());
            });
        }
    }
}
