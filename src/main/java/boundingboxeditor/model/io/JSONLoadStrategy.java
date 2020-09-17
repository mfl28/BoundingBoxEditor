package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.Model;
import boundingboxeditor.model.ObjectCategory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        final Set<String> fileNamesToLoad = model.getImageFileNameSet();
        final Map<String, Integer> boundingShapeCountPerCategory =
                new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingShapesCountMap());
        final Map<String, ObjectCategory> nameToObjectCategoryMap =
                new ConcurrentHashMap<>(model.getObjectCategories().stream()
                                             .collect(Collectors.toMap(ObjectCategory::getName, Function.identity())));
        final List<IOResult.ErrorInfoEntry> errorInfoEntries = Collections.synchronizedList(new ArrayList<>());
        final Map<String, ImageMetaData> imageMetaDataMap = model.getImageFileNameToMetaDataMap();
        final AtomicReference<String> currentFilename = new AtomicReference<>();
        final String annotationFileName = path.getFileName().toString();

        final java.lang.reflect.Type imageAnnotationListType =
                new TypeToken<List<ImageAnnotation>>() {}.getType();

        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(imageAnnotationListType, new ImageAnnotationDataListDeserializer(progress))
                .registerTypeAdapter(ImageAnnotation.class,
                                     new ImageAnnotationDeserializer(errorInfoEntries, annotationFileName))
                .registerTypeAdapter(ImageMetaData.class,
                                     new ImageMetaDataDeserializer(errorInfoEntries, annotationFileName,
                                                                   currentFilename,
                                                                   fileNamesToLoad,
                                                                   imageMetaDataMap))
                .registerTypeAdapter(BoundingShapeData.class, new BoundingShapeDataDeserializer(errorInfoEntries,
                                                                                                currentFilename,
                                                                                                annotationFileName))
                .registerTypeAdapter(BoundingBoxData.class, new BoundingBoxDataDeserializer(errorInfoEntries,
                                                                                            currentFilename,
                                                                                            annotationFileName,
                                                                                            nameToObjectCategoryMap,
                                                                                            boundingShapeCountPerCategory))
                .registerTypeAdapter(BoundingPolygonData.class, new BoundingPolygonDataDeserializer(errorInfoEntries,
                                                                                                    currentFilename,
                                                                                                    annotationFileName,
                                                                                                    nameToObjectCategoryMap,
                                                                                                    boundingShapeCountPerCategory))
                .registerTypeAdapter(ObjectCategory.class,
                                     new ObjectCategoryDeserializer(errorInfoEntries, currentFilename,
                                                                    annotationFileName))
                .registerTypeHierarchyAdapter(Bounds.class, new BoundsDeserializer(errorInfoEntries, currentFilename,
                                                                                   annotationFileName))
                .create();

        try(final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            final List<ImageAnnotation> imageAnnotations;

            try {
                imageAnnotations = gson.fromJson(reader, imageAnnotationListType);
            } catch(JsonIOException | JsonSyntaxException e) {
                final String message = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();

                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName, message));

                return new IOResult(
                        IOResult.OperationType.ANNOTATION_IMPORT,
                        0,
                        errorInfoEntries
                );
            }

            if(imageAnnotations != null && !imageAnnotations.isEmpty()) {
                model.getObjectCategories().setAll(nameToObjectCategoryMap.values());
                model.getCategoryToAssignedBoundingShapesCountMap().putAll(boundingShapeCountPerCategory);
                model.updateImageAnnotations(imageAnnotations);
            }

            return new IOResult(
                    IOResult.OperationType.ANNOTATION_IMPORT,
                    imageAnnotations != null ? imageAnnotations.size() : 0,
                    errorInfoEntries
            );
        }
    }

    private static Optional<List<String>> parseBoundingShapeTags(JsonDeserializationContext context,
                                                                 JsonObject jsonObject,
                                                                 List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                                                 String elementName,
                                                                 String annotationFileName,
                                                                 String currentFileName) {
        final java.lang.reflect.Type tagsType = new TypeToken<List<String>>() {}.getType();

        List<String> tags;

        try {
            tags = context.deserialize(jsonObject.get(TAGS_SERIALIZED_NAME), tagsType);
        } catch(JsonParseException e) {
            errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
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
                                                                                 List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                                                                 String elementName,
                                                                                 String annotationFileName,
                                                                                 String currentFileName) {
        final java.lang.reflect.Type partsType = new TypeToken<List<BoundingShapeData>>() {}.getType();

        List<BoundingShapeData> parts;

        try {
            parts = context.deserialize(jsonObject.get(PARTS_SERIALIZED_NAME), partsType);
        } catch(JsonParseException e) {
            errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
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
                                                                List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                                                String elementName,
                                                                String annotationFileName,
                                                                String currentFileName) {
        if(!jsonObject.has(OBJECT_CATEGORY_SERIALIZED_NAME)) {
            errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
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
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;

        public BoundingShapeDataDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
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
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 MISSING_BOUNDING_SHAPE_ERROR_MESSAGE +
                                                                         currentFileName
                                                                                 .get() + "."));
            }

            return boundingShapeData;
        }
    }

    private static class ObjectCategoryDeserializer implements JsonDeserializer<ObjectCategory> {
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFilename;
        private final String annotationFileName;

        public ObjectCategoryDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                          AtomicReference<String> currentFilename,
                                          String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFilename = currentFilename;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public ObjectCategory deserialize(JsonElement json, java.lang.reflect.Type type,
                                          JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            if(!jsonObject.has(OBJECT_CATEGORY_NAME_SERIALIZED_NAME)) {
                errorInfoEntries
                        .add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                         MISSING_CATEGORY_NAME_ERROR_MESSAGE + currentFilename.get() +
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
                            .add(new IOResult.ErrorInfoEntry(annotationFileName, INVALID_COLOR_ERROR_MESSAGE +
                                    IMAGE_ATTRIBUTION_MESSAGE_PART + currentFilename.get() + "."));
                    return null;
                }
            } else {
                categoryColor = ColorUtils.createRandomColor();
            }

            return new ObjectCategory(categoryName, categoryColor);
        }
    }

    private static class BoundsDeserializer implements JsonDeserializer<Bounds> {
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFilename;
        private final String annotationFileName;

        public BoundsDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                  AtomicReference<String> currentFilename, String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFilename = currentFilename;
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
                        .add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                         MISSING_MESSAGE_PART + name +
                                                                 ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                                 BOUNDING_BOX_SERIALIZED_NAME +
                                                                 IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                 currentFilename.get() + "."));
                return Optional.empty();
            }

            double value;

            try {
                value = jsonObject.get(name).getAsDouble();
            } catch(ClassCastException | NumberFormatException e) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 INVALID_COORDINATE_ERROR_MESSAGE + name +
                                                                         ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                                         BOUNDING_BOX_SERIALIZED_NAME +
                                                                         IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                         currentFilename.get() + "."));
                return Optional.empty();
            }

            if(!MathUtils.isWithin(value, 0.0, 1.0)) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 INVALID_COORDINATE_ERROR_MESSAGE + name +
                                                                         ELEMENT_LOCATION_ERROR_MESSAGE_PART +
                                                                         BOUNDING_BOX_SERIALIZED_NAME +
                                                                         IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                         currentFilename.get() + "."));
                return Optional.empty();
            }

            return Optional.of(value);
        }
    }

    private static class ImageMetaDataDeserializer implements JsonDeserializer<ImageMetaData> {
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final String annotationFileName;
        private final AtomicReference<String> currentFileName;
        private final Set<String> fileNamesToLoad;
        private final Map<String, ImageMetaData> imageMetaDataMap;

        public ImageMetaDataDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                         String annotationFileName, AtomicReference<String> currentFileName,
                                         Set<String> fileNamesToLoad,
                                         Map<String, ImageMetaData> imageMetaDataMap) {
            this.errorInfoEntries = errorInfoEntries;
            this.annotationFileName = annotationFileName;
            this.currentFileName = currentFileName;
            this.fileNamesToLoad = fileNamesToLoad;
            this.imageMetaDataMap = imageMetaDataMap;
        }

        @Override
        public ImageMetaData deserialize(JsonElement json, java.lang.reflect.Type type,
                                         JsonDeserializationContext context) {
            final JsonObject jsonObject = json.getAsJsonObject();

            if(!jsonObject.has(IMAGE_FILE_NAME_SERIALIZED_NAME)) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 MISSING_IMAGE_FILE_NAME_ERROR_MESSAGE));
                return null;
            }

            final String imageFileName = jsonObject.get(IMAGE_FILE_NAME_SERIALIZED_NAME).getAsString();

            if(!fileNamesToLoad.contains(imageFileName)) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 "Image " + imageFileName +
                                                                         " does not belong to currently loaded image files."));
                return null;
            }

            currentFileName.set(imageFileName);

            return imageMetaDataMap.getOrDefault(imageFileName,
                                                 new ImageMetaData(imageFileName));
        }
    }

    private static class ImageAnnotationDeserializer implements JsonDeserializer<ImageAnnotation> {
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final String annotationFileName;

        public ImageAnnotationDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                           String annotationFileName) {
            this.errorInfoEntries = errorInfoEntries;
            this.annotationFileName = annotationFileName;
        }

        @Override
        public ImageAnnotation deserialize(JsonElement json, java.lang.reflect.Type type,
                                           JsonDeserializationContext context) {

            if(!json.getAsJsonObject().has(IMAGE_META_DATA_SERIALIZED_NAME)) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
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
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
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
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFileName;
        private final String annotationFileName;
        private final Map<String, ObjectCategory> nameToObjectCategoryMap;
        private final Map<String, Integer> boundingShapeCountPerCategory;

        public BoundingBoxDataDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
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
        private final List<IOResult.ErrorInfoEntry> errorInfoEntries;
        private final AtomicReference<String> currentFilename;
        private final String annotationFileName;
        private final Map<String, ObjectCategory> nameToObjectCategoryMap;
        private final Map<String, Integer> boundingShapeCountPerCategory;

        public BoundingPolygonDataDeserializer(List<IOResult.ErrorInfoEntry> errorInfoEntries,
                                               AtomicReference<String> currentFilename,
                                               String annotationFileName,
                                               Map<String, ObjectCategory> nameToObjectCategoryMap,
                                               Map<String, Integer> boundingShapeCountPerCategory) {
            this.errorInfoEntries = errorInfoEntries;
            this.currentFilename = currentFilename;
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
                                                                                      currentFilename.get());
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
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 INVALID_COORDINATES_ERROR_MESSAGE +
                                                                         BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                         IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                         currentFilename.get() + "."));
                return null;
            }

            if(points == null || points.isEmpty() || points.size() % 2 != 0) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 INVALID_COORDINATE_NUMBER_ERROR_MESSAGE +
                                                                         BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                         IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                         currentFilename.get() + "."));
                return null;
            }

            if(!points.stream().allMatch(value -> MathUtils.isWithin(value, 0.0, 1.0))) {
                errorInfoEntries.add(new IOResult.ErrorInfoEntry(annotationFileName,
                                                                 INVALID_COORDINATES_ERROR_MESSAGE +
                                                                         BOUNDING_POLYGON_SERIALIZED_NAME +
                                                                         IMAGE_ATTRIBUTION_MESSAGE_PART +
                                                                         currentFilename.get() + "."));
                return null;
            }

            final Optional<List<String>> tags = parseBoundingShapeTags(context, jsonObject, errorInfoEntries,
                                                                       BOUNDING_POLYGON_SERIALIZED_NAME,
                                                                       annotationFileName,
                                                                       currentFilename.get());

            if(tags.isEmpty()) {
                return null;
            }

            final Optional<List<BoundingShapeData>> parts = parseBoundingShapeDataParts(context, jsonObject,
                                                                                        errorInfoEntries,
                                                                                        BOUNDING_POLYGON_SERIALIZED_NAME,
                                                                                        annotationFileName,
                                                                                        currentFilename.get());

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

    private static class ImageAnnotationDataListDeserializer implements JsonDeserializer<List<ImageAnnotation>> {
        final DoubleProperty progress;

        public ImageAnnotationDataListDeserializer(DoubleProperty progress) {
            this.progress = progress;
        }

        @Override
        public List<ImageAnnotation> deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                                                 JsonDeserializationContext context) {
            final JsonArray jsonArray = json.getAsJsonArray();

            int totalNrAnnotations = jsonArray.size();
            final AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);

            return StreamSupport.stream(jsonArray.spliterator(), true)
                                .map(jsonElement -> {
                                    progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() / totalNrAnnotations);

                                    return (ImageAnnotation) context.deserialize(jsonElement, ImageAnnotation.class);
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        }
    }
}
