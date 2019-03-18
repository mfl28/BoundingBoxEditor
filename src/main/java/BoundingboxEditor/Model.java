package BoundingboxEditor;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

class Model {
    private static final String BOUNDING_BOX_COORDINATES_PATTERN = "#0.0000";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);
    private ObservableList<File> imageFileList = FXCollections.observableArrayList();
    private ObservableList<BoundingBoxCategory> boundingBoxCategories = FXCollections.observableArrayList();
    private IntegerProperty fileIndex = new SimpleIntegerProperty(0);
    private IntegerProperty imageFileListSize = new SimpleIntegerProperty(0);
    private BooleanBinding hasNextFile;
    private BooleanBinding hasPreviousFile;
    private ObjectBinding<File> currentFile;
    private Set<String> boundingBoxCategoryNames = new HashSet<>();

    Model() {
        BoundingBoxCategory defaultCategory = new BoundingBoxCategory();
        boundingBoxCategories.add(defaultCategory);
        boundingBoxCategoryNames.add(defaultCategory.getName());
        numberFormat.applyPattern(BOUNDING_BOX_COORDINATES_PATTERN);
    }

    void setImageFileListFromPath(Path path) throws IOException, NoValidImagesException {
        imageFileList = FXCollections.observableArrayList(
                Files.walk(path, MAX_DIRECTORY_DEPTH)
                        .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                        .map(p -> new File(p.toString()))
                        .collect(Collectors.toList())
        );

        if(imageFileList.isEmpty()) {
            throw new NoValidImagesException(String.format("The path \"%s\" does not contain any valid images.",
                    path.toString()));
        }

        fileIndex.set(0);
        imageFileListSize.bind(Bindings.size(imageFileList));
        hasNextFile = fileIndex.lessThan(imageFileListSize.subtract(1));
        hasPreviousFile = fileIndex.greaterThan(0);
        currentFile = Bindings.valueAt(imageFileList, fileIndex);
    }

    Image getCurrentImage() {
        return getImageFromFile(currentFile.get());
    }

    IntegerProperty fileIndexProperty() {
        return fileIndex;
    }

    ObservableList<BoundingBoxCategory> getBoundingBoxCategories() {
        return boundingBoxCategories;
    }

    void incrementFileIndex() {
        fileIndex.set(fileIndex.get() + 1);
    }

    void decrementFileIndex() {
        fileIndex.set(fileIndex.get() - 1);
    }

    IntegerProperty fileListSizeProperty() {
        return imageFileListSize;
    }

    BooleanBinding hasNextFileBinding() {
        return hasNextFile;
    }

    BooleanBinding hasPreviousFileBinding() {
        return hasPreviousFile;
    }

    Boolean isHasNextFile() {
        return hasNextFile.get();
    }

    Boolean isHasPreviousFile() {
        return hasPreviousFile.get();
    }

    String getCurrentImageFilePath() {
        final String imagePath = getCurrentImage().getUrl()
                .replace("/", "\\")
                .replace("%20", " ");
        return imagePath.substring(imagePath.indexOf('\\') + 1);
    }

    Set<String> getBoundingBoxCategoryNames() {
        return boundingBoxCategoryNames;
    }

    ObservableList<File> getImageFileList() {
        return imageFileList;
    }

    private Image getImageFromFile(File file) {
        return new Image(file.toURI().toString(), true);
    }
}
