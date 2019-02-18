import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Model {
    private static final String BOUNDING_BOX_COORDINATES_PATTERN = "#0.0000";
    private static final String[] imageExtensions = {".jpg", ".bmp", ".png"};
    private static final int MAX_DIRECTORY_DEPTH = 1;
    private static final DecimalFormat numberFormat = (DecimalFormat) NumberFormat.getNumberInstance(Locale.ENGLISH);

    private ObservableList<File> imageFileList;
    private ObservableList<BoundingBoxItem> boundingBoxItems;
    private ObservableMap<String, List<Double>> boundingBoxData;
    private IntegerProperty fileIndex;
    private IntegerBinding imageFileListSize;
    private BooleanBinding hasNextFile;
    private BooleanBinding hasPreviousFile;
    private ObjectBinding<File> currentFile;

    public Model() {
        fileIndex = new SimpleIntegerProperty(0);
        boundingBoxData = FXCollections.observableMap(new LinkedHashMap<>());
        // Put in default valued Bounding Box class.
        boundingBoxItems = FXCollections.observableArrayList(new BoundingBoxItem());
        numberFormat.applyPattern(BOUNDING_BOX_COORDINATES_PATTERN);
    }

    public void setImageFileListFromPath(Path path) throws Exception {
        imageFileList = FXCollections.observableArrayList(
                Files.walk(path, MAX_DIRECTORY_DEPTH)
                        .filter(p -> Arrays.stream(imageExtensions).anyMatch(p.toString()::endsWith))
                        .map(p -> new File(p.toString()))
                        .collect(Collectors.toList())
        );

        if (imageFileList.isEmpty()) {
            throw new NoValidImagesException(String.format("The path \"%s\" does not contain any valid images.",
                    path.toString()));
        }

        fileIndex.set(0);
        imageFileListSize = Bindings.size(imageFileList);
        hasNextFile = fileIndex.lessThan(imageFileListSize.subtract(1));
        hasPreviousFile = fileIndex.greaterThan(0);
        currentFile = Bindings.valueAt(imageFileList, fileIndex);
    }

    public Image getCurrentImage() {
        return getImageFromFile(currentFile.get());
    }

    public IntegerProperty fileIndexProperty() {
        return fileIndex;
    }

    public ObservableList<BoundingBoxItem> getBoundingBoxItems() {
        return boundingBoxItems;
    }

    public void incrementFileIndex() {
        fileIndex.set(fileIndex.get() + 1);
    }

    public void decrementFileIndex() {
        fileIndex.set(fileIndex.get() - 1);
    }

    public IntegerBinding getFileListSizeBinding() {
        return imageFileListSize;
    }

    public BooleanBinding hasNextFileBinding() {
        return hasNextFile;
    }

    public BooleanBinding hasPreviousFileBinding() {
        return hasPreviousFile;
    }

    public ObservableMap<String, List<Double>> getBoundingBoxData() {
        return boundingBoxData;
    }

    void writeBoundingBoxDataToFile(File file) throws IOException {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {
            boundingBoxData.forEach((key, value) -> printWriter.println(key + ", " +
                    value.stream().map(numberFormat::format).collect(Collectors.joining(", "))));
        }
    }

    public String getCurrentImageFilePath() {
        final String imagePath = getCurrentImage().getUrl()
                .replace("/", "\\")
                .replace("%20", " ");
        return imagePath.substring(imagePath.indexOf("\\") + 1);
    }

    private Image getImageFromFile(File file) {
        return new Image(file.toURI().toString());
    }
}
