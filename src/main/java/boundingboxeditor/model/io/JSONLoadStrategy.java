package boundingboxeditor.model.io;

import boundingboxeditor.model.ImageMetaData;
import boundingboxeditor.model.Model;
import boundingboxeditor.model.ObjectCategory;
import boundingboxeditor.utils.ColorUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JSONLoadStrategy implements ImageAnnotationLoadStrategy {
    private static final String OBJECT_CATEGORY_SERIALIZED_NAME = "name";
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

    @Override
    public IOResult load(Model model, Path path, DoubleProperty progress) throws IOException {
        Set<String> fileNamesToLoad = model.getImageFileNameSet();
        Map<String, Integer> boundingShapeCountPerCategory = new ConcurrentHashMap<>(model.getCategoryToAssignedBoundingShapesCountMap());
        Map<String, ObjectCategory> nameToObjectCategoryMap = new ConcurrentHashMap<>(model.getObjectCategories().stream()
                .collect(Collectors.toMap(ObjectCategory::getName, Function.identity())));
        List<IOResult.ErrorInfoEntry> unParsedFileErrorMessages = Collections.synchronizedList(new ArrayList<>());
        Map<String, ImageMetaData> imageMetaDataMap = model.getImageFileNameToMetaDataMap();

        Gson gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(ImageAnnotation.class, (JsonDeserializer<ImageAnnotation>) (json, type, context) -> {
                    final ImageAnnotation imageAnnotation =
                            new ImageAnnotation(context.deserialize(json.getAsJsonObject().get(IMAGE_META_DATA_SERIALIZED_NAME), ImageMetaData.class),
                                    context.deserialize(json.getAsJsonObject().get(BOUNDING_SHAPE_DATA_SERIALIZED_NAME), new TypeToken<List<BoundingShapeData>>() {
                                    }.getType()));

                    if(fileNamesToLoad.contains(imageAnnotation.getImageFileName()) && !imageAnnotation.getBoundingShapeData().isEmpty()) {
                        return imageAnnotation;
                    }

                    return null;
                })
                .registerTypeAdapter(ImageMetaData.class, (JsonDeserializer<ImageMetaData>) (json, type, context) -> {
                    final JsonObject jsonObject = json.getAsJsonObject();

                    final String imageFileName = jsonObject.get(IMAGE_FILE_NAME_SERIALIZED_NAME).getAsString();

                    return imageMetaDataMap.getOrDefault(imageFileName,
                            new ImageMetaData(imageFileName));
                })
                .registerTypeAdapter(BoundingShapeData.class, (JsonDeserializer<BoundingShapeData>) (json, type, context) -> {
                    final JsonObject jsonObject = json.getAsJsonObject();

                    BoundingShapeData boundingShapeData = null;

                    if(jsonObject.has(BOUNDING_BOX_SERIALIZED_NAME)) {
                        boundingShapeData = context.deserialize(json, BoundingBoxData.class);
                    } else if(jsonObject.has(BOUNDING_POLYGON_SERIALIZED_NAME)) {
                        boundingShapeData = context.deserialize(json, BoundingPolygonData.class);
                    }

                    return boundingShapeData;
                })
                .registerTypeAdapter(ObjectCategory.class, (JsonDeserializer<ObjectCategory>) (json, type, context) -> {
                    final JsonObject jsonObject = json.getAsJsonObject();

                    final String categoryName = jsonObject.get(OBJECT_CATEGORY_SERIALIZED_NAME).getAsString();

                    Color categoryColor;

                    if(jsonObject.has(OBJECT_COLOR_SERIALIZED_NAME)) {
                        categoryColor = Color.web(jsonObject.get(OBJECT_COLOR_SERIALIZED_NAME).getAsString());
                    } else {
                        categoryColor = ColorUtils.createRandomColor();
                    }

                    final ObjectCategory category = nameToObjectCategoryMap.computeIfAbsent(categoryName,
                            key -> new ObjectCategory(key, categoryColor));

                    boundingShapeCountPerCategory.merge(category.getName(), 1, Integer::sum);

                    return category;
                })
                .registerTypeHierarchyAdapter(Bounds.class, (JsonDeserializer<Bounds>) (json, type, context) -> {
                    final JsonObject jsonObject = json.getAsJsonObject();

                    double minX = jsonObject.get(BOUNDS_MIN_X_SERIALIZED_NAME).getAsDouble();
                    double minY = jsonObject.get(BOUNDS_MIN_Y_SERIALIZED_NAME).getAsDouble();
                    double maxX = jsonObject.get(BOUNDS_MAX_X_SERIALIZED_NAME).getAsDouble();
                    double maxY = jsonObject.get(BOUNDS_MAX_Y_SERIALIZED_NAME).getAsDouble();

                    return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
                })
                .create();

        try(BufferedReader reader = Files.newBufferedReader(path)) {
            java.lang.reflect.Type imageAnnotationsType = new TypeToken<ArrayList<ImageAnnotation>>() {
            }.getType();
            List<ImageAnnotation> imageAnnotations = gson.fromJson(reader, imageAnnotationsType);

            model.getObjectCategories().setAll(nameToObjectCategoryMap.values());
            model.getCategoryToAssignedBoundingShapesCountMap().putAll(boundingShapeCountPerCategory);
            model.updateImageAnnotations(imageAnnotations);

            return new IOResult(
                    IOResult.OperationType.ANNOTATION_IMPORT,
                    imageAnnotations.size(),
                    unParsedFileErrorMessages
            );
        }
    }
}
