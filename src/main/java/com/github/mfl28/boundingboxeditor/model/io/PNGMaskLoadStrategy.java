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
        final Optional<String> missingInput = findMissingInput(classesFile, classFolder);

        if(missingInput.isPresent()) {
            errorEntries.add(new IOErrorInfoEntry(path.getFileName().toString(), missingInput.get()));
            return emptyResult(errorEntries, existingCategoryNameToCategoryMap);
        }

        final boolean hasObjectMasks = Files.isDirectory(objectFolder);
        final List<Path> maskFiles = listPngFiles(hasObjectMasks ? objectFolder : classFolder);
        final LoadContext context = new LoadContext(classFolder, hasObjectMasks,
                Files.readAllLines(classesFile, StandardCharsets.UTF_8).stream().map(String::strip).toList(),
                filesToLoad.stream().collect(Collectors.groupingBy(FilenameUtils::getBaseName)),
                existingCategoryNameToCategoryMap, errorEntries);

        for(int i = 0; i < maskFiles.size(); ++i) {
            context.loadMaskFile(maskFiles.get(i));
            progress.set((i + 1.0) / maskFiles.size());
        }

        progress.set(1.0);
        return new ImageAnnotationImportResult(context.fileNameToAnnotation.size(), errorEntries,
                                               new ImageAnnotationData(context.fileNameToAnnotation.values(),
                                                                       context.categoryNameToShapeCount,
                                                                       existingCategoryNameToCategoryMap));
    }

    private static Optional<String> findMissingInput(Path classesFile, Path classFolder) {
        if(!Files.isRegularFile(classesFile)) {
            return Optional.of("The file \"" + PNGMasks.CLASSES_FILE_NAME + "\" with the category names is missing.");
        }

        if(!Files.isDirectory(classFolder)) {
            return Optional.of("The folder \"" + PNGMasks.CLASS_FOLDER_NAME + "\" with the category masks is missing.");
        }

        return Optional.empty();
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
        final List<BoundingShapeData> shapes = new ArrayList<>();

        for(Map.Entry<Integer, ObjectRegion> entry : findObjects(objects, classes).entrySet()) {
            final int object = entry.getKey();
            final int classIndex = entry.getValue().mostFrequentClass();

            if(classIndex == PNGMasks.BACKGROUND || classIndex > categoryNames.size()
                    || categoryNames.get(classIndex - 1).isEmpty()) {
                errorEntries.add(new IOErrorInfoEntry(maskFileName, "Object " + object + " has no valid category ("
                        + (classIndex == PNGMasks.BACKGROUND ? "its pixels are background in the category mask"
                        : "category " + classIndex + " is not in " + PNGMasks.CLASSES_FILE_NAME) + ")."));
            } else {
                final ObjectCategory category = categoryNameToCategory.computeIfAbsent(
                        categoryNames.get(classIndex - 1),
                        name -> new ObjectCategory(name, ColorUtils.createRandomColor()));
                shapes.add(new BoundingMaskData(category, entry.getValue().toMask(objects, object),
                                                new ArrayList<>()));
            }
        }

        return shapes;
    }

    /**
     * Finds the objects of an object mask (in the order of their numbers), with their bounds and the categories of
     * their pixels.
     */
    private static Map<Integer, ObjectRegion> findObjects(PNGMasks.Values objects, PNGMasks.Values classes) {
        final Map<Integer, ObjectRegion> regions = new TreeMap<>();

        for(int y = 0; y < objects.height(); ++y) {
            for(int x = 0; x < objects.width(); ++x) {
                final int object = objects.get(x, y);

                if(object != PNGMasks.BACKGROUND && object != PNGMasks.IGNORED) {
                    regions.computeIfAbsent(object, key -> new ObjectRegion()).add(x, y, classes.get(x, y));
                }
            }
        }

        return regions;
    }

    private static ImageAnnotationImportResult emptyResult(List<IOErrorInfoEntry> errorEntries,
                                                           Map<String, ObjectCategory> categories) {
        return new ImageAnnotationImportResult(0, errorEntries,
                                               new ImageAnnotationData(Collections.emptyList(),
                                                                       Collections.emptyMap(), categories));
    }

    /**
     * The state of one import (strategies are shared between threads, so it isn't kept in fields).
     */
    private static final class LoadContext {
        private final Path classFolder;
        private final boolean hasObjectMasks;
        private final List<String> categoryNames;
        private final Map<String, List<String>> baseNameToImageFileNames;
        private final Map<String, ObjectCategory> categoryNameToCategory;
        private final List<IOErrorInfoEntry> errorEntries;
        private final Map<String, ImageAnnotation> fileNameToAnnotation = new LinkedHashMap<>();
        private final Map<String, Integer> categoryNameToShapeCount = new HashMap<>();

        LoadContext(Path classFolder, boolean hasObjectMasks, List<String> categoryNames,
                    Map<String, List<String>> baseNameToImageFileNames,
                    Map<String, ObjectCategory> categoryNameToCategory, List<IOErrorInfoEntry> errorEntries) {
            this.classFolder = classFolder;
            this.hasObjectMasks = hasObjectMasks;
            this.categoryNames = categoryNames;
            this.baseNameToImageFileNames = baseNameToImageFileNames;
            this.categoryNameToCategory = categoryNameToCategory;
            this.errorEntries = errorEntries;
        }

        /**
         * Loads the masks of the image that has the name of a mask file.
         */
        void loadMaskFile(Path maskFile) {
            final String maskFileName = maskFile.getFileName().toString();
            final List<String> imageFileNames =
                    baseNameToImageFileNames.getOrDefault(FilenameUtils.getBaseName(maskFileName), List.of());

            if(imageFileNames.size() != 1) {
                errorEntries.add(new IOErrorInfoEntry(maskFileName, imageFileNames.isEmpty()
                        ? "No image of the currently loaded image files has the name of the mask."
                        : "Several images (" + String.join(", ", imageFileNames) + ") have the name of the mask."));
                return;
            }

            try {
                addAnnotation(imageFileNames.getFirst(), readMasks(maskFile, maskFileName));
            } catch(IOException e) {
                errorEntries.add(new IOErrorInfoEntry(maskFileName, e.getMessage() != null ? e.getMessage()
                        : "The mask can't be read."));
            }
        }

        private List<BoundingShapeData> readMasks(Path maskFile, String maskFileName) throws IOException {
            final PNGMasks.Values classes = PNGMasks.read(classFolder.resolve(maskFileName));
            final PNGMasks.Values objects = hasObjectMasks ? PNGMasks.read(maskFile) : classes;

            if(objects.width() != classes.width() || objects.height() != classes.height()) {
                throw new IOException("The object and category masks have different sizes.");
            }

            return createMasks(objects, classes, categoryNames, categoryNameToCategory, maskFileName, errorEntries);
        }

        private void addAnnotation(String imageFileName, List<BoundingShapeData> shapes) {
            if(shapes.isEmpty()) {
                return;
            }

            fileNameToAnnotation.put(imageFileName, new ImageAnnotation(new ImageMetaData(imageFileName), shapes));
            shapes.forEach(shape -> categoryNameToShapeCount.merge(shape.getCategoryName(), 1, Integer::sum));
        }
    }

    /**
     * The pixels of one object in an object mask: its bounds, and how many of its pixels have which category.
     */
    private static final class ObjectRegion {
        private final Map<Integer, Integer> classCounts = new HashMap<>();
        private int minX = Integer.MAX_VALUE;
        private int minY = Integer.MAX_VALUE;
        private int maxX = Integer.MIN_VALUE;
        private int maxY = Integer.MIN_VALUE;

        void add(int x, int y, int classIndex) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + 1);
            maxY = Math.max(maxY, y + 1);
            classCounts.merge(classIndex, 1, Integer::sum);
        }

        /**
         * Returns the category that most of the object's pixels have (the lower number on ties), or the background if
         * none of them has one.
         */
        int mostFrequentClass() {
            return classCounts.entrySet().stream()
                    .filter(count -> count.getKey() != PNGMasks.BACKGROUND && count.getKey() != PNGMasks.IGNORED)
                    .max(Map.Entry.<Integer, Integer>comparingByValue()
                                  .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                    .map(Map.Entry::getKey)
                    .orElse(PNGMasks.BACKGROUND);
        }

        /**
         * Builds the mask from the object's region only.
         */
        MaskBitmap toMask(PNGMasks.Values objects, int object) {
            final MutableMask mask = new MutableMask(objects.width(), objects.height());

            for(int y = minY; y < maxY; ++y) {
                for(int x = minX; x < maxX; ++x) {
                    if(objects.get(x, y) == object) {
                        mask.set(x, y, true);
                    }
                }
            }

            return mask.toBitmap();
        }
    }
}
