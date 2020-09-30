/*
 * Copyright (C) 2020 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package boundingboxeditor.model.io;

import boundingboxeditor.model.data.*;
import boundingboxeditor.model.io.results.IOErrorInfoEntry;
import boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import boundingboxeditor.utils.ColorUtils;
import boundingboxeditor.utils.MathUtils;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class JSONLoadStrategy implements ImageAnnotationLoadStrategy {
    private static final String OBJECT_CATEGORY_NAME_SERIALIZED_NAME = "name";
    private static final String OBJECT_CATEGORY_SERIALIZED_NAME = "category";
    private static final String OBJECT_COLOR_SERIALIZED_NAME = "color";
    private static final String IMAGE_META_DATA_SERIALIZED_NAME = "image";
    private static final String BOUNDING_SHAPE_DATA_SERIALIZED_NAME = "objects";
    private static final String BOUNDS_MIN_X_SERIALIZED_NAME = "minX";
    private static final String BOUNDS_MIN_Y_SERIALIZED_NAME = "minY";
    private static final String BOUNDS_MAX_X_SERIALIZED_NAME = "maxX";
    private static final String BOUNDS_MAX_Y_SERIALIZED_NAME = "maxY";
    private static final String IMAGE_FILE_NAME_SERIALIZED_NAME = "fileName";
    private static final String BOUNDING_BOX_SERIALIZED_NAME = "bndbox";
    private static final String BOUNDING_POLYGON_SERIALIZED_NAME = "polygon";
    private static final String TAGS_SERIALIZED_NAME = "tags";
    private static final String PARTS_SERIALIZED_NAME = "parts";
    private static final String ELEMENT_LOCATION_ERROR_MESSAGE_PART = " element in ";
    private static final String IMAGE_ATTRIBUTION_MESSAGE_PART =
            ELEMENT_LOCATION_ERROR_MESSAGE_PART + "annotation for image ";
    private static final String MISSING_MESSAGE_PART = "Missing ";
    private static final String MISSING_CATEGORY_ERROR_MESSAGE =
            MISSING_MESSAGE_PART + OBJECT_CATEGORY_SERIALIZED_NAME + ELEMENT_LOCATION_ERROR_MESSAGE_PART;
    private static final String INVALID_COORDINATES_ERROR_MESSAGE = "Invalid coordinate value(s) in ";
    private static final String INVALID_COORDINATE_ERROR_MESSAGE = "Invalid coordinate value for ";
    private static final String INVALID_COORDINATE_NUMBER_ERROR_MESSAGE = "Invalid number of coordinates in ";
    private static final String INVALID_MESSAGE_PART = "Invalid ";
    private static final String INVALID_TAGS_ERROR_MESSAGE =
            INVALID_MESSAGE_PART + TAGS_SERIALIZED_NAME + " value(s) in ";
    private static final String INVALID_PARTS_ERROR_MESSAGE =
            INVALID_MESSAGE_PART + PARTS_SERIALIZED_NAME + " value(s) in ";
    private static final String MISSING_IMAGE_FILE_NAME_ERROR_MESSAGE =
            MISSING_MESSAGE_PART + IMAGE_META_DATA_SERIALIZED_NAME + " " + IMAGE_FILE_NAME_SERIALIZED_NAME +
                    " element.";
    private static final String MISSING_IMAGES_FIELD_ERROR_MESSAGE = "Missing images element.";
    private static final String MISSING_OBJECTS_FIELD_ERROR_MESSAGE =
            MISSING_MESSAGE_PART + BOUNDING_SHAPE_DATA_SERIALIZED_NAME + IMAGE_ATTRIBUTION_MESSAGE_PART;
    private static final String MISSING_CATEGORY_NAME_ERROR_MESSAGE =
            MISSING_MESSAGE_PART + OBJECT_CATEGORY_SERIALIZED_NAME + " name" + IMAGE_ATTRIBUTION_MESSAGE_PART;
    private static final String MISSING_BOUNDING_SHAPE_ERROR_MESSAGE =
            MISSING_MESSAGE_PART + BOUNDING_BOX_SERIALIZED_NAME + " or " + BOUNDING_POLYGON_SERIALIZED_NAME +
                    IMAGE_ATTRIBUTION_MESSAGE_PART;
    private static final String INVALID_COLOR_ERROR_MESSAGE = INVALID_MESSAGE_PART + OBJECT_COLOR_SERIALIZED_NAME;

    @Override
    public ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                            Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                            DoubleProperty progress) throws IOException {
        final Map<String, Integer> categoryNameToBoundingShapesCountMap = new HashMap<>();
        final List<IOErrorInfoEntry> errorInfoEntries = new ArrayList<>();
        final AtomicReference<String> currentImageFileName = new AtomicReference<>();
        final String annotationFileName = path.getFileName().toString();

        final java.lang.reflect.Type imageAnnotationListType =
                new TypeToken<List<ImageAnnotation>>() {}.getType();

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(imageAnnotationListType, new ImageAnnotationListDeserializer(progress))
                .registerTypeAdapter(ImageAnnotation.class,
                                     new ImageAnnotationDeserializer(errorInfoEntries, annotationFileName))
                .registerTypeAdapter(ImageMetaData.class,
                                     new ImageMetaDataDeserializer(errorInfoEntries, annotationFileName,
                                                                   currentImageFileName,
                                                                   filesToLoad))
                .registerTypeAdapter(BoundingShapeData.class, new BoundingShapeDataDeserializer(errorInfoEntries,
                                                                                                currentImageFileName,
                                                                                                annotationFileName))
                .registerTypeAdapter(BoundingBoxData.class, new BoundingBoxDataDeserializer(errorInfoEntries,
                                                                                            currentImageFileName,
                                                                                            annotationFileName,
                                                                                            existingCategoryNameToCategoryMap,
                                                                                            categoryNameToBoundingShapesCountMap))
                .registerTypeAdapter(BoundingPolygonData.class, new BoundingPolygonDataDeserializer(errorInfoEntries,
                                                                                                    currentImageFileName,
                                                                                                    annotationFileName,
                                                                                                    existingCategoryNameToCategoryMap,
                                                                                                    categoryNameToBoundingShapesCountMap))
                .registerTypeAdapter(ObjectCategory.class,
                                     new ObjectCategoryDeserializer(errorInfoEntries, currentImageFileName,
                                                                    annotationFileName))
                .registerTypeHierarchyAdapter(Bounds.class,
                                              new BoundsDeserializer(errorInfoEntries, currentImageFileName,
                                                                     annotationFileName))
                .create();

        try(final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final List<ImageAnnotation> imageAnnotations;

            try {
                imageAnnotations = gson.fromJson(reader, imageAnnotationListType);
            } catch(JsonIOException | JsonSyntaxException e) {
                final String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName, message));

                return new ImageAnnotationImportResult(
                        0,
                        errorInfoEntries,
                        ImageAnnotationData.empty()
                );
            }


            return new ImageAnnotationImportResult(
                    imageAnnotations != null ? imageAnnotations.size() : 0,
                    errorInfoEntries,
                    new ImageAnnotationData(imageAnnotations, categoryNameToBoundingShapesCountMap,
                                            existingCategoryNameToCategoryMap)
            );
        }
    }

    private static Optional<List<String>> parseBoundingShapeTags(JsonDeserializationContext context,
                                                                 JsonObject jsonObject,
                                                                 List<IOErrorInfoEntry> errorInfoEntries,
                                                                 String elementName,
                                                                 String annotationFileName,
                                                                 String currentFileName) {
        final java.lang.reflect.Type tagsType = new TypeToken<List<String>>() {}.getType();

        List<String> tags;

        try {
            tags = context.deserialize(jsonObject.get(TAGS_SERIALIZED_NAME), tagsType);
        } catch(JsonParseException e) {
            errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                      INVALID_TAGS_ERROR_MESSAGE +
                                                              elementName +
                                                              IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                              currentFileName + "."));
            return Optional.empty();
        }

        if(tags == null) {
            tags = new ArrayList<>();
        } else {
            tags.removeIf(tag -> tag == null || tag.isBlank());
        }

        return Optional.of(tags);
    }

    private static Optional<List<BoundingShapeData>> parseBoundingShapeDataParts(JsonDeserializationContext context,
                                                                                 JsonObject jsonObject,
                                                                                 List<IOErrorInfoEntry> errorInfoEntries,
                                                                                 String elementName,
                                                                                 String annotationFileName,
                                                                                 String currentFileName) {
        final java.lang.reflect.Type partsType = new TypeToken<List<BoundingShapeData>>() {}.getType();

        List<BoundingShapeData> parts;

        try {
            parts = context.deserialize(jsonObject.get(PARTS_SERIALIZED_NAME), partsType);
        } catch(JsonParseException e) {
            errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                      INVALID_PARTS_ERROR_MESSAGE +
                                                              elementName +
                                                              IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                              currentFileName + "."));
            return Optional.empty();
        }

        if(parts == null) {
            parts = Collections.emptyList();
        } else {
            parts.removeIf(Objects::isNull);

            if(parts.isEmpty()) {
                parts = Collections.emptyList();
            }
        }

        return Optional.of(parts);
    }

    private static Optional<ObjectCategory> parseObjectCategory(JsonDeserializationContext context,
                                                                JsonObject jsonObject,
                                                                List<IOErrorInfoEntry> errorInfoEntries,
                                                                String elementName,
                                                                String annotationFileName,
                                                                String currentFileName) {
        if(!jsonObject.has(OBJECT_CATEGORY_SERIALIZED_NAME)) {
            errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                      MISSING_CATEGORY_ERROR_MESSAGE +
                                                              elementName +
                                                              IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                              currentFileName + "."));
            return Optional.empty();
        }

        return Optional.ofNullable(context.deserialize(jsonObject.get(OBJECT_CATEGORY_SERIALIZED_NAME),
                                                       ObjectCategory.class));
    }

    private static class BoundingShapeDataDeserializer implements JsonDeserializer<BoundingShapeData> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;

        public BoundingShapeDataDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                             AtomicReference<String> currentFileName,
                                             String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFileName = currentFileName;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public BoundingShapeData deserialize(JsonElement json, java.lang.reflect.Type type,
                                             JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            BoundingShapeData boundingShapeData = null;

            if(jsonObject.has(BOUNDING_BOX_SERIALIZED_NAME)) {
                boundingShapeData = context.deserialize(json, BoundingBoxData.class);
            } else if(jsonObject.has(BOUNDING_POLYGON_SERIALIZED_NAME)) {
                boundingShapeData = context.deserialize(json, BoundingPolygonData.class);
            } else {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          MISSING_BOUNDING_SHAPE_ERROR_MESSAGE +
                                                                  currentFileName
                                                                          .get() + "."));
            }

            return boundingShapeData;
        }
    }

    private static class ObjectCategoryDeserializer implements JsonDeserializer<ObjectCategory> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;

        public ObjectCategoryDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                          AtomicReference<String> currentFileName,
                                          String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFileName = currentFileName;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public ObjectCategory deserialize(JsonElement json, java.lang.reflect.Type type,
                                          JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            if(!jsonObject.has(OBJECT_CATEGORY_NAME_SERIALIZED_NAME)) {
                errorInfoEntries
                        .add(new IOErrorInfoEntry(annotationFileName,
                                                  MISSING_CATEGORY_NAME_ERROR_MESSAGE + currentFileName.get() +
                                                          "."));
                return null;
            }

            final String categoryName = jsonObject.get(OBJECT_CATEGORY_NAME_SERIALIZED_NAME).getAsString();

            Color categoryColor;

            if(jsonObject.has(OBJECT_COLOR_SERIALIZED_NAME)) {
                try {
                    categoryColor = Color.web(jsonObject.get(OBJECT_COLOR_SERIALIZED_NAME).getAsString());
                } catch(IllegalArgumentException | ClassCastException e) {
                    errorInfoEntries
                            .add(new IOErrorInfoEntry(annotationFileName, INVALID_COLOR_ERROR_MESSAGE +
                                    IMAGE_ATTRIBUTION_MESSAGE_PART + currentFileName.get() + "."));
                    return null;
                }
            } else {
                categoryColor = ColorUtils.createRandomColor();
            }

            return new ObjectCategory(categoryName, categoryColor);
        }
    }

    private static class BoundsDeserializer implements JsonDeserializer<Bounds> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;

        public BoundsDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                  AtomicReference<String> currentFileName, String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFileName = currentFileName;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public Bounds deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            final Optional<Double> minX = parseCoordinateField(jsonObject, BOUNDS_MIN_X_SERIALIZED_NAME);

            if(minX.isEmpty()) {
                return null;
            }

            final Optional<Double> minY = parseCoordinateField(jsonObject, BOUNDS_MIN_Y_SERIALIZED_NAME);

            if(minY.isEmpty()) {
                return null;
            }

            final Optional<Double> maxX = parseCoordinateField(jsonObject, BOUNDS_MAX_X_SERIALIZED_NAME);

            if(maxX.isEmpty()) {
                return null;
            }

            final Optional<Double> maxY = parseCoordinateField(jsonObject, BOUNDS_MAX_Y_SERIALIZED_NAME);

            if(maxY.isEmpty()) {
                return null;
            }

            return new BoundingBox(minX.get(), minY.get(), maxX.get() - minX.get(), maxY.get() - minY.get());
        }

        private Optional<Double> parseCoordinateField(JsonObject jsonObject, String name) {
            if(!jsonObject.has(name)) {
                errorInfoEntries
                        .add(new IOErrorInfoEntry(annotationFileName,
                                                  MISSING_MESSAGE_PART + name +
                                                          ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                          BOUNDING_BOX_SERIALIZED_NAME +
                                                          IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                          currentFileName.get() + "."));
                return Optional.empty();
            }

            double value;

            try {
                value = jsonObject.get(name).getAsDouble();
            } catch(ClassCastException | NumberFormatException e) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          INVALID_COORDINATE_ERROR_MESSAGE + name +
                                                                  ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                                  BOUNDING_BOX_SERIALIZED_NAME +
                                                                  IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                  currentFileName.get() + "."));
                return Optional.empty();
            }

            if(!MathUtils.isWithin(value, 0.0, 1.0)) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          INVALID_COORDINATE_ERROR_MESSAGE + name +
                                                                  ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                                  BOUNDING_BOX_SERIALIZED_NAME +
                                                                  IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                  currentFileName.get() + "."));
                return Optional.empty();
            }

            return Optional.of(value);
        }
    }

    private static class ImageMetaDataDeserializer implements JsonDeserializer<ImageMetaData> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final String annotationFileName;
        private final AtomicReference<String> currentFileName;
        private final Set<String> fileNamesToLoad;

        public ImageMetaDataDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                         String annotationFileName, AtomicReference<String> currentFileName,
                                         Set<String> fileNamesToLoad) {
            this.errorInfoEntries = errorInfoEntries;
            this.annotationFileName = annotationFileName;
            this.currentFileName = currentFileName;
            this.fileNamesToLoad = fileNamesToLoad;
        }

        @Override
        public ImageMetaData deserialize(JsonElement json, java.lang.reflect.Type type,
                                         JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            if(!jsonObject.has(IMAGE_FILE_NAME_SERIALIZED_NAME)) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          MISSING_IMAGE_FILE_NAME_ERROR_MESSAGE));
                return null;
            }

            final String imageFileName = jsonObject.get(IMAGE_FILE_NAME_SERIALIZED_NAME).getAsString();

            if(!fileNamesToLoad.contains(imageFileName)) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          "Image " + imageFileName +
                                                                  " does not belong to currently loaded image files."));
                return null;
            }

            currentFileName.set(imageFileName);

            return new ImageMetaData(imageFileName);
        }
    }

    private static class ImageAnnotationDeserializer implements JsonDeserializer<ImageAnnotation> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final String annotationFileName;

        public ImageAnnotationDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                           String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public ImageAnnotation deserialize(JsonElement json, java.lang.reflect.Type type,
                                           JsonDeserializationContext context) {

            if(!json.getAsJsonObject().has(IMAGE_META_DATA_SERIALIZED_NAME)) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          MISSING_IMAGES_FIELD_ERROR_MESSAGE));
                return null;
            }

            final ImageMetaData imageMetaData =
                    context.deserialize(json.getAsJsonObject().get(IMAGE_META_DATA_SERIALIZED_NAME),
                                        ImageMetaData.class);

            if(imageMetaData == null) {
                return null;
            }

            if(!json.getAsJsonObject().has(BOUNDING_SHAPE_DATA_SERIALIZED_NAME)) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          MISSING_OBJECTS_FIELD_ERROR_MESSAGE
                                                                  + imageMetaData.getFileName() + "."));
                return null;
            }

            final java.lang.reflect.Type boundingShapeDataListType = new TypeToken<List<BoundingShapeData>>() {
            }.getType();
            final List<BoundingShapeData> boundingShapeDataList =
                    context.deserialize(json.getAsJsonObject().get(BOUNDING_SHAPE_DATA_SERIALIZED_NAME),
                                        boundingShapeDataListType);

            boundingShapeDataList.removeIf(Objects::isNull);

            if(boundingShapeDataList.isEmpty()) {
                return null;
            }

            return new ImageAnnotation(imageMetaData, boundingShapeDataList);
        }
    }

    private static class BoundingBoxDataDeserializer implements JsonDeserializer<BoundingBoxData> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;
        private final Map<String, ObjectCategory> nameToObjectCategoryMap;
        private final Map<String, Integer> boundingShapeCountPerCategory;

        public BoundingBoxDataDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                           AtomicReference<String> currentFileName, String annotationFileName,
                                           Map<String, ObjectCategory> nameToObjectCategoryMap,
                                           Map<String, Integer> boundingShapeCountPerCategory) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFileName = currentFileName;
            this.annotationFileName = annotationFileName;
            this.nameToObjectCategoryMap = nameToObjectCategoryMap;
            this.boundingShapeCountPerCategory = boundingShapeCountPerCategory;
        }

        @Override
        public BoundingBoxData deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                           JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            final Optional<ObjectCategory> parsedObjectCategory =
                    parseObjectCategory(context, jsonObject, errorInfoEntries,
                                        BOUNDING_BOX_SERIALIZED_NAME, annotationFileName,
                                        currentFileName.get());

            if(parsedObjectCategory.isEmpty()) {
                return null;
            }

            final Bounds bounds =
                    context.deserialize(json.getAsJsonObject().get(BOUNDING_BOX_SERIALIZED_NAME), Bounds.class);

            if(bounds == null) {
                return null;
            }

            final Optional<List<String>> tags = parseBoundingShapeTags(context, jsonObject, errorInfoEntries,
                                                                       BOUNDING_BOX_SERIALIZED_NAME, annotationFileName,
                                                                       currentFileName.get());

            if(tags.isEmpty()) {
                return null;
            }

            final Optional<List<BoundingShapeData>> parts = parseBoundingShapeDataParts(context, jsonObject,
                                                                                        errorInfoEntries,
                                                                                        BOUNDING_BOX_SERIALIZED_NAME,
                                                                                        annotationFileName,
                                                                                        currentFileName.get());

            if(parts.isEmpty()) {
                return null;
            }

            final ObjectCategory objectCategory =
                    nameToObjectCategoryMap.computeIfAbsent(parsedObjectCategory.get().getName(),
                                                            key -> parsedObjectCategory.get());

            boundingShapeCountPerCategory.merge(objectCategory.getName(), 1, Integer::sum);

            final BoundingBoxData boundingBoxData = new BoundingBoxData(objectCategory, bounds, tags.get());
            boundingBoxData.setParts(parts.get());

            return boundingBoxData;
        }

    }

    private static class BoundingPolygonDataDeserializer implements JsonDeserializer<BoundingPolygonData> {
        private final List<IOErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;
        private final Map<String, ObjectCategory> nameToObjectCategoryMap;
        private final Map<String, Integer> boundingShapeCountPerCategory;

        public BoundingPolygonDataDeserializer(List<IOErrorInfoEntry> errorInfoEntries,
                                               AtomicReference<String> currentFileName,
                                               String annotationFileName,
                                               Map<String, ObjectCategory> nameToObjectCategoryMap,
                                               Map<String, Integer> boundingShapeCountPerCategory) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFileName = currentFileName;
            this.annotationFileName = annotationFileName;
            this.nameToObjectCategoryMap = nameToObjectCategoryMap;
            this.boundingShapeCountPerCategory = boundingShapeCountPerCategory;
        }

        @Override
        public BoundingPolygonData deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                               JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            final Optional<ObjectCategory> parsedObjectCategory = parseObjectCategory(context, jsonObject,
                                                                                      errorInfoEntries,
                                                                                      BOUNDING_POLYGON_SERIALIZED_NAME,
                                                                                      annotationFileName,
                                                                                      currentFileName.get());
            context.deserialize(json.getAsJsonObject().get(OBJECT_CATEGORY_SERIALIZED_NAME),
                                ObjectCategory.class);

            if(parsedObjectCategory.isEmpty()) {
                return null;
            }

            final java.lang.reflect.Type pointsType = new TypeToken<List<Double>>() {
            }.getType();
            List<Double> points;

            try {
                points = context.deserialize(json.getAsJsonObject().get(BOUNDING_POLYGON_SERIALIZED_NAME), pointsType);
            } catch(JsonParseException | NumberFormatException e) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          INVALID_COORDINATES_ERROR_MESSAGE +
                                                                  BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                  IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                  currentFileName.get() + "."));
                return null;
            }

            if(points == null || points.isEmpty() || points.size() % 2 != 0) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          INVALID_COORDINATE_NUMBER_ERROR_MESSAGE +
                                                                  BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                  IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                  currentFileName.get() + "."));
                return null;
            }

            if(!points.stream().allMatch(value -> MathUtils.isWithin(value, 0.0, 1.0))) {
                errorInfoEntries.add(new IOErrorInfoEntry(annotationFileName,
                                                          INVALID_COORDINATES_ERROR_MESSAGE +
                                                                  BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                  IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                  currentFileName.get() + "."));
                return null;
            }

            final Optional<List<String>> tags = parseBoundingShapeTags(context, jsonObject, errorInfoEntries,
                                                                       BOUNDING_POLYGON_SERIALIZED_NAME,
                                                                       annotationFileName,
                                                                       currentFileName.get());

            if(tags.isEmpty()) {
                return null;
            }

            final Optional<List<BoundingShapeData>> parts = parseBoundingShapeDataParts(context, jsonObject,
                                                                                        errorInfoEntries,
                                                                                        BOUNDING_POLYGON_SERIALIZED_NAME,
                                                                                        annotationFileName,
                                                                                        currentFileName.get());

            if(parts.isEmpty()) {
                return null;
            }

            final ObjectCategory objectCategory =
                    nameToObjectCategoryMap.computeIfAbsent(parsedObjectCategory.get().getName(),
                                                            key -> parsedObjectCategory.get());

            boundingShapeCountPerCategory.merge(objectCategory.getName(), 1, Integer::sum);

            final BoundingPolygonData boundingPolygonData = new BoundingPolygonData(objectCategory, points, tags.get());
            boundingPolygonData.setParts(parts.get());

            return boundingPolygonData;
        }
    }

    private static class ImageAnnotationListDeserializer implements JsonDeserializer<List<ImageAnnotation>> {
        final DoubleProperty progress;

        public ImageAnnotationListDeserializer(DoubleProperty progress) {
            this.progress = progress;
        }

        @Override
        public List<ImageAnnotation> deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                                 JsonDeserializationContext context) {
            final JsonArray jsonArray = json.getAsJsonArray();

            int totalNrAnnotations = jsonArray.size();
            final AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);

            return StreamSupport.stream(jsonArray.spliterator(), false)
                                .map(jsonElement -> {
                                    progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrAnnotations);

                                    return (ImageAnnotation) context.deserialize(jsonElement, ImageAnnotation.class);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }
    }
}
