package BoundingBoxEditor.model;

import BoundingBoxEditor.model.io.BoundingBoxData;
import BoundingBoxEditor.model.io.ImageAnnotation;
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

/**
 * The model-component of the program (MVC architecture pattern is used). Holds internal representations
 * of the data that is used by the app.
 *
 * @see BoundingBoxEditor.controller.Controller Controller
 * @see BoundingBoxEditor.ui.MainView MainView
 */
public class Model {
    private static final String BOUNDING_BOX_COORDINATES_PATTERN = "#0.0000";
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);

    /**
     * Maps the filenames of the currently loaded image-files onto the corresponding {@link File} objects. A
     * {@link ListOrderedMap} data-structure is used to preserve an order (in this case the input order) to
     * allow consistent iteration through the files.
     */
    private ListOrderedMap<String, File> imageFileNameToFile = new ListOrderedMap<>();

    /**
     * Maps the filenames of the currently loaded image-files onto corresponding {@link ImageMetaData} objects. Image-metadata for
     * an image is constructed (at most) once when the first bounding-box on an image is created and is reused subsequently.
     */
    private Map<String, ImageMetaData> imageFileNameToMetaData = new HashMap<>();

    /**
     * Maps the filenames of the currently loaded image-files onto corresponding {@link ImageAnnotation} objects. This is the
     * main data-structure for storing data of the bounding-boxes and image-metadata of image annotations. {@link ImageAnnotation} objects
     * are constructed exactly once when the first store-trigger-event occurs and are updated in subsequent store-trigger-events. A
     * store-trigger-event may be any of the following:
     * <ul>
     * <li>Selection of a different image-file from the currently loaded images.</li>
     * <li>Requesting the saving of image-annotations.</li>
     * <li>Requesting the import of image-annotations.</li>
     * <li>Requesting to open a different image-folder (in this case all current annotations are removed).</li>
     * </ul>
     */
    private Map<String, ImageAnnotation> imageFileNameToAnnotation = new HashMap<>();

    /**
     * Contains all currently existing {@link BoundingBoxCategory} objects.
     */
    private ObservableList<BoundingBoxCategory> boundingBoxCategories = FXCollections.observableArrayList();

    /**
     * Maps the name of a currently existing bounding-box category to the current number of existing bounding-box elements
     * assigned to the category.
     */
    private Map<String, Integer> categoryToAssignedBoundingBoxesCount = new HashMap<>();

    private IntegerProperty fileIndex = new SimpleIntegerProperty(0);
    private IntegerProperty nrImageFiles = new SimpleIntegerProperty(0);
    private BooleanProperty nextImageFileExists = new SimpleBooleanProperty(false);
    private BooleanProperty previousImageFileExists = new SimpleBooleanProperty(false);

    /**
     * Creates the app's model-component.
     */
    public Model() {
        numberFormat.applyPattern(BOUNDING_BOX_COORDINATES_PATTERN);
        setUpInternalListeners();
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
     * Clears all existing annotation- and category-data, resets properties and sets
     * new image-files.
     *
     * @param imageFiles the new image-files
     */
    public void resetDataAndSetImageFiles(Collection<File> imageFiles) {
        imageFileNameToMetaData.clear();
        clearAnnotationData();

        imageFileNameToFile = ListOrderedMap.listOrderedMap(imageFiles.parallelStream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getName(), item), Map::putAll));

        nrImageFiles.set(imageFileNameToFile.size());
        fileIndex.set(0);
    }

    /**
     * Convenience method to update the currently selected image-file annotation's bounding-box data.
     *
     * @param boundingBoxes the new bounding-box data
     */
    public void updateCurrentBoundingBoxData(List<BoundingBoxData> boundingBoxes) {
        updateBoundingBoxDataAtFileIndex(fileIndex.get(), boundingBoxes);
    }

    /**
     * Creates or updates the {@link ImageAnnotation} associated with the image-file at the provided index using
     * new bounding-box data.
     *
     * @param fileIndex     the index of the image-file whose annotation should be created/updated
     * @param boundingBoxes the new bounding-box data
     */
    public void updateBoundingBoxDataAtFileIndex(int fileIndex, List<BoundingBoxData> boundingBoxes) {
        String fileName = imageFileNameToFile.get(fileIndex);

        if(!boundingBoxes.isEmpty()) {
            ImageAnnotation imageAnnotation = imageFileNameToAnnotation.get(fileName);

            if(imageAnnotation == null) {
                ImageMetaData metaData = imageFileNameToMetaData.get(fileName);
                imageFileNameToAnnotation.put(fileName, new ImageAnnotation(metaData, boundingBoxes));
            } else {
                imageAnnotation.setBoundingBoxData(boundingBoxes);
            }
        } else {
            imageFileNameToAnnotation.remove(fileName);
        }
    }

    /**
     * Updates the model's image-annotations with the provided new annotations by
     * either constructing new {@link ImageAnnotation} objects and putting them into
     * the annotation map or by updating existing ones. This method should be used by
     * classes extending {@link BoundingBoxEditor.model.io.ImageAnnotationLoadStrategy ImageAnnotationLoadStrategy} to load
     * annotations from files into the model. Notice that existing bounding-box data is not overwritten, the loaded bounding-box
     * data is merely added to the existing ones.
     *
     * @param imageAnnotations the new image-annotations
     */
    public void updateImageAnnotations(List<ImageAnnotation> imageAnnotations) {
        imageAnnotations.forEach(annotation -> {
            ImageAnnotation imageAnnotation = imageFileNameToAnnotation.get(annotation.getImageFileName());
            if(imageAnnotation == null) {
                imageFileNameToAnnotation.put(annotation.getImageFileName(), annotation);
            } else {
                imageAnnotation.getBoundingBoxData().addAll(annotation.getBoundingBoxData());
            }
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

    /**
     * Returns the currently existing image-annotations.
     *
     * @return the image-annotations
     */
    public Collection<ImageAnnotation> getImageAnnotations() {
        return imageFileNameToAnnotation.values();
    }

    /**
     * Returns the category to existing bounding-boxes count mapping.
     *
     * @return the mapping
     */
    public Map<String, Integer> getCategoryToAssignedBoundingBoxesCountMap() {
        return categoryToAssignedBoundingBoxesCount;
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
     * Returns an observable list of the {@link BoundingBoxCategory} objects.
     *
     * @return the list of bounding-box categories
     */
    public ObservableList<BoundingBoxCategory> getBoundingBoxCategories() {
        return boundingBoxCategories;
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
    public Boolean hasNextImageFile() {
        return nextImageFileExists.get();
    }

    /**
     * Returns a boolean signifying the existence of the previous image-file.
     *
     * @return true if the current index is the first, false otherwise
     */
    public Boolean hasPreviousImageFile() {
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
        return !boundingBoxCategories.isEmpty();
    }

    /**
     * Returns the the currently set image-files as an unmodifiable observable list.
     *
     * @return the image-file list
     */
    public ObservableList<File> getImageFilesAsObservableList() {
        return FXCollections.unmodifiableObservableList(FXCollections.observableList(imageFileNameToFile.valueList()));
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
    public void clearAnnotationData() {
        imageFileNameToAnnotation.clear();

        boundingBoxCategories.clear();
        categoryToAssignedBoundingBoxesCount.clear();
    }

    private void setUpInternalListeners() {
        nextImageFileExists.bind(fileIndex.lessThan(nrImageFilesProperty().subtract(1)));
        previousImageFileExists.bind(fileIndex.greaterThan(0));

        boundingBoxCategories.addListener((ListChangeListener<BoundingBoxCategory>) c -> {
            while(c.next()) {
                if(c.wasAdded()) {
                    c.getAddedSubList().forEach(item ->
                            categoryToAssignedBoundingBoxesCount.put(item.getName(), 0));
                }

                if(c.wasRemoved()) {
                    c.getRemoved().forEach(item -> categoryToAssignedBoundingBoxesCount.remove(item.getName()));
                }
            }
        });
    }
}
