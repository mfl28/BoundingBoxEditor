/*
 * Copyright (C) 2024 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
import com.github.mfl28.boundingboxeditor.model.data.ObjectCategory;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationExportResult;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import com.google.gson.*;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Bounds;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class JSONSaveStrategy implements ImageAnnotationSaveStrategy {
    private static final DecimalFormat DECIMAL_FORMAT =
            new DecimalFormat("#.######", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    private static final String OBJECT_CATEGORY_SERIALIZED_NAME = "name";
    private static final String OBJECT_COLOR_SERIALIZED_NAME = "color";
    private static final String BOUNDS_MIN_X_SERIALIZED_NAME = "minX";
    private static final String BOUNDS_MIN_Y_SERIALIZED_NAME = "minY";
    private static final String BOUNDS_MAX_X_SERIALIZED_NAME = "maxX";
    private static final String BOUNDS_MAX_Y_SERIALIZED_NAME = "maxY";
    private static final String FILE_NAME_SERIALIZED_NAM = "fileName";
    private static final String FOLDER_NAME_SERIALIZED_NAME = "folderName";
    private static final String WIDTH_SERIALIZED_NAME = "width";
    private static final String HEIGHT_SERIALIZED_NAME = "height";
    private static final String DEPTH_SERIALIZED_NAME = "depth";
    private static final String DETAILS_SERIALIZED_NAME = "details";

    @Override
    public ImageAnnotationExportResult save(ImageAnnotationData annotations, Path destination,
                                            DoubleProperty progress) {
        final int totalNrAnnotations = annotations.imageAnnotations().size();
        final AtomicInteger nrProcessedAnnotations = new AtomicInteger(0);

        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(ImageAnnotationData.class,
                        (JsonSerializer<ImageAnnotationData>) (src, typeOfSrc, context) -> {
                            JsonArray serializedAnnotations = new JsonArray();

                            for(ImageAnnotation annotation : src.imageAnnotations()) {
                                serializedAnnotations.add(context.serialize(annotation));
                                progress.set(1.0 * nrProcessedAnnotations.incrementAndGet() /
                                        totalNrAnnotations);
                            }

                            return serializedAnnotations;
                        })
                .registerTypeAdapter(ObjectCategory.class,
                        (JsonSerializer<ObjectCategory>) (src, typeOfSrc, context) -> {
                            JsonObject categoryObject = new JsonObject();
                            categoryObject.add(OBJECT_CATEGORY_SERIALIZED_NAME,
                                    context.serialize(src.getName()));
                            categoryObject.add(OBJECT_COLOR_SERIALIZED_NAME,
                                    context.serialize(
                                            ColorUtils.colorToHexString(src.getColor())));

                            return categoryObject;
                        })
                .registerTypeAdapter(ImageMetaData.class,
                        (JsonSerializer<ImageMetaData>) (src, typeOfSrc, context) -> {
                            JsonObject imageMetaDataObject = new JsonObject();
                            imageMetaDataObject.add(FILE_NAME_SERIALIZED_NAM, context.serialize(src.getFileName()));

                            JsonObject imageMetaDataDetailsObject = new JsonObject();
                            imageMetaDataDetailsObject.add(FOLDER_NAME_SERIALIZED_NAME, context.serialize(src.getFolderName()));
                            imageMetaDataDetailsObject.add(WIDTH_SERIALIZED_NAME, context.serialize(src.getOrientedWidth()));
                            imageMetaDataDetailsObject.add(HEIGHT_SERIALIZED_NAME, context.serialize(src.getOrientedHeight()));
                            imageMetaDataDetailsObject.add(DEPTH_SERIALIZED_NAME, context.serialize(src.getImageDepth()));

                            imageMetaDataObject.add(DETAILS_SERIALIZED_NAME, imageMetaDataDetailsObject);

                            return imageMetaDataObject;
                        })
                .registerTypeHierarchyAdapter(Bounds.class, (JsonSerializer<Bounds>) (src, typeOfSrc, context) -> {
                    JsonObject boundsObject = new JsonObject();
                    boundsObject.add(BOUNDS_MIN_X_SERIALIZED_NAME, context.serialize(src.getMinX()));
                    boundsObject.add(BOUNDS_MIN_Y_SERIALIZED_NAME, context.serialize(src.getMinY()));
                    boundsObject.add(BOUNDS_MAX_X_SERIALIZED_NAME, context.serialize(src.getMaxX()));
                    boundsObject.add(BOUNDS_MAX_Y_SERIALIZED_NAME, context.serialize(src.getMaxY()));

                    return boundsObject;
                })
                .registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context)
                        -> new JsonPrimitive(Double.parseDouble(DECIMAL_FORMAT.format(src))))
                .create();

        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();

        try(BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            gson.toJson(annotations, writer);
        } catch(IOException e) {
            errorEntries.add(new IOErrorInfoEntry(destination.getFileName().toString(), e.getMessage()));
        }

        return new ImageAnnotationExportResult(
                errorEntries.isEmpty() ? totalNrAnnotations : 0,
                errorEntries
        );
    }
}
