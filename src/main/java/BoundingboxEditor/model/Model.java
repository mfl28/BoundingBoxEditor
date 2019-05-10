package BoundingboxEditor.model;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class Model {
    private static final String BOUNDING_BOX_COORDINATES_PATTERN = "#0.0000";
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);

    private ObservableList<File> imageFiles = FXCollections.observableArrayList();
    private Set<String> imageFileSet = new HashSet<>();
    private ObservableList<BoundingBoxCategory> boundingBoxCategories = FXCollections.observableArrayList();

    private IntegerProperty fileIndex = new SimpleIntegerProperty(0);
    private IntegerProperty imageFileListSize = new SimpleIntegerProperty(0);
    private BooleanProperty nextFileValid = new SimpleBooleanProperty(false);
    private BooleanProperty previousFileValid = new SimpleBooleanProperty(false);

    private Set<String> boundingBoxCategoryNames = new HashSet<>();

    public Model() {
        numberFormat.applyPattern(BOUNDING_BOX_COORDINATES_PATTERN);
        setUpInternalListeners();
    }

    public void updateFromFiles(Collection<File> files) {
        imageFiles.setAll(files);
        imageFileSet = files.stream().map(file -> file.getName().substring(0, file.getName().lastIndexOf('.'))).collect(Collectors.toSet());
        boundingBoxCategories.clear();
        boundingBoxCategoryNames.clear();
        fileIndex.set(0);
    }

    public Image getCurrentImage() {
        return getImageFromFile(imageFiles.get(fileIndex.get()));
    }

    public IntegerProperty fileIndexProperty() {
        return fileIndex;
    }

    public ObservableList<BoundingBoxCategory> getBoundingBoxCategories() {
        return boundingBoxCategories;
    }

    public Set<String> getImageFileSet() {
        return imageFileSet;
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

    public int getImageFileListSize() {
        return imageFileListSize.get();
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
        return imageFiles.get(fileIndex.get()).getPath();
    }

    public Set<String> getBoundingBoxCategoryNames() {
        return boundingBoxCategoryNames;
    }

    public ObservableList<File> getImageFiles() {
        return FXCollections.unmodifiableObservableList(imageFiles);
    }

    private Image getImageFromFile(File file) {
        return new Image(file.toURI().toString(), true);
    }

    private void setUpInternalListeners() {

        imageFileListSize.bind(Bindings.size(imageFiles));
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
