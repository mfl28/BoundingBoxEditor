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
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Bounds;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Saves the annotations of all images to one file in the COCO object detection format
 * (<a href="https://cocodataset.org/#format-data">cocodataset.org/#format-data</a>):
 * <ul>
 *     <li>boxes as {@code bbox} (without {@code segmentation});</li>
 *     <li>polygons as polygon {@code segmentation};</li>
 *     <li>masks as compressed run-length {@code segmentation} (as written by pycocotools).</li>
 * </ul>
 * Nested shapes are saved as separate annotations; tags are not saved. Coordinates are in pixels of the image as
 * shown (after EXIF orientation). Categories get an additional {@code color} field, which the app reads back.
 */
public class COCOSaveStrategy implements ImageAnnotationSaveStrategy {
    static final String IMAGES_KEY = "images";
    static final String CATEGORIES_KEY = "categories";
    static final String ANNOTATIONS_KEY = "annotations";
    static final String ID_KEY = "id";
    static final String FILE_NAME_KEY = "file_name";
    static final String WIDTH_KEY = "width";
    static final String HEIGHT_KEY = "height";
    static final String NAME_KEY = "name";
    static final String COLOR_KEY = "color";
    static final String IMAGE_ID_KEY = "image_id";
    static final String CATEGORY_ID_KEY = "category_id";
    static final String BBOX_KEY = "bbox";
    static final String AREA_KEY = "area";
    static final String ISCROWD_KEY = "iscrowd";
    static final String SEGMENTATION_KEY = "segmentation";
    static final String SIZE_KEY = "size";
    static final String COUNTS_KEY = "counts";
    private static final String INFO_KEY = "info";
    private static final String DESCRIPTION_KEY = "description";
    private static final String SUPERCATEGORY_KEY = "supercategory";

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        progress.set(0);
        // DecimalFormat isn't thread-safe, so every call uses its own instance.
        final DecimalFormat decimalFormat =
                new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        final List<ImageAnnotation> imageAnnotations = annotations.imageAnnotations().stream()
                .sorted(Comparator.comparing(ImageAnnotation::getImageFileName))
                .toList();
        final Map<String, Integer> categoryIds = createCategoryIds(imageAnnotations);

        final JsonObject root = new JsonObject();
        final JsonObject info = new JsonObject();
        info.addProperty(DESCRIPTION_KEY, "Created with Bounding Box Editor");
        root.add(INFO_KEY, info);

        final JsonArray images = new JsonArray();
        final JsonArray annotationsArray = new JsonArray();
        int annotationId = 1;

        for(int i = 0; i < imageAnnotations.size(); ++i) {
            final ImageAnnotation imageAnnotation = imageAnnotations.get(i);
            final int imageId = i + 1;
            final int width = (int) Math.round(imageAnnotation.getOrientedImageWidth());
            final int height = (int) Math.round(imageAnnotation.getOrientedImageHeight());

            final JsonObject image = new JsonObject();
            image.addProperty(ID_KEY, imageId);
            image.addProperty(FILE_NAME_KEY, imageAnnotation.getImageFileName());
            image.addProperty(WIDTH_KEY, width);
            image.addProperty(HEIGHT_KEY, height);
            images.add(image);

            for(BoundingShapeData shape : imageAnnotation.getBoundingShapeData().stream()
                                                         .flatMap(BoundingShapeData::flatten).toList()) {
                final JsonObject annotation = createAnnotation(shape, width, height, decimalFormat);

                if(annotation == null) {
                    continue;
                }

                annotation.addProperty(ID_KEY, annotationId++);
                annotation.addProperty(IMAGE_ID_KEY, imageId);
                annotation.addProperty(CATEGORY_ID_KEY, categoryIds.get(shape.getCategoryName()));
                annotation.addProperty(ISCROWD_KEY, 0);
                annotationsArray.add(annotation);
            }

            progress.set(0.9 * (i + 1) / imageAnnotations.size());
        }

        root.add(IMAGES_KEY, images);
        root.add(CATEGORIES_KEY, createCategories(categoryIds, annotations.categoryNameToCategoryMap()));
        root.add(ANNOTATIONS_KEY, annotationsArray);

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();

        try(BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            new Gson().toJson(root, writer);
        } catch(IOException e) {
            errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(), e.getMessage()));
        }

        progress.set(1.0);
        return new ImageAnnotationExportResult(errorEntries.isEmpty() ? imageAnnotations.size() : 0, errorEntries);
    }

    private static Map<String, Integer> createCategoryIds(List<ImageAnnotation> imageAnnotations) {
        final List<String> names = imageAnnotations.stream()
                .flatMap(annotation -> annotation.getBoundingShapeData().stream())
                .flatMap(BoundingShapeData::flatten)
                .map(BoundingShapeData::getCategoryName)
                .distinct()
                .sorted()
                .toList();
        final Map<String, Integer> ids = new LinkedHashMap<>();

        for(int i = 0; i < names.size(); ++i) {
            ids.put(names.get(i), i + 1);
        }

        return ids;
    }

    private static JsonArray createCategories(Map<String, Integer> categoryIds,
                                              Map<String, ObjectCategory> categoryNameToCategory) {
        final JsonArray categories = new JsonArray();

        categoryIds.forEach((name, id) -> {
            final JsonObject category = new JsonObject();
            category.addProperty(ID_KEY, id);
            category.addProperty(NAME_KEY, name);
            category.addProperty(SUPERCATEGORY_KEY, "");

            final ObjectCategory objectCategory = categoryNameToCategory.get(name);

            if(objectCategory != null) {
                category.addProperty(COLOR_KEY, ColorUtils.colorToHexString(objectCategory.getColor()));
            }

            categories.add(category);
        });

        return categories;
    }

    /**
     * Creates the annotation of one shape (without ids).
     *
     * @return the annotation, or null for a shape that can't be saved (an empty mask, a polygon with too few points)
     */
    private static JsonObject createAnnotation(BoundingShapeData shape, int imageWidth, int imageHeight,
                                               DecimalFormat decimalFormat) {
        final JsonObject annotation = new JsonObject();

        switch(shape) {
            case BoundingBoxData box -> {
                final Bounds bounds = box.getAbsoluteBoundsInImage(imageWidth, imageHeight);
                annotation.add(BBOX_KEY, numbers(decimalFormat, bounds.getMinX(), bounds.getMinY(),
                                                 bounds.getWidth(), bounds.getHeight()));
                annotation.add(AREA_KEY, number(decimalFormat, bounds.getWidth() * bounds.getHeight()));
            }
            case BoundingPolygonData polygon -> {
                final List<Double> points = polygon.getAbsolutePointsInImage(imageWidth, imageHeight);

                if(points.size() < 6) {
                    return null;
                }

                final JsonArray segmentation = new JsonArray();
                segmentation.add(numbers(decimalFormat, points.stream().mapToDouble(Double::doubleValue).toArray()));
                annotation.add(SEGMENTATION_KEY, segmentation);
                annotation.add(BBOX_KEY, polygonBounds(points, decimalFormat));
                annotation.add(AREA_KEY, number(decimalFormat, polygonArea(points)));
            }
            case BoundingMaskData maskData -> {
                final MaskBitmap mask = maskData.getMaskForImageSize(imageWidth, imageHeight);

                if(mask.isEmpty()) {
                    return null;
                }

                final JsonObject segmentation = new JsonObject();
                final JsonArray size = new JsonArray();
                size.add(imageHeight);
                size.add(imageWidth);
                segmentation.add(SIZE_KEY, size);
                segmentation.addProperty(COUNTS_KEY, COCORunLengthEncoding.encode(mask.toColumnMajorRunLengths()));
                annotation.add(SEGMENTATION_KEY, segmentation);
                annotation.add(BBOX_KEY, numbers(decimalFormat, mask.getMinX(), mask.getMinY(), mask.getWidth(),
                                                 mask.getHeight()));
                annotation.addProperty(AREA_KEY, mask.getArea());
            }
        }

        return annotation;
    }

    private static JsonArray polygonBounds(List<Double> points, DecimalFormat decimalFormat) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < points.size(); i += 2) {
            minX = Math.min(minX, points.get(i));
            maxX = Math.max(maxX, points.get(i));
            minY = Math.min(minY, points.get(i + 1));
            maxY = Math.max(maxY, points.get(i + 1));
        }

        return numbers(decimalFormat, minX, minY, maxX - minX, maxY - minY);
    }

    private static double polygonArea(List<Double> points) {
        // Shoelace formula.
        double doubleArea = 0;
        final int nrPoints = points.size() / 2;

        for(int i = 0; i < nrPoints; ++i) {
            final int next = (i + 1) % nrPoints;
            doubleArea += points.get(2 * i) * points.get(2 * next + 1) - points.get(2 * next) * points.get(2 * i + 1);
        }

        return Math.abs(doubleArea) / 2;
    }

    private static JsonArray numbers(DecimalFormat decimalFormat, double... values) {
        final JsonArray array = new JsonArray();

        for(double value : values) {
            array.add(number(decimalFormat, value));
        }

        return array;
    }

    private static JsonPrimitive number(DecimalFormat decimalFormat, double value) {
        // Rounded (and without a trailing ".0" for whole numbers).
        return new JsonPrimitive(new BigDecimal(decimalFormat.format(value)));
    }
}
