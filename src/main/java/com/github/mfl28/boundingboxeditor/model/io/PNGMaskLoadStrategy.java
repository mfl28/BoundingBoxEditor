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
import javafx.beans.property.DoubleProperty;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads masks from PNG masks (see {@link PNGMasks}). Each object number in an object mask becomes a mask, with the
 * category that most of its pixels have in the category mask. Without an object mask folder, each category in a
 * category mask becomes one mask.
 */
public class PNGMaskLoadStrategy implements ImageAnnotationLoadStrategy {
    @Override
    public ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                            Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                            DoubleProperty progress) throws IOException {
        progress.set(0);
        final List<IOErrorInfoEntry> errorEntries = new ArrayList<>();
        final Path classesFile = path.resolve(PNGMasks.CLASSES_FILE_NAME);
        final Path objectFolder = path.resolve(PNGMasks.OBJECT_FOLDER_NAME);
        final Path classFolder = path.resolve(PNGMasks.CLASS_FOLDER_NAME);

        if(!Files.isRegularFile(classesFile)) {
            errorEntries.add(new IOErrorInfoEntry(path.getFileName().toString(), "The file \""
                    + PNGMasks.CLASSES_FILE_NAME + "\" with the category names is missing."));
            return emptyResult(errorEntries, existingCategoryNameToCategoryMap);
        }

        if(!Files.isDirectory(classFolder)) {
            errorEntries.add(new IOErrorInfoEntry(path.getFileName().toString(), "The folder \""
                    + PNGMasks.CLASS_FOLDER_NAME + "\" with the category masks is missing."));
            return emptyResult(errorEntries, existingCategoryNameToCategoryMap);
        }

        final List<String> categoryNames = Files.readAllLines(classesFile, StandardCharsets.UTF_8).stream()
                .map(String::strip)
                .toList();
        final boolean hasObjectMasks = Files.isDirectory(objectFolder);
        final List<Path> maskFiles = listPngFiles(hasObjectMasks ? objectFolder : classFolder);
        final Map<String, List<String>> baseNameToImageFileNames = filesToLoad.stream()
                .collect(Collectors.groupingBy(FilenameUtils::getBaseName));
        final Map<String, ImageAnnotation> fileNameToAnnotation = new LinkedHashMap<>();
        final Map<String, Integer> categoryNameToShapeCount = new HashMap<>();

        for(int i = 0; i < maskFiles.size(); ++i) {
            final Path maskFile = maskFiles.get(i);
            final String maskFileName = maskFile.getFileName().toString();
            final List<String> imageFileNames =
                    baseNameToImageFileNames.getOrDefault(FilenameUtils.getBaseName(maskFileName), List.of());

            if(imageFileNames.size() != 1) {
                errorEntries.add(new IOErrorInfoEntry(maskFileName, imageFileNames.isEmpty()
                        ? "No image of the currently loaded image files has the name of the mask."
                        : "Several images (" + String.join(", ", imageFileNames) + ") have the name of the mask."));
            } else {
                try {
                    final PNGMasks.Values classes = PNGMasks.read(classFolder.resolve(maskFileName));
                    final PNGMasks.Values objects = hasObjectMasks ? PNGMasks.read(maskFile) : classes;

                    if(objects.width() != classes.width() || objects.height() != classes.height()) {
                        throw new IOException("The object and category masks have different sizes.");
                    }

                    final List<BoundingShapeData> shapes = createMasks(objects, classes, categoryNames,
                                                                       existingCategoryNameToCategoryMap,
                                                                       maskFileName, errorEntries);

                    if(!shapes.isEmpty()) {
                        final String imageFileName = imageFileNames.getFirst();
                        fileNameToAnnotation.put(imageFileName, new ImageAnnotation(new ImageMetaData(imageFileName),
                                                                                    shapes));
                        shapes.forEach(shape -> categoryNameToShapeCount.merge(shape.getCategoryName(), 1,
                                                                               Integer::sum));
                    }
                } catch(IOException e) {
                    errorEntries.add(new IOErrorInfoEntry(maskFileName, e.getMessage() != null ? e.getMessage()
                            : "The mask can't be read."));
                }
            }

            progress.set((i + 1.0) / maskFiles.size());
        }

        progress.set(1.0);
        return new ImageAnnotationImportResult(fileNameToAnnotation.size(), errorEntries,
                                               new ImageAnnotationData(fileNameToAnnotation.values(),
                                                                       categoryNameToShapeCount,
                                                                       existingCategoryNameToCategoryMap));
    }

    private static List<Path> listPngFiles(Path folder) throws IOException {
        try(Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> Files.isRegularFile(file)
                                        && file.getFileName().toString().toLowerCase(Locale.ROOT)
                                               .endsWith(PNGMasks.PNG_EXTENSION))
                        .sorted()
                        .toList();
        }
    }

    /**
     * Creates the masks of one image, in the order of their numbers.
     */
    private static List<BoundingShapeData> createMasks(PNGMasks.Values objects, PNGMasks.Values classes,
                                                       List<String> categoryNames,
                                                       Map<String, ObjectCategory> categoryNameToCategory,
                                                       String maskFileName, List<IOErrorInfoEntry> errorEntries) {
        // The bounds of each object, so that each mask is built from its own region only.
        final Map<Integer, int[]> objectBounds = new TreeMap<>();
        // Per object, how many of its pixels have which category.
        final Map<Integer, Map<Integer, Integer>> objectClassCounts = new HashMap<>();

        for(int y = 0; y < objects.height(); ++y) {
            for(int x = 0; x < objects.width(); ++x) {
                final int object = objects.get(x, y);

                if(object == PNGMasks.BACKGROUND || object == PNGMasks.IGNORED) {
                    continue;
                }

                int[] bounds = objectBounds.get(object);

                if(bounds == null) {
                    bounds = new int[]{x, y, x + 1, y + 1};
                    objectBounds.put(object, bounds);
                }

                bounds[0] = Math.min(bounds[0], x);
                bounds[1] = Math.min(bounds[1], y);
                bounds[2] = Math.max(bounds[2], x + 1);
                bounds[3] = Math.max(bounds[3], y + 1);
                objectClassCounts.computeIfAbsent(object, key -> new HashMap<>())
                                 .merge(classes.get(x, y), 1, Integer::sum);
            }
        }

        final List<BoundingShapeData> shapes = new ArrayList<>();

        for(Map.Entry<Integer, int[]> entry : objectBounds.entrySet()) {
            final int object = entry.getKey();
            final int classIndex = objectClassCounts.get(object).entrySet().stream()
                    .filter(count -> count.getKey() != PNGMasks.BACKGROUND && count.getKey() != PNGMasks.IGNORED)
                    .max(Map.Entry.<Integer, Integer>comparingByValue()
                                  .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                    .map(Map.Entry::getKey)
                    .orElse(PNGMasks.BACKGROUND);

            if(classIndex == PNGMasks.BACKGROUND || classIndex > categoryNames.size()
                    || categoryNames.get(classIndex - 1).isEmpty()) {
                errorEntries.add(new IOErrorInfoEntry(maskFileName, "Object " + object + " has no valid category ("
                        + (classIndex == PNGMasks.BACKGROUND ? "its pixels are background in the category mask"
                        : "category " + classIndex + " is not in " + PNGMasks.CLASSES_FILE_NAME) + ")."));
                continue;
            }

            final int[] bounds = entry.getValue();
            final MutableMask mask = new MutableMask(objects.width(), objects.height());

            for(int y = bounds[1]; y < bounds[3]; ++y) {
                for(int x = bounds[0]; x < bounds[2]; ++x) {
                    if(objects.get(x, y) == object) {
                        mask.set(x, y, true);
                    }
                }
            }

            final ObjectCategory category = categoryNameToCategory.computeIfAbsent(
                    categoryNames.get(classIndex - 1), name -> new ObjectCategory(name, ColorUtils.createRandomColor()));
            shapes.add(new BoundingMaskData(category, mask.toBitmap(), new ArrayList<>()));
        }

        return shapes;
    }

    private static ImageAnnotationImportResult emptyResult(List<IOErrorInfoEntry> errorEntries,
                                                           Map<String, ObjectCategory> categories) {
        return new ImageAnnotationImportResult(0, errorEntries,
                                               new ImageAnnotationData(Collections.emptyList(),
                                                                       Collections.emptyMap(), categories));
    }
}
