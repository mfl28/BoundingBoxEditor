/*
 * Copyright (C) 2025 Markus Fleischhacker <markus.fleischhacker28@gmail.com>
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
package com.github.mfl28.boundingboxeditor.model;

import com.github.mfl28.boundingboxeditor.model.data.*;
import com.github.mfl28.boundingboxeditor.model.io.BoundingBoxPredictorConfig;
import com.github.mfl28.boundingboxeditor.model.io.restclients.BoundingBoxPredictorClientConfig;
import com.github.mfl28.boundingboxeditor.model.io.results.IOResult;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The model-component of the program (MVC architecture pattern is used). Holds internal representations
 * of the data that is used by the app.
 *
 * @see com.github.mfl28.boundingboxeditor.controller.Controller Controller
 * @see com.github.mfl28.boundingboxeditor.ui.MainView MainView
 */
public class Model {
    private static final String BOUNDING_SHAPE_COORDINATES_PATTERN = "#0.0000";
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    /**
     * Maps the filenames of the currently loaded image-files onto corresponding {@link ImageMetaData} objects. Image-metadata for
     * an image is constructed (at most) once when the first bounding-shape on an image is created and is reused subsequently.
     */
    private final Map<String, ImageMetaData> imageFileNameToMetaData = new HashMap<>();
    /**
     * Maps the filenames of the currently loaded image-files onto corresponding {@link ImageAnnotation} objects. This is the
     * main data-structure for storing data of the bounding-shapes and image-metadata of image annotations. {@link ImageAnnotation} objects
     * are constructed exactly once when the first store-trigger-event occurs and are updated in subsequent store-trigger-events. A
     * store-trigger-event may be any of the following:
     * <ul>
     * <li>Selection of a different image-file from the currently loaded images.</li>
     * <li>Requesting the saving of image-annotations.</li>
     * <li>Requesting the import of image-annotations.</li>
     * <li>Requesting to open a different image-folder (in this case all current annotations are removed).</li>
     * </ul>
     */
    private final Map<String, ImageAnnotation> imageFileNameToAnnotation = new HashMap<>();
    /**
     * Contains all currently existing {@link ObjectCategory} objects.
     */
    private final ObservableList<ObjectCategory> objectCategories = FXCollections.observableArrayList();
    /**
     * Maps the name of a currently existing bounding-shape category to the current number of existing bounding-shape elements
     * assigned to the category.
     */
    private final Map<String, Integer> categoryToAssignedBoundingShapesCount = new HashMap<>();
    private final IntegerProperty fileIndex = new SimpleIntegerProperty(0);
    private final IntegerProperty nrImageFiles = new SimpleIntegerProperty(0);
    private final BooleanProperty nextImageFileExists = new SimpleBooleanProperty(false);
    private final BooleanProperty previousImageFileExists = new SimpleBooleanProperty(false);
    private final BooleanProperty saved = new SimpleBooleanProperty(true);

    private final BoundingBoxPredictorClientConfig
            boundingBoxPredictorClientConfig = new BoundingBoxPredictorClientConfig();
    private final BoundingBoxPredictorConfig boundingBoxPredictorConfig = new BoundingBoxPredictorConfig();

    /**
     * Maps the filenames of the currently loaded image-files onto the corresponding {@link File} objects. A
     * {@link ListOrderedMap} data-structure is used to preserve an order (in this case the input order) to
     * allow consistent iteration through the files.
     */
    private ListOrderedMap<String, File> imageFileNameToFile = new ListOrderedMap<>();

    /**
     * Creates the app's model-component.
     */
    public Model() {
        numberFormat.applyPattern(BOUNDING_SHAPE_COORDINATES_PATTERN);
        setUpInternalListeners();
    }

    public boolean isSaved() {
        return saved.get();
    }

    public void setSaved(boolean saved) {
        this.saved.set(saved);
    }

    public BooleanProperty savedProperty() {
        return saved;
    }

    /**
     * Returns a property representing the number of currently set image-files.
     *
     * @return the property
     */
    public IntegerProperty nrImageFilesProperty() {
        return nrImageFiles;
    }

    /**
     * Clears all existing annotation- and category-data and resets properties.
     */
    public void clear() {
        imageFileNameToMetaData.clear();
        clearAnnotationData(false);

        imageFileNameToFile = new ListOrderedMap<>();

        nrImageFiles.set(imageFileNameToMetaData.size());
        fileIndex.set(0);
    }

    /**
     * Convenience method to update the currently selected image-file annotation's bounding-shape data.
     *
     * @param boundingShapeData the new bounding-shape data elements
     */
    public void updateCurrentBoundingShapeData(List<BoundingShapeData> boundingShapeData) {
        updateBoundingShapeDataAtFileIndex(fileIndex.get(), boundingShapeData);
    }

    /**
     * Creates or updates the {@link ImageAnnotation} associated with the image-file at the provided index using
     * new bounding-shape data.
     *
     * @param fileIndex         the index of the image-file whose annotation should be created/updated
     * @param boundingShapeData the new bounding-shape data elements
     */
    public void updateBoundingShapeDataAtFileIndex(int fileIndex, List<BoundingShapeData> boundingShapeData) {
        String fileName = imageFileNameToFile.get(fileIndex);

        ImageAnnotation imageAnnotation = imageFileNameToAnnotation.getOrDefault(fileName,
                new ImageAnnotation(
                        imageFileNameToMetaData
                                .get(fileName)));

        if(!boundingShapeData.isEmpty()) {
            if(!imageAnnotation.getBoundingShapeData().equals(boundingShapeData)) {
                saved.set(false);
            }

            imageAnnotation.setBoundingShapeData(boundingShapeData);
            imageFileNameToAnnotation.put(fileName, imageAnnotation);
        } else {
            if(imageFileNameToAnnotation.remove(fileName) != null && !imageFileNameToAnnotation.isEmpty()) {
                saved.set(false);
            } else if(imageFileNameToAnnotation.isEmpty()) {
                saved.set(true);
            }
        }
    }

    /**
     * Updates the model's image-annotations with the provided new annotations by
     * either constructing new {@link ImageAnnotation} objects and putting them into
     * the annotation map or by updating existing ones. This method should be used by
     * classes extending {@link com.github.mfl28.boundingboxeditor.model.io.ImageAnnotationLoadStrategy ImageAnnotationLoadStrategy} to load
     * annotations from files into the model. Notice that existing bounding-shape data is not overwritten, the loaded bounding-shape
     * data is merely added to the existing ones.
     *
     * @param imageAnnotations the new image-annotations
     */
    public void updateImageAnnotations(Collection<ImageAnnotation> imageAnnotations,
                                       IOResult.OperationType operationType) {
        boolean noCurrentAnnotations =
                imageFileNameToAnnotation.values().stream()
                        .allMatch(imageAnnotation -> imageAnnotation.getBoundingShapeData().isEmpty());
        boolean boundingShapesAdded = false;

        for(final ImageAnnotation annotation : imageAnnotations) {
            ImageAnnotation imageAnnotation = imageFileNameToAnnotation.get(annotation.getImageFileName());
            if(imageAnnotation == null) {
                annotation.setImageMetaData(imageFileNameToMetaData.get(annotation.getImageFileName()));
                imageFileNameToAnnotation.put(annotation.getImageFileName(), annotation);
            } else {
                imageAnnotation.getBoundingShapeData().addAll(annotation.getBoundingShapeData());
            }

            if(!annotation.getBoundingShapeData().isEmpty()) {
                boundingShapesAdded = true;
            }
        }

        if(boundingShapesAdded) {
            if(operationType.equals(IOResult.OperationType.ANNOTATION_IMPORT)) {
                saved.set(noCurrentAnnotations);
            } else if(operationType.equals(IOResult.OperationType.BOUNDING_BOX_PREDICTION)) {
                saved.set(false);
            }
        }
    }

    public ImageMetaData getCurrentImageMetaData() {
        return imageFileNameToMetaData.computeIfAbsent(getCurrentImageFileName(),
                key -> {
                    ImageMetaData newMetaData;

                    try {
                        newMetaData =
                                ImageMetaData.fromFile(getCurrentImageFile());
                    } catch(IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    ImageAnnotation currentImageAnnotation =
                            getCurrentImageAnnotation();

                    if(currentImageAnnotation != null &&
                            !currentImageAnnotation.getImageMetaData()
                                    .hasDetails()) {
                        currentImageAnnotation.setImageMetaData(newMetaData);
                    }

                    return newMetaData;
                });
    }

    /**
     * Returns the image-filename to image-metadata mapping.
     *
     * @return the mapping
     */
    public Map<String, ImageMetaData> getImageFileNameToMetaDataMap() {
        return imageFileNameToMetaData;
    }

    /**
     * Returns the image-filename to image annotation mapping.
     *
     * @return the mapping
     */
    public Map<String, ImageAnnotation> getImageFileNameToAnnotationMap() {
        return imageFileNameToAnnotation;
    }

    /**
     * Returns the {@link ImageAnnotation} object corresponding to the file with
     * the currently set file-index.
     *
     * @return the image-annotation
     */
    public ImageAnnotation getCurrentImageAnnotation() {
        return imageFileNameToAnnotation.get(getCurrentImageFileName());
    }

    /**
     * Returns the filename of the image-file corresponding to the currently set file-index.
     *
     * @return the filename
     */
    public String getCurrentImageFileName() {
        return imageFileNameToFile.getValue(fileIndex.get()).getName();
    }

    /**
     * Returns the currently set image-file index.
     *
     * @return the file-index
     */
    public int getCurrentFileIndex() {
        return fileIndex.get();
    }

    public BoundingBoxPredictorClientConfig getBoundingBoxPredictorClientConfig() {
        return boundingBoxPredictorClientConfig;
    }

    public BoundingBoxPredictorConfig getBoundingBoxPredictorConfig() {
        return boundingBoxPredictorConfig;
    }

    /**
     * Returns the currently existing image-annotation data.
     *
     * @return the image-annotation data
     */
    public ImageAnnotationData createImageAnnotationData() {
        return new ImageAnnotationData(imageFileNameToAnnotation.values(), categoryToAssignedBoundingShapesCount,
                getCategoryNameToCategoryMap());
    }

    /**
     * Updates the model data from an {@link ImageAnnotationData} object.
     *
     * @param imageAnnotationData the image-annotation data
     */
    public void updateFromImageAnnotationData(ImageAnnotationData imageAnnotationData,
                                              IOResult.OperationType operationType) {
        final Map<String, Integer> updatedCategoryNameToBoundingShapeCountMap =
                createMergedCategoryToBoundingShapeCountMap(
                        imageAnnotationData.categoryNameToBoundingShapeCountMap());
        updateObjectCategoriesFromData(imageAnnotationData.categoryNameToCategoryMap());
        categoryToAssignedBoundingShapesCount.putAll(updatedCategoryNameToBoundingShapeCountMap);
        updateImageAnnotations(imageAnnotationData.imageAnnotations(), operationType);
    }

    /**
     * Returns the category to existing bounding-shapes count mapping.
     *
     * @return the mapping
     */
    public Map<String, Integer> getCategoryToAssignedBoundingShapesCountMap() {
        return categoryToAssignedBoundingShapesCount;
    }

    /**
     * Returns the file-index property.
     *
     * @return the file-index property
     */
    public IntegerProperty fileIndexProperty() {
        return fileIndex;
    }

    /**
     * Returns an observable list of the {@link ObjectCategory} objects.
     *
     * @return the list of object categories
     */
    public ObservableList<ObjectCategory> getObjectCategories() {
        return objectCategories;
    }

    /**
     * Returns a set of the filenames of all currently set image-files.
     *
     * @return the set of filenames
     */
    public Set<String> getImageFileNameSet() {
        return imageFileNameToFile.keySet();
    }

    /**
     * Returns a boolean signifying the existence of the next image-file.
     *
     * @return true if the current index is the last, false otherwise
     */
    public boolean hasNextImageFile() {
        return nextImageFileExists.get();
    }

    /**
     * Returns a boolean signifying the existence of the previous image-file.
     *
     * @return true if the current index is the first, false otherwise
     */
    public boolean hasPreviousImageFile() {
        return previousImageFileExists.get();
    }

    /**
     * Returns a property monitoring the existence of a next image-file in the
     * sequence of currently set image-files. The property's values is false if
     * the current file-index is the last, otherwise it is true.
     *
     * @return the property
     */
    public BooleanProperty hasNextImageFileProperty() {
        return nextImageFileExists;
    }

    /**
     * Returns a property monitoring the existence of a previous image-file in the
     * sequence of currently set image-files. The property's values is false if
     * the current file-index is the first, otherwise it is true.
     *
     * @return the property
     */
    public BooleanProperty hasPreviousImageFileProperty() {
        return previousImageFileExists;
    }

    /**
     * Returns the path of the image-file corresponding to the currently set file-index.
     *
     * @return the path as a string
     */
    public String getCurrentImageFilePath() {
        return imageFileNameToFile.getValue(fileIndex.get()).getPath();
    }

    /**
     * Returns image-file corresponding to the currently set file-index.
     *
     * @return the image-file
     */
    public File getCurrentImageFile() {
        return imageFileNameToFile.getValue(fileIndex.get());
    }

    /**
     * Returns a boolean indicating if the model currently contains image-files.
     *
     * @return true if the model contains image-files, false otherwise
     */
    public boolean containsImageFiles() {
        return !imageFileNameToFile.isEmpty();
    }

    /**
     * Returns a boolean indicating if the model currently contains annotations.
     *
     * @return true if the model contains annotations, false otherwise
     */
    public boolean containsAnnotations() {
        return !imageFileNameToAnnotation.isEmpty();
    }

    /**
     * Returns a boolean indicating if the model currently contains bounding box categories.
     *
     * @return true if the model contains categories, false otherwise
     */
    public boolean containsCategories() {
        return !objectCategories.isEmpty();
    }

    /**
     * Returns the currently set image-files as an unmodifiable list.
     *
     * @return the image-file list
     */
    public List<File> getImageFiles() {
        return Collections.unmodifiableList(imageFileNameToFile.valueList());
    }

    public List<ImageMetaData> getImageMetaDataList() {
        return imageFileNameToFile.valueList().stream().map(file -> imageFileNameToMetaData.get(file.getName())).toList();
    }

    public void setImageFiles(Collection<File> imageFiles) {
        imageFileNameToFile = ListOrderedMap.listOrderedMap(imageFiles.parallelStream()
                .collect(LinkedHashMap::new, (map, item) -> map
                        .put(item.getName(), item), Map::putAll));

        nrImageFiles.set(imageFileNameToFile.size());
        fileIndex.set(0);
    }

    /**
     * Increments the file-index by 1.
     */
    public void incrementFileIndex() {
        fileIndex.set(fileIndex.get() + 1);
    }

    /**
     * Decrements the file-index by 1.
     */
    public void decrementFileIndex() {
        fileIndex.set(fileIndex.get() - 1);
    }

    /**
     * Clears all data relating to annotations (including created categories).
     */
    public void clearAnnotationData(boolean keepCategories) {
        imageFileNameToAnnotation.clear();

        if(!keepCategories) {
            objectCategories.clear();
        }

        categoryToAssignedBoundingShapesCount.clear();

        saved.set(true);
    }

    public Map<String, ObjectCategory> getCategoryNameToCategoryMap() {
        return objectCategories.stream()
                .collect(Collectors.toMap(ObjectCategory::getName, Function.identity()));
    }

    private Map<String, Integer> createMergedCategoryToBoundingShapeCountMap(Map<String, Integer> toMerge) {
        return Stream.of(categoryToAssignedBoundingShapesCount, toMerge)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                Integer::sum
                        )
                );
    }

    private void updateObjectCategoriesFromData(Map<String, ObjectCategory> categoryNameToCategoryMap) {
        objectCategories.setAll(categoryNameToCategoryMap.values());
    }

    private void setUpInternalListeners() {
        nextImageFileExists.bind(fileIndex.lessThan(nrImageFilesProperty().subtract(1)));
        previousImageFileExists.bind(fileIndex.greaterThan(0));

        objectCategories.addListener((ListChangeListener<ObjectCategory>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    c.getAddedSubList().forEach(item -> categoryToAssignedBoundingShapesCount.put(item.getName(), 0));
                }

                if(c.wasRemoved()) {
                    c.getRemoved().forEach(item -> categoryToAssignedBoundingShapesCount.remove(item.getName()));
                }
            }
        });
    }
}
