package BoundingboxEditor.model;

import BoundingboxEditor.model.io.BoundingBoxData;
import BoundingboxEditor.model.io.ImageAnnotationDataElement;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Model {
    private static final String BOUNDING_BOX_COORDINATES_PATTERN = "#0.0000";
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);

    private ListOrderedMap<String, File> imageFiles = new ListOrderedMap<>();
    private ObservableList<BoundingBoxCategory> boundingBoxCategories = FXCollections.observableArrayList();
    private Map<String, ImageMetaData> imageMetaDataMap = new HashMap<>();

    private Map<String, ImageAnnotationDataElement> imageFileNameToAnnotation = new HashMap<>();
    private Set<String> categoriesWithExistingBoundingBoxes = new HashSet<>();

    private IntegerProperty fileIndex = new SimpleIntegerProperty(0);
    private IntegerProperty imageFileListSize = new SimpleIntegerProperty(0);
    private BooleanProperty nextFileValid = new SimpleBooleanProperty(false);
    private BooleanProperty previousFileValid = new SimpleBooleanProperty(false);

    private Set<String> boundingBoxCategoryNames = ConcurrentHashMap.newKeySet();

    public Model() {
        numberFormat.applyPattern(BOUNDING_BOX_COORDINATES_PATTERN);
        setUpInternalListeners();
    }

    public void updateFromFiles(Collection<File> files) {
        imageFiles = ListOrderedMap.listOrderedMap(files.parallelStream().collect(LinkedHashMap::new,
                (map, item) -> map.put(item.getName(), item), Map::putAll));

        boundingBoxCategories.clear();
        boundingBoxCategoryNames.clear();
        categoriesWithExistingBoundingBoxes.clear();
        imageMetaDataMap.clear();
        imageFileNameToAnnotation.clear();

        imageFileListSize.set(imageFiles.size());
        fileIndex.set(0);
    }

    public void updateBoundingBoxData(int fileIndex, List<BoundingBoxData> boundingBoxes) {
        String oldFileName = getFileNameByIndex(fileIndex);

        if(!boundingBoxes.isEmpty()) {
            ImageAnnotationDataElement imageAnnotation = imageFileNameToAnnotation.get(oldFileName);

            if(imageAnnotation == null) {
                ImageMetaData metaData = imageMetaDataMap.get(oldFileName);
                imageFileNameToAnnotation.put(oldFileName, new ImageAnnotationDataElement(metaData, boundingBoxes));
            } else {
                imageAnnotation.setBoundingBoxes(boundingBoxes);
            }
        } else {
            imageFileNameToAnnotation.remove(oldFileName);
        }
    }

    public void updateCurrentBoundingBoxData(List<BoundingBoxData> boundingBoxes) {
        updateBoundingBoxData(fileIndex.get(), boundingBoxes);
    }

    public Map<String, ImageMetaData> getImageMetaDataMap() {
        return imageMetaDataMap;
    }

    public Map<String, ImageAnnotationDataElement> getImageFileNameToAnnotation() {
        return imageFileNameToAnnotation;
    }

    public ImageAnnotationDataElement getCurrentImageAnnotation() {
        return imageFileNameToAnnotation.get(getCurrentImageFileName());
    }

    public int getCurrentFileIndex() {
        return fileIndex.get();
    }

    public void updateCurrentImageAnnotation(List<BoundingBoxData> boundingBoxData) {
        getCurrentImageAnnotation().setBoundingBoxes(boundingBoxData);
    }

    public void updateImageAnnotations(List<ImageAnnotationDataElement> imageAnnotations) {
        imageAnnotations.forEach(annotation -> {
            ImageAnnotationDataElement imageAnnotationDataElement = imageFileNameToAnnotation.get(annotation.getImageFileName());
            if(imageAnnotationDataElement == null) {
                imageFileNameToAnnotation.put(annotation.getImageFileName(), annotation);
            } else {
                imageAnnotationDataElement.getBoundingBoxes().addAll(annotation.getBoundingBoxes());
            }
        });
    }

    public Collection<ImageAnnotationDataElement> getImageAnnotations() {
        return imageFileNameToAnnotation.values();
    }

    public Set<String> getCategoriesWithExistingBoundingBoxes() {
        return categoriesWithExistingBoundingBoxes;
    }

    public String getFileNameByIndex(int index) {
        return imageFiles.get(index);
    }

    public IntegerProperty fileIndexProperty() {
        return fileIndex;
    }

    public ObservableList<BoundingBoxCategory> getBoundingBoxCategories() {
        return boundingBoxCategories;
    }

    public Set<String> getImageFileNameSet() {
        return imageFiles.keySet();
    }

    public void incrementFileIndex() {
        fileIndex.set(fileIndex.get() + 1);
    }

    public void decrementFileIndex() {
        fileIndex.set(fileIndex.get() - 1);
    }

    public IntegerProperty imageFileListSizeProperty() {
        return imageFileListSize;
    }

    public BooleanProperty nextFileValidProperty() {
        return nextFileValid;
    }

    public BooleanProperty previousFileValidProperty() {
        return previousFileValid;
    }

    public Boolean isHasNextFile() {
        return nextFileValid.get();
    }

    public Boolean isHasPreviousFile() {
        return previousFileValid.get();
    }

    public String getCurrentImageFilePath() {
        return imageFiles.getValue(fileIndex.get()).getPath();
    }

    public String getCurrentImageFileName() {
        return imageFiles.getValue(fileIndex.get()).getName();
    }

    public File getCurrentImageFile() {
        return imageFiles.getValue(fileIndex.get());
    }

    public Set<String> getBoundingBoxCategoryNames() {
        return boundingBoxCategoryNames;
    }

    public ListOrderedMap<String, File> getImageFiles() {
        return imageFiles;
    }

    public ObservableList<File> getImageFilesAsObservableList() {
        return FXCollections.unmodifiableObservableList(FXCollections.observableList(imageFiles.valueList()));
    }

    private void setUpInternalListeners() {
        nextFileValid.bind(fileIndex.lessThan(imageFileListSizeProperty().subtract(1)));
        previousFileValid.bind(fileIndex.greaterThan(0));

        boundingBoxCategories.addListener((ListChangeListener<BoundingBoxCategory>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    boundingBoxCategoryNames.addAll(c.getAddedSubList().stream()
                            .map(BoundingBoxCategory::getName).collect(Collectors.toList()));
                }

                if(c.wasRemoved()) {
                    boundingBoxCategoryNames.removeAll(c.getRemoved().stream()
                            .map(BoundingBoxCategory::getName).collect(Collectors.toList()));
                }
            }
        });
    }
}
