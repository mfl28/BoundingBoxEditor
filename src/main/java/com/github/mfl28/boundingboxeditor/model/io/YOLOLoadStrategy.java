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

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.results.IOErrorInfoEntry;
import com.github.mfl28.boundingboxeditor.model.io.results.ImageAnnotationImportResult;
import com.github.mfl28.boundingboxeditor.utils.ColorUtils;
import javafx.beans.property.DoubleProperty;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads rectangular bounding-box annotations and instance-segmentation annotations in the YOLO-format described at
 * <a href="https://github.com/AlexeyAB/Yolo_mark/issues/60#issuecomment-401854885">...</a> and
 * <a href="https://docs.ultralytics.com/datasets/segment/">...</a>
 */
public class YOLOLoadStrategy implements ImageAnnotationLoadStrategy {
    public static final String INVALID_BOUNDING_BOX_COORDINATES_MESSAGE = "Invalid bounding-box coordinates on line ";
    private static final boolean INCLUDE_SUBDIRECTORIES = false;
    private static final String OBJECT_DATA_FILE_NAME = "object.data";
    private final List<String> categories = new ArrayList<>();
    private final List<IOErrorInfoEntry> unParsedFileErrorMessages =
            Collections.synchronizedList(new ArrayList<>());
    private Map<String, List<String>> baseFileNameToImageFileMap;
    private Map<String, ObjectCategory> categoryNameToCategoryMap;
    private Map<String, Integer> boundingShapeCountPerCategory;

    @Override
    public ImageAnnotationImportResult load(Path path, Set<String> filesToLoad,
                                            Map<String, ObjectCategory> existingCategoryNameToCategoryMap,
                                            DoubleProperty progress)
            throws IOException {
        this.baseFileNameToImageFileMap = filesToLoad.stream().collect(
                Collectors.groupingBy(FilenameUtils::getBaseName, HashMap::new,
                        Collectors.mapping(Function.identity(), Collectors.toList()))
        );
        this.boundingShapeCountPerCategory = new ConcurrentHashMap<>();
        this.categoryNameToCategoryMap = new ConcurrentHashMap<>(existingCategoryNameToCategoryMap);

        try {
            loadObjectCategories(path);
        } catch (Exception e) {
            unParsedFileErrorMessages.add(new IOErrorInfoEntry(OBJECT_DATA_FILE_NAME, e.getMessage()));
            return new ImageAnnotationImportResult(0, unParsedFileErrorMessages, ImageAnnotationData.empty());
        }

        if (categories.isEmpty()) {
            unParsedFileErrorMessages
                    .add(new IOErrorInfoEntry(OBJECT_DATA_FILE_NAME, "Does not contain any category names."));
            return new ImageAnnotationImportResult(0, unParsedFileErrorMessages, ImageAnnotationData.empty());
        }

        try (Stream<Path> fileStream = Files.walk(path, INCLUDE_SUBDIRECTORIES ? Integer.MAX_VALUE : 1)) {
            List<File> annotationFiles = fileStream
                    .filter(pathItem -> pathItem.getFileName().toString().endsWith(".txt"))
                    .map(Path::toFile).toList();

            int totalNrOfFiles = annotationFiles.size();
            AtomicInteger nrProcessedFiles = new AtomicInteger(0);

            List<ImageAnnotation> imageAnnotations = annotationFiles.parallelStream()
                    .map(file -> {
                        progress.set(1.0 * nrProcessedFiles
                                .incrementAndGet() / totalNrOfFiles);

                        try {
                            return loadAnnotationFromFile(file);
                        } catch (InvalidAnnotationFormatException |
                                 AnnotationToNonExistentImageException |
                                 AnnotationAssociationException |
                                 IOException e) {
                            unParsedFileErrorMessages
                                    .add(new IOErrorInfoEntry(
                                            file.getName(),
                                            e.getMessage()));
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            return new ImageAnnotationImportResult(
                    imageAnnotations.size(),
                    unParsedFileErrorMessages,
                    new ImageAnnotationData(imageAnnotations, boundingShapeCountPerCategory, categoryNameToCategoryMap)
            );
        }
    }

    private void loadObjectCategories(Path root) throws IOException {
        if (!root.resolve(OBJECT_DATA_FILE_NAME).toFile().exists()) {
            throw new InvalidAnnotationFormatException(
                    "Does not exist in annotation folder \"" + root.getFileName().toString() + "\".");
        }

        try (BufferedReader fileReader = Files.newBufferedReader(root.resolve(OBJECT_DATA_FILE_NAME))) {
            String line;

            while ((line = fileReader.readLine()) != null) {
                line = line.strip();

                if (!line.isBlank()) {
                    categories.add(line);
                }
            }
        }
    }

    private ImageAnnotation loadAnnotationFromFile(File file) throws IOException {
        final List<String> annotatedImageFiles = baseFileNameToImageFileMap.get(
                FilenameUtils.getBaseName(file.getName()));

        if (annotatedImageFiles == null) {
            throw new AnnotationToNonExistentImageException(
                    "No associated image file.");
        } else if (annotatedImageFiles.size() > 1) {
            throw new AnnotationAssociationException(
                    "More than one associated image file.");
        }

        final String annotatedImageFileName = annotatedImageFiles.getFirst();

        try (BufferedReader fileReader = Files.newBufferedReader(file.toPath())) {
            String line;

            List<BoundingShapeData> boundingShapeDataList = new ArrayList<>();

            int counter = 1;

            while ((line = fileReader.readLine()) != null) {
                line = line.strip();

                if (!line.isBlank()) {
                    try {
                        final BoundingShapeData boundingShapeData = parseBoundingShapeData(line, counter);
                        boundingShapeDataList.add(boundingShapeData);
                        boundingShapeCountPerCategory.merge(boundingShapeData.getCategoryName(), 1, Integer::sum);
                    } catch (InvalidAnnotationFormatException e) {
                        unParsedFileErrorMessages.add(new IOErrorInfoEntry(file.getName(), e.getMessage()));
                    }
                }

                ++counter;
            }

            if (boundingShapeDataList.isEmpty()) {
                return null;
            }

            // ImageMetaData will be loaded when the corresponding image is displayed for the first time.
            return new ImageAnnotation(new ImageMetaData(annotatedImageFileName), boundingShapeDataList);
        }
    }

    private BoundingShapeData parseBoundingShapeData(String line, int lineNumber) {
        Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);

        int categoryId = parseCategoryIndex(scanner, lineNumber);

        List<Double> entries = new ArrayList<>();

        while (scanner.hasNextDouble()) {
            double entry = scanner.nextDouble();

            assertRatio(entry, "Bounds value not within interval [0, 1] on line " + lineNumber + ".");

            entries.add(entry);
        }

        if (entries.size() == 4) {
            return createBoundingBoxData(
                    categoryId, entries.get(0), entries.get(1), entries.get(2), entries.get(3), lineNumber);
        } else if(entries.size() >= 6 && entries.size() % 2 == 0) {
            return createBoundingPolygonData(categoryId, entries);
        }

        throw new InvalidAnnotationFormatException("Invalid number of bounds values on line " + lineNumber + ".");
    }

    private BoundingBoxData createBoundingBoxData(int categoryId, double xMidRelative, double yMidRelative,
                                                  double widthRelative, double heightRelative,
                                                  int lineNumber) {
        double xMinRelative = xMidRelative - widthRelative / 2;
        if (xMinRelative < 0 && -xMinRelative < 1e-6) {
            xMinRelative = 0;
        }
        assertRatio(xMinRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double yMinRelative = yMidRelative - heightRelative / 2;
        if (yMinRelative < 0 && -yMinRelative < 1e-6) {
            yMinRelative = 0;
        }
        assertRatio(yMinRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double xMaxRelative = xMidRelative + widthRelative / 2;
        if (xMaxRelative > 1 && xMaxRelative - 1 < 1e-6) {
            xMaxRelative = 1;
        }
        assertRatio(xMaxRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        double yMaxRelative = yMidRelative + heightRelative / 2;
        if (yMaxRelative > 1 && yMaxRelative - 1 < 1e-6) {
            yMaxRelative = 1;
        }
        assertRatio(yMaxRelative, INVALID_BOUNDING_BOX_COORDINATES_MESSAGE + lineNumber + ".");

        String categoryName = categories.get(categoryId);

        ObjectCategory objectCategory = categoryNameToCategoryMap.computeIfAbsent(
                categoryName,
                key -> new ObjectCategory(key,
                        ColorUtils
                                .createRandomColor()));

        // Note that there are no tags or parts in YOLO-format.
        return new BoundingBoxData(objectCategory,
                xMinRelative, yMinRelative, xMaxRelative, yMaxRelative,
                Collections.emptyList());
    }

    private BoundingPolygonData createBoundingPolygonData(int categoryId, List<Double> entries) {
        String categoryName = categories.get(categoryId);

        ObjectCategory objectCategory = categoryNameToCategoryMap.computeIfAbsent(categoryName,
                key -> new ObjectCategory(key,
                        ColorUtils
                                .createRandomColor()));

        // Note that there are no tags or parts in YOLO-format.
        return new BoundingPolygonData(objectCategory, entries, Collections.emptyList());
    }

    private int parseCategoryIndex(Scanner scanner, int lineNumber) {
        if (!scanner.hasNextInt()) {
            throw new InvalidAnnotationFormatException("Missing or invalid category index on line " + lineNumber + ".");
        }

        int categoryId = scanner.nextInt();

        if (categoryId < 0 || categoryId >= categories.size()) {
            throw new InvalidAnnotationFormatException("Invalid category index " + categoryId
                    + " (of " + categories.size() + " categories) on line " +
                    lineNumber + ".");
        }

        return categoryId;
    }

    private void assertRatio(double ratio, String message) {
        if (ratio < 0 || ratio > 1) {
            throw new InvalidAnnotationFormatException(message);
        }
    }
}
